package com.hh.uiperception.smallmodelplugin.gemma;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import com.google.ai.edge.litertlm.Backend;
import com.google.ai.edge.litertlm.Content;
import com.google.ai.edge.litertlm.Contents;
import com.google.ai.edge.litertlm.Conversation;
import com.google.ai.edge.litertlm.ConversationConfig;
import com.google.ai.edge.litertlm.Engine;
import com.google.ai.edge.litertlm.EngineConfig;
import com.google.ai.edge.litertlm.Message;
import com.google.ai.edge.litertlm.SamplerConfig;
import com.hh.uiperception.smallmodelplugin.api.SmallModelCallback;
import com.hh.uiperception.smallmodelplugin.api.SmallModelError;
import com.hh.uiperception.smallmodelplugin.api.SmallModelInitConfig;
import com.hh.uiperception.smallmodelplugin.api.SmallModelRequest;
import com.hh.uiperception.smallmodelplugin.api.SmallModelResult;
import com.hh.uiperception.smallmodelplugin.api.SmallModelVisionClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gemma-4-E2B-it 的 LiteRT-LM Java 封装。
 */
public final class Gemma4E2BClient implements SmallModelVisionClient {

    private static final String TAG = "Gemma4E2BClient";

    private final Object lock = new Object();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean inferenceRunning = new AtomicBoolean(false);

    private Engine engine;
    private ConversationConfig conversationConfig;
    private SmallModelInitConfig initConfig;

    @Override
    public void initialize(Context context, SmallModelInitConfig config,
                           SmallModelCallback<Void> callback) {
        executor.execute(() -> {
            SmallModelInitConfig resolvedConfig = config != null
                    ? config
                    : SmallModelInitConfig.defaultFor(context);
            Log.i(TAG, "initialize requested. modelPath=" + resolvedConfig.modelPath()
                    + ", preferGpu=" + resolvedConfig.preferGpu()
                    + ", maxTokens=" + resolvedConfig.maxTokens()
                    + ", topK=" + resolvedConfig.topK()
                    + ", topP=" + resolvedConfig.topP()
                    + ", temperature=" + resolvedConfig.temperature());
            SmallModelError validationError = validateInit(context, resolvedConfig);
            if (validationError != null) {
                Log.e(TAG, "initialize validation failed: " + validationError);
                dispatchError(callback, validationError);
                return;
            }

            synchronized (lock) {
                if (engine != null && conversationConfig != null) {
                    Log.i(TAG, "initialize skipped: already initialized");
                    dispatchSuccess(callback, null);
                    return;
                }
            }

            try {
                Backend backend = resolvedConfig.preferGpu() ? new Backend.GPU() : new Backend.CPU();
                Backend visionBackend = resolvedConfig.preferGpu() ? new Backend.GPU() : new Backend.CPU();
                File cacheDir = new File(context.getCacheDir(), "litertlm");
                if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                    Log.w(TAG, "failed to create LiteRT-LM cache dir: " + cacheDir.getAbsolutePath());
                }
                File modelFile = new File(resolvedConfig.modelPath());
                Log.i(TAG, "creating LiteRT-LM engine. backend=" + backend
                        + ", visionBackend=" + visionBackend
                        + ", cacheDir=" + cacheDir.getAbsolutePath()
                        + ", modelSizeBytes=" + modelFile.length()
                        + ", canRead=" + modelFile.canRead());
                EngineConfig engineConfig = new EngineConfig(
                        resolvedConfig.modelPath(),
                        backend,
                        visionBackend,
                        null,
                        resolvedConfig.maxTokens(),
                        null,
                        cacheDir.getAbsolutePath()
                );
                Engine newEngine = new Engine(engineConfig);
                Log.i(TAG, "engine created. calling initialize()");
                newEngine.initialize();
                Log.i(TAG, "engine initialized");
                ConversationConfig conversationConfig = new ConversationConfig(
                        null,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        new SamplerConfig(
                                resolvedConfig.topK(),
                                resolvedConfig.topP(),
                                resolvedConfig.temperature(),
                                0
                        ),
                        false
                );

                synchronized (lock) {
                    engine = newEngine;
                    this.conversationConfig = conversationConfig;
                    initConfig = resolvedConfig;
                }
                Log.i(TAG, "initialize succeeded");
                dispatchSuccess(callback, null);
            } catch (Throwable throwable) {
                Log.e(TAG, "initialize failed", throwable);
                close();
                dispatchError(callback, new SmallModelError(
                        SmallModelError.CODE_INITIALIZATION_FAILED,
                        throwable.getMessage(),
                        throwable
                ));
            }
        });
    }

    @Override
    public void analyze(SmallModelRequest request, SmallModelCallback<SmallModelResult> callback) {
        Log.i(TAG, "analyze called. thread=" + Thread.currentThread().getName()
                + ", inferenceRunning=" + inferenceRunning.get());
        if (!inferenceRunning.compareAndSet(false, true)) {
            dispatchError(callback, new SmallModelError(
                    SmallModelError.CODE_INFERENCE_IN_PROGRESS,
                    "已有小模型推理正在执行"
            ));
            return;
        }
        if (request == null || request.image() == null) {
            inferenceRunning.set(false);
            dispatchError(callback, new SmallModelError(
                    SmallModelError.CODE_INVALID_REQUEST,
                    "图片输入不能为空"
            ));
            return;
        }

        executor.execute(() -> analyzeOnExecutor(request, callback));
    }

    private void analyzeOnExecutor(SmallModelRequest request,
                                   SmallModelCallback<SmallModelResult> callback) {
        Log.i(TAG, "analyze started on executor. thread=" + Thread.currentThread().getName());
        Conversation activeConversation = null;
        try {
            synchronized (lock) {
                if (engine == null || conversationConfig == null) {
                    inferenceRunning.set(false);
                    dispatchError(callback, new SmallModelError(
                            SmallModelError.CODE_NOT_INITIALIZED,
                            "小模型尚未初始化"
                    ));
                    return;
                }
                activeConversation = engine.createConversation(conversationConfig);
            }
            Log.i(TAG, "created new conversation for inference. thread="
                    + Thread.currentThread().getName());

            long startedAtMs = System.currentTimeMillis();
            String prompt = request.prompt().trim().isEmpty()
                    ? GemmaUiUnderstandingPrompt.defaultPrompt()
                    : request.prompt();
            Log.i(TAG, "encoding image...");
            long encodeStartedAtMs = System.currentTimeMillis();
            PreparedImage preparedImage = prepareImage(request);
            long imageEncodeMs = System.currentTimeMillis() - encodeStartedAtMs;
            byte[] imageBytes = preparedImage.bytes;
            Log.i(TAG, "image encoded. promptLength=" + prompt.length()
                    + ", imageBytes=" + imageBytes.length);
            List<Content> contents = new ArrayList<>();
            contents.add(new Content.ImageBytes(imageBytes));
            contents.add(new Content.Text(prompt));
            Contents input = Contents.Companion.of(contents);
            Conversation conversationForCallback = activeConversation;

            Log.i(TAG, "calling sendMessage. thread=" + Thread.currentThread().getName());
            long modelCallStartedAtMs = System.currentTimeMillis();
            Message message = conversationForCallback.sendMessage(input, Collections.emptyMap());
            long modelCallMs = System.currentTimeMillis() - modelCallStartedAtMs;
            inferenceRunning.set(false);
            closeQuietly(conversationForCallback);
            String rawText = message == null ? "" : message.toString();
            Log.i(TAG, "inference done. outputLength=" + rawText.length()
                    + ", latencyMs=" + (System.currentTimeMillis() - startedAtMs)
                    + ", thread=" + Thread.currentThread().getName());
            dispatchSuccess(callback, new SmallModelResult(
                    rawText,
                    GemmaUiUnderstandingPrompt.rawTextToYamlCandidate(rawText),
                    System.currentTimeMillis() - startedAtMs,
                    preparedImage.inputWidth,
                    preparedImage.inputHeight,
                    preparedImage.encodedWidth,
                    preparedImage.encodedHeight,
                    preparedImage.bytes.length,
                    imageEncodeMs,
                    modelCallMs
            ));
        } catch (Throwable throwable) {
            inferenceRunning.set(false);
            closeQuietly(activeConversation);
            Log.e(TAG, "sendMessage threw", throwable);
            dispatchError(callback, new SmallModelError(
                    SmallModelError.CODE_INFERENCE_FAILED,
                    throwable.getMessage(),
                    throwable
            ));
        }
    }

    @Override
    public boolean isInitialized() {
        synchronized (lock) {
            return engine != null && conversationConfig != null;
        }
    }

    public SmallModelInitConfig initConfig() {
        synchronized (lock) {
            return initConfig;
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (engine != null) {
                try {
                    engine.close();
                } catch (Throwable ignored) {
                }
                engine = null;
            }
            conversationConfig = null;
            initConfig = null;
            inferenceRunning.set(false);
        }
    }

    private SmallModelError validateInit(Context context, SmallModelInitConfig config) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return new SmallModelError(
                    SmallModelError.CODE_UNSUPPORTED_ANDROID_VERSION,
                    "Gemma-4-E2B-it 本地运行要求 Android 12 及以上"
            );
        }
        if (context == null) {
            return new SmallModelError(
                    SmallModelError.CODE_INITIALIZATION_FAILED,
                    "Context 不能为空"
            );
        }
        File modelFile = new File(config.modelPath());
        Log.i(TAG, "validate model file. exists=" + modelFile.exists()
                + ", canRead=" + modelFile.canRead()
                + ", sizeBytes=" + (modelFile.exists() ? modelFile.length() : -1));
        if (config.modelPath().trim().isEmpty() || !modelFile.exists()) {
            return new SmallModelError(
                    SmallModelError.CODE_MODEL_FILE_MISSING,
                    "模型文件不存在: " + config.modelPath()
            );
        }
        return null;
    }

    private PreparedImage prepareImage(SmallModelRequest request) {
        Bitmap bitmap = request.image();
        Bitmap croppedBitmap = maybeCropBitmap(bitmap, request.options().get(SmallModelRequest.OPTION_IMAGE_CROP));
        Bitmap scaledBitmap = maybeScaleBitmap(croppedBitmap, request.options().get(SmallModelRequest.OPTION_IMAGE_MAX_EDGE));
        Bitmap encodedBitmap = maybeConvertBitmapConfig(
                scaledBitmap,
                request.options().get(SmallModelRequest.OPTION_BITMAP_CONFIG));
        String encoding = request.options().get(SmallModelRequest.OPTION_IMAGE_ENCODING);
        Bitmap.CompressFormat format = Bitmap.CompressFormat.PNG;
        int quality = 100;
        if (SmallModelRequest.OPTION_IMAGE_ENCODING_JPEG_90.equals(encoding)) {
            format = Bitmap.CompressFormat.JPEG;
            quality = 90;
        } else if (SmallModelRequest.OPTION_IMAGE_ENCODING_JPEG_80.equals(encoding)) {
            format = Bitmap.CompressFormat.JPEG;
            quality = 80;
        } else if (SmallModelRequest.OPTION_IMAGE_ENCODING_JPEG_75.equals(encoding)) {
            format = Bitmap.CompressFormat.JPEG;
            quality = 75;
        } else if (SmallModelRequest.OPTION_IMAGE_ENCODING_WEBP_90.equals(encoding)) {
            format = Bitmap.CompressFormat.WEBP_LOSSY;
            quality = 90;
        } else if (SmallModelRequest.OPTION_IMAGE_ENCODING_WEBP_75.equals(encoding)) {
            format = Bitmap.CompressFormat.WEBP_LOSSY;
            quality = 75;
        } else if (SmallModelRequest.OPTION_IMAGE_ENCODING_WEBP_LOSSLESS.equals(encoding)) {
            format = Bitmap.CompressFormat.WEBP_LOSSLESS;
            quality = 100;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        encodedBitmap.compress(format, quality, outputStream);
        byte[] bytes = outputStream.toByteArray();
        int encodedWidth = encodedBitmap.getWidth();
        int encodedHeight = encodedBitmap.getHeight();
        Log.i(TAG, "prepared image bytes. source=" + bitmap.getWidth() + "x" + bitmap.getHeight()
                + ", crop=" + (croppedBitmap == bitmap ? "full" : croppedBitmap.getWidth() + "x" + croppedBitmap.getHeight())
                + ", encoded=" + encodedWidth + "x" + encodedHeight
                + ", encoding=" + (encoding == null || encoding.trim().isEmpty()
                        ? SmallModelRequest.OPTION_IMAGE_ENCODING_PNG : encoding)
                + ", bitmapConfig=" + encodedBitmap.getConfig()
                + ", quality=" + quality
                + ", sizeBytes=" + bytes.length);
        if (encodedBitmap != bitmap && encodedBitmap != croppedBitmap && encodedBitmap != scaledBitmap) {
            encodedBitmap.recycle();
        }
        if (scaledBitmap != bitmap && scaledBitmap != croppedBitmap) {
            scaledBitmap.recycle();
        }
        if (croppedBitmap != bitmap) {
            croppedBitmap.recycle();
        }
        return new PreparedImage(
                bitmap.getWidth(),
                bitmap.getHeight(),
                encodedWidth,
                encodedHeight,
                bytes
        );
    }

    private Bitmap maybeConvertBitmapConfig(Bitmap bitmap, String bitmapConfig) {
        if (!SmallModelRequest.OPTION_BITMAP_CONFIG_RGB_565.equals(bitmapConfig)) {
            return bitmap;
        }
        Bitmap converted = Bitmap.createBitmap(
                bitmap.getWidth(),
                bitmap.getHeight(),
                Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(converted);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bitmap, 0f, 0f, null);
        return converted;
    }

    private Bitmap maybeCropBitmap(Bitmap bitmap, String cropMode) {
        if (cropMode == null
                || cropMode.trim().isEmpty()
                || SmallModelRequest.OPTION_IMAGE_CROP_FULL.equals(cropMode)) {
            return bitmap;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int x = 0;
        int y = 0;
        int cropWidth = width;
        int cropHeight = height;
        if (SmallModelRequest.OPTION_IMAGE_CROP_TOP_40.equals(cropMode)) {
            cropHeight = Math.max(1, Math.round(height * 0.4f));
        } else if (SmallModelRequest.OPTION_IMAGE_CROP_MIDDLE_40.equals(cropMode)) {
            cropHeight = Math.max(1, Math.round(height * 0.4f));
            y = Math.max(0, (height - cropHeight) / 2);
        } else if (SmallModelRequest.OPTION_IMAGE_CROP_BOTTOM_40.equals(cropMode)) {
            cropHeight = Math.max(1, Math.round(height * 0.4f));
            y = Math.max(0, height - cropHeight);
        } else if (SmallModelRequest.OPTION_IMAGE_CROP_CENTER_60.equals(cropMode)) {
            cropWidth = Math.max(1, Math.round(width * 0.6f));
            cropHeight = Math.max(1, Math.round(height * 0.6f));
            x = Math.max(0, (width - cropWidth) / 2);
            y = Math.max(0, (height - cropHeight) / 2);
        } else {
            return bitmap;
        }
        return Bitmap.createBitmap(bitmap, x, y, cropWidth, cropHeight);
    }

    private Bitmap maybeScaleBitmap(Bitmap bitmap, String maxEdgeValue) {
        int maxEdge = parsePositiveInt(maxEdgeValue, 0);
        if (maxEdge <= 0) {
            return bitmap;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int longerEdge = Math.max(width, height);
        if (longerEdge <= maxEdge) {
            return bitmap;
        }
        float scale = maxEdge / (float) longerEdge;
        int targetWidth = Math.max(1, Math.round(width * scale));
        int targetHeight = Math.max(1, Math.round(height * scale));
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
    }

    private int parsePositiveInt(String value, int fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private <T> void dispatchSuccess(SmallModelCallback<T> callback, T value) {
        if (callback != null) {
            callback.onSuccess(value);
        }
    }

    private static void closeQuietly(Conversation conversation) {
        if (conversation != null) {
            try {
                conversation.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private void dispatchError(SmallModelCallback<?> callback, SmallModelError error) {
        if (callback != null) {
            callback.onError(error);
        }
    }

    private static final class PreparedImage {
        private final int inputWidth;
        private final int inputHeight;
        private final int encodedWidth;
        private final int encodedHeight;
        private final byte[] bytes;

        private PreparedImage(int inputWidth, int inputHeight,
                              int encodedWidth, int encodedHeight,
                              byte[] bytes) {
            this.inputWidth = inputWidth;
            this.inputHeight = inputHeight;
            this.encodedWidth = encodedWidth;
            this.encodedHeight = encodedHeight;
            this.bytes = bytes;
        }
    }
}
