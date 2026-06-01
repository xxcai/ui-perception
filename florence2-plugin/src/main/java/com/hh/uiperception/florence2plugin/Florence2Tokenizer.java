package com.hh.uiperception.florence2plugin;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Florence-2 tokenizer — loads vocab + added_tokens from tokenizer.json.
 */
public class Florence2Tokenizer {

    private final Map<String, Long> vocab;
    private final Map<Long, String> idToToken;
    public final long eosTokenId;
    public final long bosTokenId;
    public final long padTokenId;

    public Florence2Tokenizer(Context context) throws Exception {
        this(loadTokenizerJson(context));
    }

    public Florence2Tokenizer(String json) throws Exception {
        this(new JSONObject(json));
    }

    private Florence2Tokenizer(JSONObject root) throws Exception {
        vocab = new HashMap<>();
        idToToken = new HashMap<>();

        // Load base vocab from model.vocab
        JSONObject vocabObj = root.getJSONObject("model").getJSONObject("vocab");
        Iterator<String> keys = vocabObj.keys();
        while (keys.hasNext()) {
            String token = keys.next();
            long id = vocabObj.getLong(token);
            vocab.put(token, id);
            idToToken.put(id, token);
        }

        // Load added_tokens (special tokens like <cap>, <dcap>, <loc_*> etc.)
        if (root.has("added_tokens")) {
            JSONArray addedTokens = root.getJSONArray("added_tokens");
            for (int i = 0; i < addedTokens.length(); i++) {
                JSONObject entry = addedTokens.getJSONObject(i);
                String content = entry.getString("content");
                long id = entry.getLong("id");
                vocab.put(content, id);
                idToToken.put(id, content);
            }
        }

        eosTokenId = vocab.getOrDefault("</s>", 2L);
        bosTokenId = vocab.getOrDefault("<s>", 0L);
        padTokenId = vocab.getOrDefault("<pad>", 1L);
    }

    /**
     * Returns the caption task prompt: [BOS, &lt;cap&gt;].
     */
    public long[] encodeCaptionPrompt() {
        Long captionId = vocab.get("<cap>");
        if (captionId != null) {
            return new long[]{bosTokenId, captionId};
        }
        // fallback: search for cap-related tokens
        for (Map.Entry<String, Long> e : vocab.entrySet()) {
            if (e.getKey().equals("<CAPTION>") || e.getKey().equals("<dcap>")) {
                return new long[]{bosTokenId, e.getValue()};
            }
        }
        return new long[]{bosTokenId};
    }

    /**
     * Returns the detailed caption task prompt: [BOS, &lt;dcap&gt;].
     */
    public long[] encodeDetailedCaptionPrompt() {
        Long id = vocab.get("<dcap>");
        if (id != null) {
            return new long[]{bosTokenId, id};
        }
        return encodeCaptionPrompt();
    }

    public long[] encode(String text) {
        Long id = vocab.get(text);
        if (id != null) {
            return new long[]{id};
        }
        id = vocab.get("Ġ" + text);
        if (id != null) {
            return new long[]{id};
        }
        long[] ids = new long[text.length()];
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            Long chId = vocab.get(ch);
            if (chId == null) chId = vocab.get("Ġ" + ch);
            ids[i] = chId != null ? chId : 3L;
        }
        return ids;
    }

    public String decode(long[] ids) {
        StringBuilder sb = new StringBuilder();
        for (long id : ids) {
            String token = idToToken.get(id);
            if (token == null) continue;
            if (token.equals("</s>") || token.equals("<s>") || token.equals("<pad>")) continue;
            sb.append(token.replace("Ġ", " "));
        }
        return sb.toString().trim();
    }

    private static String loadTokenizerJson(Context context) throws Exception {
        File file = new File(
                context.getExternalFilesDir(null),
                "models/florence2/tokenizer.json"
        );
        if (file.exists()) {
            try (InputStream is = new FileInputStream(file)) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        try (InputStream is = context.getAssets().open("tokenizer.json")) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
