import { z } from "zod";
import { captureUi } from "../phone-client.js";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";

export function registerCapture(server: McpServer) {
  server.tool(
    "phone_capture_ui",
    "Captures the current phone screen UI and returns it as structured YAML text showing the view hierarchy with roles, names, states, and interaction refs.",
    {},
    async () => {
      try {
        const result = await captureUi();
        if (result.status === "error") {
          return { content: [{ type: "text" as const, text: `Capture failed: ${result.error}` }] };
        }
        const yaml = result.result?.yaml ?? "No YAML content";
        const activity = result.result?.activity ?? "unknown";
        return { content: [{ type: "text" as const, text: `Phone UI captured (Activity: ${activity}):\n\n${yaml}` }] };
      } catch (err: any) {
        return { content: [{ type: "text" as const, text: `phone_capture_ui failed: ${err.message}` }] };
      }
    }
  );
}
