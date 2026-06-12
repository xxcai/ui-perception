import http from "node:http";

const PORT = 9800;

const server = http.createServer((req, res) => {
  res.setHeader("Content-Type", "application/json; charset=utf-8");
  res.setHeader("Access-Control-Allow-Origin", "*");

  if (req.method === "OPTIONS") {
    res.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
    res.setHeader("Access-Control-Allow-Headers", "Content-Type");
    res.writeHead(204);
    res.end();
    return;
  }

  const url = new URL(req.url, `http://localhost:${PORT}`);

  if (url.pathname === "/ping" && req.method === "GET") {
    res.writeHead(200);
    res.end(JSON.stringify({ status: "success", result: { version: "1.0.0" } }));
    return;
  }

  if (url.pathname === "/task" && req.method === "POST") {
    let body = "";
    req.on("data", (chunk) => { body += chunk; });
    req.on("end", () => {
      try {
        const parsed = JSON.parse(body);
        const task = parsed.task ?? "";
        console.log(`[${new Date().toISOString()}] Received task: ${task}`);

        // Mock agent response — in production this would invoke LLM + SDK
        const result = {
          status: "success",
          result: {
            message: `Agent processed: "${task}"`,
            timestamp: new Date().toISOString(),
          },
        };

        res.writeHead(200);
        res.end(JSON.stringify(result));
      } catch {
        res.writeHead(400);
        res.end(JSON.stringify({ status: "error", error: "Invalid JSON" }));
      }
    });
    return;
  }

  res.writeHead(404);
  res.end(JSON.stringify({ status: "error", error: `Not found: ${url.pathname}` }));
});

server.listen(PORT, () => {
  console.log(`Agent test server running on http://localhost:${PORT}`);
  console.log(`Run: adb reverse tcp:${PORT} tcp:${PORT}`);
});
