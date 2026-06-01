package com.hh.uiperception.yoloplugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * YOLO 输出解析、NMS 和坐标回映射。
 * 移植自 X-OmniClaw 的 UiYoloPostprocess.kt。
 */
public final class YoloPostprocess {

    private YoloPostprocess() {}

    /** NMS 前的中间检测结果。 */
    public static class RawDetection {
        public final float x1, y1, x2, y2;
        public final float score;
        public final int classId;

        public RawDetection(float x1, float y1, float x2, float y2,
                            float score, int classId) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.score = score;
            this.classId = classId;
        }
    }

    private static float sigmoid(float value) {
        return 1f / (1f + (float) Math.exp(-value));
    }

    public static float[] xywhToXyxy(float centerX, float centerY, float width, float height) {
        float left = centerX - width / 2f;
        float top = centerY - height / 2f;
        float right = centerX + width / 2f;
        float bottom = centerY + height / 2f;
        return new float[]{left, top, right, bottom};
    }

    /**
     * 将 letterbox 坐标还原到原始截图坐标系。
     */
    public static float[] scaleBoxesXyxy(float[] xyxy, YoloPreprocess.LetterboxParams params) {
        float left = (xyxy[0] - params.padX) / params.ratio;
        float top = (xyxy[1] - params.padY) / params.ratio;
        float right = (xyxy[2] - params.padX) / params.ratio;
        float bottom = (xyxy[3] - params.padY) / params.ratio;
        float w = params.srcWidth;
        float h = params.srcHeight;
        return new float[]{
                Math.max(0f, Math.min(left, w)),
                Math.max(0f, Math.min(top, h)),
                Math.max(0f, Math.min(right, w)),
                Math.max(0f, Math.min(bottom, h))
        };
    }

    /**
     * 解析 channel-major 展平布局 [1, 4+nc, numAnchors]。
     */
    public static List<RawDetection> decodeRaw(float[] raw, long[] shape,
                                                float confThresh,
                                                boolean useSigmoidOnClass,
                                                boolean anchorMajor) {
        if (shape.length != 3) return Collections.emptyList();
        int batch = (int) shape[0];
        int channels = (int) shape[1];
        int anchorCount = (int) shape[2];
        if (batch != 1) return Collections.emptyList();
        int classCount = channels - 4;
        if (classCount <= 0) return Collections.emptyList();
        int stride = 4 + classCount;

        List<RawDetection> detections = new ArrayList<>(256);
        for (int anchor = 0; anchor < anchorCount; anchor++) {
            float bestScore = -1f;
            int bestClassId = -1;
            for (int classIndex = 0; classIndex < classCount; classIndex++) {
                float scoreLogit;
                if (anchorMajor) {
                    scoreLogit = raw[anchor * stride + (4 + classIndex)];
                } else {
                    scoreLogit = raw[(4 + classIndex) * anchorCount + anchor];
                }
                float score = useSigmoidOnClass ? sigmoid(scoreLogit) : scoreLogit;
                if (score > bestScore) {
                    bestScore = score;
                    bestClassId = classIndex;
                }
            }
            if (bestScore < confThresh) continue;

            float cx = anchorMajor ? raw[anchor * stride + 0] : raw[0 * anchorCount + anchor];
            float cy = anchorMajor ? raw[anchor * stride + 1] : raw[1 * anchorCount + anchor];
            float w  = anchorMajor ? raw[anchor * stride + 2] : raw[2 * anchorCount + anchor];
            float h  = anchorMajor ? raw[anchor * stride + 3] : raw[3 * anchorCount + anchor];
            float[] xyxy = xywhToXyxy(cx, cy, w, h);
            detections.add(new RawDetection(xyxy[0], xyxy[1], xyxy[2], xyxy[3],
                    bestScore, bestClassId));
        }
        return detections;
    }

    public static List<RawDetection> nmsXyxy(List<RawDetection> detections,
                                              float iouThresh, int maxDet) {
        if (detections.isEmpty()) return Collections.emptyList();
        List<RawDetection> sorted = new ArrayList<>(detections);
        sorted.sort((a, b) -> Float.compare(b.score, a.score));

        List<RawDetection> kept = new ArrayList<>(Math.min(maxDet, sorted.size()));
        boolean[] suppressed = new boolean[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            if (suppressed[i]) continue;
            RawDetection current = sorted.get(i);
            kept.add(current);
            if (kept.size() >= maxDet) break;
            for (int j = i + 1; j < sorted.size(); j++) {
                if (suppressed[j]) continue;
                if (iouXyxy(current, sorted.get(j)) > iouThresh) {
                    suppressed[j] = true;
                }
            }
        }
        return kept;
    }

    public static float iouXyxy(RawDetection first, RawDetection second) {
        float interLeft = Math.max(first.x1, second.x1);
        float interTop = Math.max(first.y1, second.y1);
        float interRight = Math.min(first.x2, second.x2);
        float interBottom = Math.min(first.y2, second.y2);
        float intersection = Math.max(0f, interRight - interLeft) *
                Math.max(0f, interBottom - interTop);
        float areaA = Math.max(0f, first.x2 - first.x1) * Math.max(0f, first.y2 - first.y1);
        float areaB = Math.max(0f, second.x2 - second.x1) * Math.max(0f, second.y2 - second.y1);
        float union = areaA + areaB - intersection + 1e-6f;
        return intersection / union;
    }
}
