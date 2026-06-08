import { z } from "zod";
import { pressKey } from "../phone-client.js";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";

export function registerPressKey(server: McpServer) {
  server.tool(
    "phone_press_key",
    "Presses the back key. Dismisses keyboard, closes dialogs, navigates back.",
    { key: z.literal("back").describe("Only 'back' is supported") },
    async ({ key }) => {
      try {
        const result = await pressKey(key);
        if (result.status === "error") {
          return { content: [{ type: "text" as const, text: `Press key failed: ${result.error}` }] };
        }
        return { content: [{ type: "text" as const, text: `Pressed ${key}. Call phone_capture_ui to see the updated screen.` }] };
      } catch (err: any) {
        return { content: [{ type: "text" as const, text: `phone_press_key failed: ${err.message}` }] };
      }
    }
  );
}
