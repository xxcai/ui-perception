package com.hh.uiperception.smallmodelplugin.gemma;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;

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
 * Gemma-4-E4B-it 的 LiteRT-LM Java 封装。
 */
public final class Gemma4E4BClient implements SmallModelVisionClient {

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
            SmallModelError validationError = validateInit(context, resolvedConfig);
            if (validationError != null) {
                dispatchError(callback, validationError);
                return;
            }

            synchronized (lock) {
                if (engine != null && conversation != null) {
                    dispatchSuccess(callback, null);
                    return;
                }
            }

            try {
                Backend backend = resolvedConfig.preferGpu() ? new Backend.GPU() : new Backend.CPU();
                EngineConfig engineConfig = new EngineConfig(
                        resolvedConfig.modelPath(),
                        backend,
                        new Backend.GPU(),
                        null,
                        resolvedConfig.maxTokens(),
                        context.getExternalFilesDir(null).getAbsolutePath()
                );
                Engine newEngine = new Engine(engineConfig);
                newEngine.initialize();
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

                synchronized (lock) {
                    engine = newEngine;
                    conversation = newConversation;
                    initConfig = resolvedConfig;
                }
                dispatchSuccess(callback, null);
            } catch (Throwable throwable) {
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
        contents.add(new Content.ImageBytes(toPngBytes(request.image())));
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
                    "Gemma-4-E4B-it 本地运行要求 Android 12 及以上"
            );
        }
        if (context == null) {
            return new SmallModelError(
                    SmallModelError.CODE_INITIALIZATION_FAILED,
                    "Context 不能为空"
            );
        }
        if (config.modelPath().trim().isEmpty() || !new File(config.modelPath()).exists()) {
            return new SmallModelError(
                    SmallModelError.CODE_MODEL_FILE_MISSING,
                    "模型文件不存在: " + config.modelPath()
            );
        }
        return null;
    }

    private byte[] toPngBytes(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        return outputStream.toByteArray();
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
