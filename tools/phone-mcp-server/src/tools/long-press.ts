import { z } from "zod";
import { longPress } from "../phone-client.js";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";

export function registerLongPress(server: McpServer) {
  server.tool(
    "phone_long_press",
    "Long presses an element identified by ref. Triggers context menus, selection mode, etc.",
    {
      ref: z.string().describe("The element's ref"),
      duration: z.number().optional().describe("Press duration in ms (default: 500)"),
    },
    async ({ ref, duration }) => {
      try {
        const result = await longPress(ref, duration);
        if (result.status === "error") {
          return { content: [{ type: "text" as const, text: `Long press failed: ${result.error}` }] };
        }
        return { content: [{ type: "text" as const, text: `Long pressed ${ref}${duration ? ` for ${duration}ms` : ""}. Call phone_capture_ui to see the updated screen.` }] };
      } catch (err: any) {
        return { content: [{ type: "text" as const, text: `phone_long_press failed: ${err.message}` }] };
      }
    }
  );
}
