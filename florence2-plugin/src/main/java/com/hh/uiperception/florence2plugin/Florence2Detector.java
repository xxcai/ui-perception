package com.hh.uiperception.florence2plugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

/**
 * Florence-2 ONNX inference engine.
 * Uses HuggingFace onnx-community/Florence-2-base INT8 models.
 *
 * Pipeline:
 *   vision_encoder(pixel_values) → image_features
 *   embed_tokens(task_prompt_ids) → task_embeds
 *   concat [image_features; task_embeds] → encoder
 *   encoder → encoder_hidden_states
 *   decoder_model (no past KV) → captions (autoregressive)
 */
public class Florence2Detector implements AutoCloseable {

    private static final String TAG = "Florence2Detector";
    private static final int MAX_TOKENS = 20;
    private static final String MODEL_DIR = "models/florence2";

    private final OrtEnvironment env;
    private final OrtSession visionEncoder;
    private final OrtSession encoderSession;
    private final OrtSession embedTokens;
    private final OrtSession decoder;
    private final Florence2Tokenizer tokenizer;

    private final String visionInputName;

    public Florence2Detector(Context context) throws Exception {
        this(context, false);
    }

    public Florence2Detector(Context context, boolean useNnapi) throws Exception {
        env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions opts = createSessionOptions(useNnapi);

        File modelDir = new File(context.getExternalFilesDir(null), MODEL_DIR);

        visionEncoder = env.createSession(
                new File(modelDir, "vision_encoder_int8.onnx").getAbsolutePath(), opts);
        encoderSession = env.createSession(
                new File(modelDir, "encoder_model_int8.onnx").getAbsolutePath(), opts);
        embedTokens = env.createSession(
                new File(modelDir, "embed_tokens_int8.onnx").getAbsolutePath(), opts);
        // Use non-merged decoder — no past_key_values inputs, simpler and correct
        decoder = env.createSession(
                new File(modelDir, "decoder_model_int8.onnx").getAbsolutePath(), opts);

        tokenizer = new Florence2Tokenizer(context);
        visionInputName = firstInput(visionEncoder);

        logSessionInfo("VisionEncoder", visionEncoder);
        logSessionInfo("Encoder", encoderSession);
        logSessionInfo("EmbedTokens", embedTokens);
        logSessionInfo("Decoder", decoder);
    }

    public Florence2Result infer(Bitmap bitmap) throws OrtException {
        long t0 = System.currentTimeMillis();

        // 1. Preprocess
        float[] pixelData = Florence2Preprocess.preprocess(bitmap);
        long tPreprocess = System.currentTimeMillis();

        // 2. Vision Encoder: pixel_values → image features [seq, dim]
        float[][] imgFeatures;
        long[] visionShape = {1, 3, Florence2Preprocess.INPUT_SIZE, Florence2Preprocess.INPUT_SIZE};
        try (OnnxTensor pixTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(pixelData), visionShape)) {
            Map<String, OnnxTensor> inputs = new LinkedHashMap<>();
            inputs.put(visionInputName, pixTensor);
            try (OrtSession.Result result = visionEncoder.run(inputs)) {
                imgFeatures = extractSeq(result.get(0).getValue());
                Log.d(TAG, "Vision features: [" + imgFeatures.length + ", " + imgFeatures[0].length + "]");
            }
        }
        long tVision = System.currentTimeMillis();

        // 3. Embed task prompt tokens → [prompt_len, dim]
        long[] taskIds = tokenizer.encodeCaptionPrompt();
        float[][] taskEmbeds = runEmbedTokens(taskIds);
        Log.d(TAG, "Task prompt IDs: " + Arrays.toString(taskIds)
                + " → embeds [" + taskEmbeds.length + ", " + taskEmbeds[0].length + "]");

        // 4. Concatenate [image_features; task_embeds] along seq axis
        int imgSeq = imgFeatures.length;
        int taskSeq = taskEmbeds.length;
        int totalSeq = imgSeq + taskSeq;
        int hiddenDim = imgFeatures[0].length;

        float[][] concatEmbeds = new float[totalSeq][hiddenDim];
        System.arraycopy(imgFeatures, 0, concatEmbeds, 0, imgSeq);
        System.arraycopy(taskEmbeds, 0, concatEmbeds, imgSeq, taskSeq);

        long[][] encMask = new long[1][totalSeq];
        Arrays.fill(encMask[0], 1L);

        // 5. Encoder: concatenated embeds + attention_mask → encoder_hidden_states
        float[][] encoderOutput;
        try (OnnxTensor embedsTensor = OnnxTensor.createTensor(env, new float[][][]{concatEmbeds});
             OnnxTensor maskTensor = OnnxTensor.createTensor(env, encMask)) {
            Map<String, OnnxTensor> encInputs = new LinkedHashMap<>();
            encInputs.put("inputs_embeds", embedsTensor);
            encInputs.put("attention_mask", maskTensor);
            try (OrtSession.Result result = encoderSession.run(encInputs)) {
                encoderOutput = extractSeq(result.get(0).getValue());
                Log.d(TAG, "Encoder output: [" + encoderOutput.length + ", " + encoderOutput[0].length + "]");
            }
        }
        long tEncoder = System.currentTimeMillis();

        // 6. Autoregressive decoder (no KV cache — re-encode full sequence each step)
        //    Non-merged decoder_model only needs: inputs_embeds, encoder_hidden_states, encoder_attention_mask
        List<Long> generatedTokens = new ArrayList<>();

        for (int step = 0; step < MAX_TOKENS; step++) {
            // Build decoder input: [last_task_token, generated_0, generated_1, ...]
            int decSeqLen = 1 + generatedTokens.size();
            long[] decTokenIds = new long[decSeqLen];
            decTokenIds[0] = taskIds[taskIds.length - 1];
            for (int i = 0; i < generatedTokens.size(); i++) {
                decTokenIds[i + 1] = generatedTokens.get(i);
            }

            float[][] decEmbeds = runEmbedTokens(decTokenIds);
            long nextToken = runDecoder(decEmbeds, encoderOutput, encMask, decSeqLen);

            Log.d(TAG, "Step " + step + ": token=" + nextToken);
            if (nextToken == tokenizer.eosTokenId) break;
            generatedTokens.add(nextToken);
        }
        long tDecoder = System.currentTimeMillis();

        String caption = tokenizer.decode(toPrimitiveArray(generatedTokens));
        Log.d(TAG, "Caption: " + caption);

        return new Florence2Result(
                caption, generatedTokens.size(),
                tPreprocess - t0, tVision - tPreprocess,
                tEncoder - tVision, tDecoder - tEncoder,
                System.currentTimeMillis() - t0
        );
    }

    private float[][] runEmbedTokens(long[] tokenIds) throws OrtException {
        long[][] inputIds = new long[1][tokenIds.length];
        for (int i = 0; i < tokenIds.length; i++) inputIds[0][i] = tokenIds[i];

        try (OnnxTensor idsTensor = OnnxTensor.createTensor(env, inputIds)) {
            Map<String, OnnxTensor> inputs = new LinkedHashMap<>();
            inputs.put(firstInput(embedTokens), idsTensor);
            try (OrtSession.Result result = embedTokens.run(inputs)) {
                return extractSeq(result.get(0).getValue());
            }
        }
    }

    private long runDecoder(float[][] decEmbeds, float[][] encoderOutput,
                            long[][] encMask, int decSeqLen) throws OrtException {
        float[][][] decEmbeds3d = new float[][][]{decEmbeds};
        float[][][] encOut3d = new float[][][]{encoderOutput};

        try (OnnxTensor decEmbedTensor = OnnxTensor.createTensor(env, decEmbeds3d);
             OnnxTensor encHiddenTensor = OnnxTensor.createTensor(env, encOut3d);
             OnnxTensor encMaskTensor = OnnxTensor.createTensor(env, encMask)) {

            Map<String, OnnxTensor> inputs = new LinkedHashMap<>();
            inputs.put("inputs_embeds", decEmbedTensor);
            inputs.put("encoder_hidden_states", encHiddenTensor);
            inputs.put("encoder_attention_mask", encMaskTensor);

            try (OrtSession.Result result = decoder.run(inputs, Set.of("logits"))) {
                return argmaxFromOutput(result.get(0).getValue(), decSeqLen);
            }
        }
    }

    public String getSessionInfo() {
        return "Vision: " + visionEncoder.getInputNames() + " → " + visionEncoder.getOutputNames() + "\n"
             + "Encoder: " + encoderSession.getInputNames() + " → " + encoderSession.getOutputNames() + "\n"
             + "EmbedTokens: " + embedTokens.getInputNames() + " → " + embedTokens.getOutputNames() + "\n"
             + "Decoder: " + decoder.getInputNames() + " → " + decoder.getOutputNames();
    }

    @Override
    public void close() {
        try { visionEncoder.close(); } catch (OrtException ignored) {}
        try { encoderSession.close(); } catch (OrtException ignored) {}
        try { embedTokens.close(); } catch (OrtException ignored) {}
        try { decoder.close(); } catch (OrtException ignored) {}
        env.close();
    }

    // --- Helpers ---

    private OrtSession.SessionOptions createSessionOptions(boolean useNnapi) throws OrtException {
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
        opts.setIntraOpNumThreads(4);
        opts.setInterOpNumThreads(1);
        opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        if (useNnapi) {
            try {
                opts.addNnapi();
                Log.d(TAG, "NNAPI enabled");
            } catch (Exception e) {
                Log.w(TAG, "NNAPI not available: " + e.getMessage());
            }
        }
        return opts;
    }

    private void logSessionInfo(String name, OrtSession session) throws OrtException {
        Log.d(TAG, name + " inputs: " + session.getInputNames());
        Log.d(TAG, name + " outputs: " + session.getOutputNames());
    }

    private static String firstInput(OrtSession session) throws OrtException {
        return session.getInputNames().iterator().next();
    }

    private static float[][] extractSeq(Object value) {
        if (value instanceof float[][][]) {
            float[][][] arr = (float[][][]) value;
            return arr[0]; // [1, seq, dim] → [seq, dim]
        }
        if (value instanceof float[][][][]) {
            float[][][][] arr = (float[][][][]) value;
            if (arr[0].length == 1) return arr[0][0];
            return arr[0][0];
        }
        throw new IllegalArgumentException("Cannot extract sequence from: " + value.getClass().getName());
    }

    private static long argmaxFromOutput(Object value, int seqLen) {
        if (value instanceof float[][][]) {
            float[][][] logits = (float[][][]) value;
            float[] lastLogits = logits[0][seqLen - 1];
            int maxIdx = 0;
            float maxVal = lastLogits[0];
            for (int i = 1; i < lastLogits.length; i++) {
                if (lastLogits[i] > maxVal) { maxVal = lastLogits[i]; maxIdx = i; }
            }
            return maxIdx;
        }
        if (value instanceof OnnxTensor) {
            OnnxTensor tensor = (OnnxTensor) value;
            long[] shape = tensor.getInfo().getShape();
            int vocabSize = (int) shape[2];
            FloatBuffer buffer = tensor.getFloatBuffer();
            buffer.rewind();
            int offset = (seqLen - 1) * vocabSize;
            float maxVal = Float.NEGATIVE_INFINITY;
            int maxIdx = 0;
            for (int i = 0; i < vocabSize && offset + i < buffer.limit(); i++) {
                float v = buffer.get(offset + i);
                if (v > maxVal) { maxVal = v; maxIdx = i; }
            }
            return maxIdx;
        }
        throw new IllegalArgumentException("Cannot extract argmax from: " + value.getClass().getName());
    }

    private static long[] toPrimitiveArray(List<Long> list) {
        long[] arr = new long[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
}
