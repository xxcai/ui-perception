package com.hh.uiperception.smallmodelplugin.experiment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 图标识别实验数据的 JSON 转换。
 */
public final class IconExperimentJson {

    private static final String KEY_TESTSET_ID = "testset_id";
    private static final String KEY_IMAGE = "image";
    private static final String KEY_TARGETS = "targets";
    private static final String KEY_ID = "id";
    private static final String KEY_BOUNDS = "bounds";
    private static final String KEY_EXPECTED = "expected";
    private static final String KEY_ACCEPTABLE = "acceptable";
    private static final String KEY_RUN_ID = "run_id";
    private static final String KEY_CREATED_AT_MS = "created_at_ms";
    private static final String KEY_INPUT_MODE = "input_mode";
    private static final String KEY_TARGET_COUNT = "target_count";
    private static final String KEY_IMAGE_MAX_EDGE = "image_max_edge";
    private static final String KEY_IMAGE_ENCODING = "image_encoding";
    private static final String KEY_BITMAP_CONFIG = "bitmap_config";
    private static final String KEY_INPUT_IMAGE_WIDTH = "input_image_width";
    private static final String KEY_INPUT_IMAGE_HEIGHT = "input_image_height";
    private static final String KEY_ENCODED_IMAGE_WIDTH = "encoded_image_width";
    private static final String KEY_ENCODED_IMAGE_HEIGHT = "encoded_image_height";
    private static final String KEY_ENCODED_IMAGE_BYTES = "encoded_image_bytes";
    private static final String KEY_IMAGE_PREPARE_MS = "image_prepare_ms";
    private static final String KEY_IMAGE_ENCODE_MS = "image_encode_ms";
    private static final String KEY_MODEL_CALL_MS = "model_call_ms";
    private static final String KEY_MODEL_LOAD_MS = "model_load_ms";
    private static final String KEY_INFERENCE_MS = "inference_ms";
    private static final String KEY_TOTAL_MS = "total_ms";
    private static final String KEY_BATCH_SIZE = "batch_size";
    private static final String KEY_BATCH_COUNT = "batch_count";
    private static final String KEY_PROMPT = "prompt";
    private static final String KEY_RAW_OUTPUT = "raw_output";
    private static final String KEY_PARSED_OUTPUT = "parsed_output";
    private static final String KEY_MANUAL_SCORES = "manual_scores";
    private static final String KEY_SCORE = "score";
    private static final String KEY_NOTE = "note";
    private static final String KEY_ERROR = "error";
    private static final String KEY_CODE = "code";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_DESC = "desc";

    private IconExperimentJson() {
    }

    public static IconExperimentTestSet parseTestSet(String jsonText) throws JSONException {
        JSONObject json = new JSONObject(jsonText == null ? "{}" : jsonText);
        JSONArray targetsJson = json.optJSONArray(KEY_TARGETS);
        List<IconTarget> targets = new ArrayList<>();
        if (targetsJson != null) {
            for (int i = 0; i < targetsJson.length(); i++) {
                JSONObject targetJson = targetsJson.optJSONObject(i);
                if (targetJson != null) {
                    targets.add(parseTarget(targetJson));
                }
            }
        }
        return new IconExperimentTestSet(
                json.optString(KEY_TESTSET_ID, ""),
                json.optString(KEY_IMAGE, ""),
                targets
        );
    }

    public static String toJson(IconExperimentRunResult result) throws JSONException {
        return toJsonObject(result).toString(2);
    }

    public static JSONObject toJsonObject(IconExperimentRunResult result) throws JSONException {
        JSONObject json = new JSONObject();
        if (result == null) {
            return json;
        }
        json.put(KEY_RUN_ID, result.runId());
        json.put(KEY_CREATED_AT_MS, result.createdAtMs());
        json.put(KEY_TESTSET_ID, result.testsetId());
        json.put(KEY_IMAGE, result.image());
        json.put(KEY_INPUT_MODE, result.inputMode().name());
        json.put(KEY_TARGET_COUNT, result.targetCount());
        json.put(KEY_IMAGE_MAX_EDGE, result.imageMaxEdge());
        json.put(KEY_IMAGE_ENCODING, result.imageEncoding());
        json.put(KEY_BITMAP_CONFIG, result.bitmapConfig());
        json.put(KEY_INPUT_IMAGE_WIDTH, result.inputImageWidth());
        json.put(KEY_INPUT_IMAGE_HEIGHT, result.inputImageHeight());
        json.put(KEY_ENCODED_IMAGE_WIDTH, result.encodedImageWidth());
        json.put(KEY_ENCODED_IMAGE_HEIGHT, result.encodedImageHeight());
        json.put(KEY_ENCODED_IMAGE_BYTES, result.encodedImageBytes());
        json.put(KEY_IMAGE_PREPARE_MS, result.imagePrepareMs());
        json.put(KEY_IMAGE_ENCODE_MS, result.imageEncodeMs());
        json.put(KEY_MODEL_CALL_MS, result.modelCallMs());
        json.put(KEY_MODEL_LOAD_MS, result.modelLoadMs());
        json.put(KEY_INFERENCE_MS, result.inferenceMs());
        json.put(KEY_TOTAL_MS, result.totalMs());
        json.put(KEY_BATCH_SIZE, result.batchSize());
        json.put(KEY_BATCH_COUNT, result.batchCount());
        json.put(KEY_PROMPT, result.prompt());
        json.put(KEY_TARGETS, targetsToJson(result.targets()));
        json.put(KEY_RAW_OUTPUT, result.rawOutput());
        json.put(KEY_PARSED_OUTPUT, parsedOutputToJson(result.parsedOutput()));
        json.put(KEY_MANUAL_SCORES, manualScoresToJson(result.manualScores()));
        json.put(KEY_ERROR, errorToJson(result.error()));
        return json;
    }

    public static IconExperimentRunResult parseRunResult(String jsonText) throws JSONException {
        JSONObject json = new JSONObject(jsonText == null ? "{}" : jsonText);
        IconExperimentRunResult.Builder builder = IconExperimentRunResult.builder()
                .setRunId(json.optString(KEY_RUN_ID, ""))
                .setCreatedAtMs(json.optLong(KEY_CREATED_AT_MS, 0L))
                .setTestsetId(json.optString(KEY_TESTSET_ID, ""))
                .setImage(json.optString(KEY_IMAGE, ""))
                .setInputMode(parseInputMode(json.optString(KEY_INPUT_MODE, "")))
                .setTargetCount(json.optInt(KEY_TARGET_COUNT, 0))
                .setImageMaxEdge(json.optInt(KEY_IMAGE_MAX_EDGE, 0))
                .setImageEncoding(json.optString(KEY_IMAGE_ENCODING, ""))
                .setBitmapConfig(json.optString(KEY_BITMAP_CONFIG, ""))
                .setInputImageWidth(json.optInt(KEY_INPUT_IMAGE_WIDTH, 0))
                .setInputImageHeight(json.optInt(KEY_INPUT_IMAGE_HEIGHT, 0))
                .setEncodedImageWidth(json.optInt(KEY_ENCODED_IMAGE_WIDTH, 0))
                .setEncodedImageHeight(json.optInt(KEY_ENCODED_IMAGE_HEIGHT, 0))
                .setEncodedImageBytes(json.optInt(KEY_ENCODED_IMAGE_BYTES, 0))
                .setImagePrepareMs(json.optLong(KEY_IMAGE_PREPARE_MS, -1L))
                .setImageEncodeMs(json.optLong(KEY_IMAGE_ENCODE_MS, -1L))
                .setModelCallMs(json.optLong(KEY_MODEL_CALL_MS, -1L))
                .setModelLoadMs(json.optLong(KEY_MODEL_LOAD_MS, -1L))
                .setInferenceMs(json.optLong(KEY_INFERENCE_MS, -1L))
                .setTotalMs(json.optLong(KEY_TOTAL_MS, -1L))
                .setBatchSize(json.optInt(KEY_BATCH_SIZE, 0))
                .setBatchCount(json.optInt(KEY_BATCH_COUNT, 1))
                .setPrompt(json.optString(KEY_PROMPT, ""))
                .setTargets(parseTargets(json.optJSONArray(KEY_TARGETS)))
                .setRawOutput(json.optString(KEY_RAW_OUTPUT, ""))
                .setParsedOutput(parseParsedOutput(json.optJSONArray(KEY_PARSED_OUTPUT)))
                .setManualScores(parseManualScores(json.optJSONArray(KEY_MANUAL_SCORES)));
        JSONObject errorJson = json.optJSONObject(KEY_ERROR);
        if (errorJson != null) {
            builder.setError(new IconExperimentError(
                    errorJson.optString(KEY_CODE, ""),
                    errorJson.optString(KEY_MESSAGE, "")
            ));
        }
        return builder.build();
    }

    private static IconInputMode parseInputMode(String value) {
        if (value == null || value.trim().isEmpty()) {
            return IconInputMode.FULL_IMAGE;
        }
        try {
            return IconInputMode.valueOf(value.trim());
        } catch (IllegalArgumentException ignored) {
            return IconInputMode.FULL_IMAGE;
        }
    }

    private static IconTarget parseTarget(JSONObject json) {
        return new IconTarget(
                json.optString(KEY_ID, ""),
                parseBounds(json.optJSONArray(KEY_BOUNDS)),
                json.optString(KEY_EXPECTED, ""),
                parseStringArray(json.optJSONArray(KEY_ACCEPTABLE))
        );
    }

    private static List<IconTarget> parseTargets(JSONArray jsonArray) {
        List<IconTarget> targets = new ArrayList<>();
        if (jsonArray == null) {
            return targets;
        }
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject item = jsonArray.optJSONObject(i);
            if (item != null) {
                targets.add(parseTarget(item));
            }
        }
        return targets;
    }

    private static IconBounds parseBounds(JSONArray jsonArray) {
        if (jsonArray == null || jsonArray.length() < 4) {
            return null;
        }
        return new IconBounds(
                jsonArray.optInt(0),
                jsonArray.optInt(1),
                jsonArray.optInt(2),
                jsonArray.optInt(3)
        );
    }

    private static List<String> parseStringArray(JSONArray jsonArray) {
        List<String> values = new ArrayList<>();
        if (jsonArray == null) {
            return values;
        }
        for (int i = 0; i < jsonArray.length(); i++) {
            String value = jsonArray.optString(i, "").trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private static JSONArray targetsToJson(List<IconTarget> targets) throws JSONException {
        JSONArray array = new JSONArray();
        if (targets == null) {
            return array;
        }
        for (IconTarget target : targets) {
            array.put(targetToJson(target));
        }
        return array;
    }

    private static JSONObject targetToJson(IconTarget target) throws JSONException {
        JSONObject json = new JSONObject();
        if (target == null) {
            return json;
        }
        json.put(KEY_ID, target.id());
        json.put(KEY_BOUNDS, boundsToJson(target.bounds()));
        json.put(KEY_EXPECTED, target.expected());
        JSONArray acceptable = new JSONArray();
        for (String value : target.acceptable()) {
            acceptable.put(value);
        }
        json.put(KEY_ACCEPTABLE, acceptable);
        return json;
    }

    private static JSONArray boundsToJson(IconBounds bounds) {
        JSONArray array = new JSONArray();
        if (bounds == null) {
            return array;
        }
        array.put(bounds.left());
        array.put(bounds.top());
        array.put(bounds.right());
        array.put(bounds.bottom());
        return array;
    }

    private static JSONArray parsedOutputToJson(List<ParsedIconDescription> parsedOutput)
            throws JSONException {
        JSONArray array = new JSONArray();
        if (parsedOutput == null) {
            return array;
        }
        for (ParsedIconDescription item : parsedOutput) {
            JSONObject json = new JSONObject();
            json.put(KEY_ID, item.id());
            json.put(KEY_DESC, item.desc());
            array.put(json);
        }
        return array;
    }

    private static List<ParsedIconDescription> parseParsedOutput(JSONArray jsonArray) {
        List<ParsedIconDescription> parsed = new ArrayList<>();
        if (jsonArray == null) {
            return parsed;
        }
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject item = jsonArray.optJSONObject(i);
            if (item != null) {
                parsed.add(new ParsedIconDescription(
                        item.optString(KEY_ID, ""),
                        item.optString(KEY_DESC, "")
                ));
            }
        }
        return parsed;
    }

    private static JSONArray manualScoresToJson(List<IconManualScore> manualScores)
            throws JSONException {
        JSONArray array = new JSONArray();
        if (manualScores == null) {
            return array;
        }
        for (IconManualScore item : manualScores) {
            JSONObject json = new JSONObject();
            json.put(KEY_ID, item.id());
            json.put(KEY_SCORE, item.score() == null ? JSONObject.NULL : item.score());
            json.put(KEY_NOTE, item.note());
            array.put(json);
        }
        return array;
    }

    private static List<IconManualScore> parseManualScores(JSONArray jsonArray) {
        List<IconManualScore> scores = new ArrayList<>();
        if (jsonArray == null) {
            return scores;
        }
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject item = jsonArray.optJSONObject(i);
            if (item != null) {
                Integer score = item.isNull(KEY_SCORE) ? null : item.optInt(KEY_SCORE);
                scores.add(new IconManualScore(
                        item.optString(KEY_ID, ""),
                        score,
                        item.optString(KEY_NOTE, "")
                ));
            }
        }
        return scores;
    }

    private static Object errorToJson(IconExperimentError error) throws JSONException {
        if (error == null) {
            return JSONObject.NULL;
        }
        JSONObject json = new JSONObject();
        json.put(KEY_CODE, error.code());
        json.put(KEY_MESSAGE, error.message());
        return json;
    }
}
