package com.hh.uiperception.sdk.internal;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PerceptionHttpServer {

    private static final String TAG = "PerceptionSdk";
    private static final int THREAD_POOL_SIZE = 3;

    private ServerSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executor;
    private int port;

    public void start(int port) {
        if (running.get()) {
            Log.w(TAG, "Server already running on port " + this.port);
            return;
        }
        this.port = port;
        try {
            serverSocket = new ServerSocket(port);
            executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            running.set(true);
            executor.submit(this::acceptLoop);
            Log.i(TAG, "HTTP server started on port " + port);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start server on port " + port, e);
        }
    }

    public void stop() {
        if (!running.getAndSet(false)) return;
        try {
            serverSocket.close();
        } catch (Exception ignored) {
        }
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
        Log.i(TAG, "HTTP server stopped");
    }

    public boolean isRunning() {
        return running.get();
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket client = serverSocket.accept();
                executor.submit(() -> handleClient(client));
            } catch (SocketException e) {
                if (running.get()) Log.e(TAG, "Accept error", e);
            } catch (Exception e) {
                Log.e(TAG, "Accept error", e);
            }
        }
    }

    private void handleClient(Socket client) {
        try (Socket s = client) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            OutputStream out = s.getOutputStream();

            String requestLine = reader.readLine();
            if (requestLine == null) return;

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                sendJson(out, 400, "{\"status\":\"error\",\"error\":\"Bad Request\"}");
                return;
            }

            String method = parts[0];
            String path = parts[1].split("\\?")[0];

            // Parse headers
            int contentLength = 0;
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(headerLine.substring(15).trim());
                }
            }

            // Read body if present
            String body = null;
            if (contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                reader.read(bodyChars, 0, contentLength);
                body = new String(bodyChars);
            }

            String responseJson;
            if ("/ping".equals(path)) {
                responseJson = "{\"status\":\"success\",\"result\":{\"version\":\"1.0.0\"}}";
            } else if ("/capture".equals(path)) {
                responseJson = com.hh.uiperception.sdk.PerceptionSdk.capture().toJson();
            } else if ("/capture-raw".equals(path)) {
                responseJson = RawCaptureHandler.capture();
            } else if ("/debug-dom".equals(path)) {
                responseJson = DebugDomHandler.capture();
            } else if ("/click".equals(path) && "POST".equals(method)) {
                responseJson = body != null ? OperationHandler.handleClick(body)
                        : OperationResponse.error("Missing body").toJson();
            } else if ("/swipe".equals(path) && "POST".equals(method)) {
                responseJson = body != null ? OperationHandler.handleSwipe(body)
                        : OperationResponse.error("Missing body").toJson();
            } else {
                responseJson = "{\"status\":\"error\",\"error\":\"Not found: " + path + "\"}";
            }

            sendJson(out, 200, responseJson);
        } catch (Exception e) {
            Log.e(TAG, "Error handling client", e);
        }
    }

    private void sendJson(OutputStream out, int statusCode, String body) {
        try {
            String reason = statusCode == 200 ? "OK"
                    : statusCode == 400 ? "Bad Request"
                    : statusCode == 405 ? "Method Not Allowed"
                    : "Internal Server Error";
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            String header = "HTTP/1.1 " + statusCode + " " + reason + "\r\n"
                    + "Content-Type: application/json; charset=utf-8\r\n"
                    + "Content-Length: " + bodyBytes.length + "\r\n"
                    + "Access-Control-Allow-Origin: *\r\n"
                    + "Connection: close\r\n"
                    + "\r\n";
            out.write(header.getBytes(StandardCharsets.UTF_8));
            out.write(bodyBytes);
            out.flush();
        } catch (Exception e) {
            Log.e(TAG, "Error sending response", e);
        }
    }
}
