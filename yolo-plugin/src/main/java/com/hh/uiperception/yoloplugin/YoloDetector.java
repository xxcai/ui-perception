package com.hh.uiperception.yoloplugin;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

/**
 * 本地 ONNX Runtime YOLO 检测器。
 * 移植自 X-OmniClaw 的 UiYoloOnnxDetector.kt。
 *
 * 给一张 Bitmap，返回原图坐标系下的检测框。
 */
public class YoloDetector implements AutoCloseable {

    private volatile float confThreshold;
    private volatile float iouThreshold;

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final int inputSize;

    public YoloDetector(Context context) throws IOException, OrtException {
        this(context, YoloClassLabels.MODEL_ASSET_NAME,
                YoloClassLabels.INPUT_SIZE,
                YoloClassLabels.DEFAULT_CONFIDENCE,
                YoloClassLabels.DEFAULT_IOU);
    }

    public YoloDetector(Context context, String assetModelFileName,
                        int inputSize, float confThreshold, float iouThreshold)
            throws IOException, OrtException {
        this.confThreshold = clampConf(confThreshold);
        this.iouThreshold = clampIou(iouThreshold);
        this.inputSize = inputSize;
        this.environment = OrtEnvironment.getEnvironment();

        byte[] modelBytes;
        try (var is = context.getAssets().open(assetModelFileName)) {
            modelBytes = is.readAllBytes();
        }

        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
        opts.setIntraOpNumThreads(4);
        opts.setInterOpNumThreads(1);
        this.session = environment.createSession(modelBytes, opts);
    }

    public void setConfThreshold(float value) { this.confThreshold = clampConf(value); }
    public void setIouThreshold(float value) { this.iouThreshold = clampIou(value); }
    public float getConfThreshold() { return confThreshold; }
    public float getIouThreshold() { return iouThreshold; }

    public YoloDetectionResult detect(Bitmap bitmap) throws OrtException {
        long startMs = System.currentTimeMillis();

        // 1. 预处理：letterbox + NCHW
        YoloPreprocess.LetterboxResult lbResult = YoloPreprocess.letterbox(bitmap, inputSize);
        float[] inputTensorData = YoloPreprocess.bitmapToNchw01(lbResult.bitmap, inputSize);
        if (!lbResult.bitmap.isRecycled()) {
            lbResult.bitmap.recycle();
        }
        long preprocessEndMs = System.currentTimeMillis();

        // 2. 推理
        String inputName = session.getInputNames().iterator().next();
        long[] inputShape = {1, 3, inputSize, inputSize};

        float[] rawOutput;
        long[] fixedShape;
        long inferenceEndMs;

        try (OnnxTensor inputTensor = OnnxTensor.createTensor(
                environment, FloatBuffer.wrap(inputTensorData), inputShape)) {
            Map<String, OnnxTensor> inputs = new LinkedHashMap<>();
            inputs.put(inputName, inputTensor);

            try (OrtSession.Result outputs = session.run(inputs)) {
                inferenceEndMs = System.currentTimeMillis();
                Object outputValue = outputs.get(0).getValue();
                float[][] shapeAndData = extractOutputToFloatArray(outputValue);
                rawOutput = shapeAndData[0];
                long[] outputShape = longsFromFloats(shapeAndData[1]);
                fixedShape = fixOutputShapeIfNeeded(outputShape, rawOutput);
            }
        }

        // 3. 后处理
        float letterboxSize = lbResult.params.dstSize;

        List<YoloPostprocess.RawDetection> decoded = YoloPostprocess.decodeRaw(
                rawOutput, fixedShape, confThreshold, true, false);

        // 归一化坐标 → letterbox 像素坐标
        List<YoloPostprocess.RawDetection> scaled = new ArrayList<>(decoded.size());
        for (YoloPostprocess.RawDetection d : decoded) {
            scaled.add(new YoloPostprocess.RawDetection(
                    scaleIfNormalized(d.x1, letterboxSize),
                    scaleIfNormalized(d.y1, letterboxSize),
                    scaleIfNormalized(d.x2, letterboxSize),
                    scaleIfNormalized(d.y2, letterboxSize),
                    d.score, d.classId
            ));
        }

        List<YoloPostprocess.RawDetection> nmsResult = YoloPostprocess.nmsXyxy(
                scaled, iouThreshold, YoloClassLabels.MAX_DETECTIONS);

        int classCount = (int) fixedShape[1] - 4;
        List<YoloDetection> detections = new ArrayList<>(nmsResult.size());
        for (YoloPostprocess.RawDetection d : nmsResult) {
            float[] origCoords = YoloPostprocess.scaleBoxesXyxy(
                    new float[]{d.x1, d.y1, d.x2, d.y2}, lbResult.params);
            detections.add(new YoloDetection(
                    origCoords[0], origCoords[1], origCoords[2], origCoords[3],
                    d.score, d.classId, YoloClassLabels.displayName(d.classId)
            ));
        }

        long endMs = System.currentTimeMillis();
        return new YoloDetectionResult(
                detections, classCount,
                preprocessEndMs - startMs,
                inferenceEndMs - preprocessEndMs,
                endMs - inferenceEndMs
        );
    }

    @Override
    public void close() {
        try { session.close(); } catch (OrtException ignored) {}
        environment.close();
    }

    // --- 私有辅助方法 ---

    private static float clampConf(float v) { return Math.max(0.001f, Math.min(0.999f, v)); }
    private static float clampIou(float v) { return Math.max(0.05f, Math.min(0.95f, v)); }

    private static float scaleIfNormalized(float value, float letterboxSize) {
        return (value >= 0f && value <= 1.01f) ? value * letterboxSize : value;
    }

    /**
     * 修正输出形状：标准 [1, 4+nc, anchors] 直接返回；
     * 若为 [1, anchors, 4+nc] 则原地转置后返回修正形状。
     */
    private static long[] fixOutputShapeIfNeeded(long[] shape, float[] raw) {
        if (shape.length != 3) return shape;
        int secondDim = (int) shape[1];
        int thirdDim = (int) shape[2];
        int channelLike = secondDim - 4;
        int anchorLike = thirdDim - 4;

        if (channelLike > 0 && channelLike <= 512) return shape;

        if (anchorLike > 0 && anchorLike <= 512) {
            transposeLastTwoDims(raw, secondDim, 4 + anchorLike);
            return new long[]{1, 4 + anchorLike, secondDim};
        }
        return shape;
    }

    private static void transposeLastTwoDims(float[] raw, int anchorCount, int channels) {
        float[] copy = raw.clone();
        for (int anchor = 0; anchor < anchorCount; anchor++) {
            for (int channel = 0; channel < channels; channel++) {
                raw[channel * anchorCount + anchor] = copy[anchor * channels + channel];
            }
        }
    }

    /**
     * 从 ONNX 输出中提取 float 数组和形状。
     * 返回 [0]=数据, [1]=形状（用 float[] 暂存 long 值）。
     */
    private static float[][] extractOutputToFloatArray(Object value) {
        if (value instanceof OnnxTensor) {
            OnnxTensor tensor = (OnnxTensor) value;
            long[] shape = tensor.getInfo().getShape();
            long total = 1;
            for (long s : shape) total *= s;
            float[] result = new float[(int) total];
            java.nio.FloatBuffer buffer = tensor.getFloatBuffer();
            buffer.rewind();
            int readable = Math.min(result.length, buffer.remaining());
            buffer.get(result, 0, readable);
            return new float[][]{result, floatsFromLongs(shape)};
        }
        return flattenNestedFloatTensor(value);
    }

    private static float[][] flattenNestedFloatTensor(Object value) {
        int[] dimensions = nestedFloatTensorDimensions(value);
        ArrayList<Float> buffer = new ArrayList<>();
        for (int dim : dimensions) { /* pre-size hint */ }
        flattenNestedFloatTensorData(value, buffer);
        float[] data = new float[buffer.size()];
        for (int i = 0; i < data.length; i++) data[i] = buffer.get(i);
        return new float[][]{data, floatsFromLongs(dimensions)};
    }

    private static int[] nestedFloatTensorDimensions(Object value) {
        if (value instanceof float[]) {
            return new int[]{((float[]) value).length};
        }
        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            if (arr.length == 0) throw new IllegalArgumentException("ONNX 输出为空数组");
            int[] inner = nestedFloatTensorDimensions(arr[0]);
            int[] result = new int[1 + inner.length];
            result[0] = arr.length;
            System.arraycopy(inner, 0, result, 1, inner.length);
            return result;
        }
        throw new IllegalArgumentException("不支持的 ONNX 输出类型: " + value.getClass().getName());
    }

    private static void flattenNestedFloatTensorData(Object value, List<Float> output) {
        if (value instanceof float[]) {
            for (float f : (float[]) value) output.add(f);
        } else if (value instanceof Object[]) {
            for (Object item : (Object[]) value) flattenNestedFloatTensorData(item, output);
        } else {
            throw new IllegalArgumentException("展平 ONNX 输出失败: " + value.getClass().getName());
        }
    }

    private static float[] floatsFromLongs(long[] arr) {
        float[] result = new float[arr.length];
        for (int i = 0; i < arr.length; i++) result[i] = (float) arr[i];
        return result;
    }

    private static float[] floatsFromLongs(int[] arr) {
        float[] result = new float[arr.length];
        for (int i = 0; i < arr.length; i++) result[i] = (float) arr[i];
        return result;
    }

    private static long[] longsFromFloats(float[] arr) {
        long[] result = new long[arr.length];
        for (int i = 0; i < arr.length; i++) result[i] = (long) arr[i];
        return result;
    }
}
