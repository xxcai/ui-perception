package com.hh.uiperception.smallmodelplugin.gemma;

import android.content.Context;
import android.graphics.Bitmap;
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
import com.google.ai.edge.litertlm.MessageCallback;
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
    private Conversation conversation;
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
                if (engine != null && conversation != null) {
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
                Log.i(TAG, "engine initialized. creating conversation");
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
                Conversation newConversation = newEngine.createConversation(conversationConfig);
                Log.i(TAG, "conversation created");

                synchronized (lock) {
                    engine = newEngine;
                    conversation = newConversation;
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
        if (!inferenceRunning.compareAndSet(false, true)) {
            dispatchError(callback, new SmallModelError(
                    SmallModelError.CODE_INFERENCE_IN_PROGRESS,
                    "已有小模型推理正在执行"
            ));
            return;
        }

        Conversation activeConversation;
        synchronized (lock) {
            activeConversation = conversation;
        }
        if (activeConversation == null) {
            inferenceRunning.set(false);
            dispatchError(callback, new SmallModelError(
                    SmallModelError.CODE_NOT_INITIALIZED,
                    "小模型尚未初始化"
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

        long startedAtMs = System.currentTimeMillis();
        String prompt = request.prompt().trim().isEmpty()
                ? GemmaUiUnderstandingPrompt.defaultPrompt()
                : request.prompt();
        List<Content> contents = new ArrayList<>();
        contents.add(new Content.ImageBytes(toImageBytes(request)));
        contents.add(new Content.Text(prompt));
        Contents input = Contents.Companion.of(contents);
        StringBuilder rawBuilder = new StringBuilder();

        try {
            activeConversation.sendMessageAsync(input, new MessageCallback() {
                @Override
                public void onMessage(Message message) {
                    rawBuilder.append(message.toString());
                }

                @Override
                public void onDone() {
                    inferenceRunning.set(false);
                    String rawText = rawBuilder.toString();
                    dispatchSuccess(callback, new SmallModelResult(
                            rawText,
                            GemmaUiUnderstandingPrompt.rawTextToYamlCandidate(rawText),
                            System.currentTimeMillis() - startedAtMs
                    ));
                }

                @Override
                public void onError(Throwable throwable) {
                    inferenceRunning.set(false);
                    dispatchError(callback, new SmallModelError(
                            SmallModelError.CODE_INFERENCE_FAILED,
                            throwable.getMessage(),
                            throwable
                    ));
                }
            }, Collections.emptyMap());
        } catch (Throwable throwable) {
            inferenceRunning.set(false);
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
            return engine != null && conversation != null;
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
            if (conversation != null) {
                try {
                    conversation.close();
                } catch (Throwable ignored) {
                }
                conversation = null;
            }
            if (engine != null) {
                try {
                    engine.close();
                } catch (Throwable ignored) {
                }
                engine = null;
            }
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

    private byte[] toImageBytes(SmallModelRequest request) {
        Bitmap bitmap = request.image();
        Bitmap croppedBitmap = maybeCropBitmap(bitmap, request.options().get(SmallModelRequest.OPTION_IMAGE_CROP));
        Bitmap encodedBitmap = maybeScaleBitmap(croppedBitmap, request.options().get(SmallModelRequest.OPTION_IMAGE_MAX_EDGE));
        String encoding = request.options().get(SmallModelRequest.OPTION_IMAGE_ENCODING);
        Bitmap.CompressFormat format = Bitmap.CompressFormat.PNG;
        int quality = 100;
        if (SmallModelRequest.OPTION_IMAGE_ENCODING_JPEG_90.equals(encoding)) {
            format = Bitmap.CompressFormat.JPEG;
            quality = 90;
        } else if (SmallModelRequest.OPTION_IMAGE_ENCODING_JPEG_80.equals(encoding)) {
            format = Bitmap.CompressFormat.JPEG;
            quality = 80;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        encodedBitmap.compress(format, quality, outputStream);
        byte[] bytes = outputStream.toByteArray();
        Log.i(TAG, "prepared image bytes. source=" + bitmap.getWidth() + "x" + bitmap.getHeight()
                + ", crop=" + (croppedBitmap == bitmap ? "full" : croppedBitmap.getWidth() + "x" + croppedBitmap.getHeight())
                + ", encoded=" + encodedBitmap.getWidth() + "x" + encodedBitmap.getHeight()
                + ", encoding=" + (format == Bitmap.CompressFormat.PNG ? "png" : "jpeg")
                + ", quality=" + quality
                + ", sizeBytes=" + bytes.length);
        if (encodedBitmap != bitmap) {
            encodedBitmap.recycle();
        }
        if (croppedBitmap != bitmap && croppedBitmap != encodedBitmap) {
            croppedBitmap.recycle();
        }
        return bytes;
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

    private void dispatchError(SmallModelCallback<?> callback, SmallModelError error) {
        if (callback != null) {
            callback.onError(error);
        }
    }
}
