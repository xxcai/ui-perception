package com.hh.uiperception.sdk;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.hh.uiperception.sdk.internal.DebugDomHandler;
import com.hh.uiperception.sdk.internal.DebugExecJsHandler;
import com.hh.uiperception.sdk.internal.OpenUriHandler;
import com.hh.uiperception.sdk.internal.OperationHandler;
import com.hh.uiperception.sdk.internal.RawCaptureHandler;

public class PerceptionSdkInitProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        Application app = (Application) getContext().getApplicationContext();
        PerceptionSdk.init(app);
        PerceptionSdk.startHttpServer();
        return false;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        String body = arg != null ? arg
                : (extras != null ? extras.getString("body") : null);
        String json = dispatch(method, body);
        Bundle result = new Bundle();
        result.putString("response", json);
        return result;
    }

    private String dispatch(String method, String body) {
        switch (method) {
            case "ping":
                return "{\"status\":\"success\",\"result\":{\"version\":\"1.0.0\"}}";
            case "capture":
                return PerceptionSdk.capture().toJson();
            case "capture_raw":
                return RawCaptureHandler.capture();
            case "debug_dom":
                return DebugDomHandler.capture();
            case "click":
                return body != null ? OperationHandler.handleClick(body)
                        : errorJson("Missing body");
            case "long_press":
                return body != null ? OperationHandler.handleLongPress(body)
                        : errorJson("Missing body");
            case "swipe":
                return body != null ? OperationHandler.handleSwipe(body)
                        : errorJson("Missing body");
            case "type_text":
                return body != null ? OperationHandler.handleTypeText(body)
                        : errorJson("Missing body");
            case "check":
                return body != null ? OperationHandler.handleCheck(body)
                        : errorJson("Missing body");
            case "uncheck":
                return body != null ? OperationHandler.handleUncheck(body)
                        : errorJson("Missing body");
            case "select_option":
                return body != null ? OperationHandler.handleSelectOption(body)
                        : errorJson("Missing body");
            case "press_key":
                return body != null ? OperationHandler.handlePressKey(body)
                        : errorJson("Missing body");
            case "exec_js":
                return body != null ? DebugExecJsHandler.execute(body)
                        : errorJson("Missing body");
            case "open_uri":
                return body != null ? OpenUriHandler.handleOpenUri(body)
                        : errorJson("Missing body");
            default:
                return errorJson("Unknown method: " + method);
        }
    }

    private static String errorJson(String msg) {
        return "{\"status\":\"error\",\"error\":\"" + msg + "\"}";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
