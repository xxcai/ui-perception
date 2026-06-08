import { z } from "zod";
import { typeText } from "../phone-client.js";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";

export function registerTypeText(server: McpServer) {
  server.tool(
    "phone_type_text",
    "Types text into an input field identified by ref. Clears existing text by default.",
    {
      ref: z.string().describe("The input element's ref"),
      text: z.string().describe("Text to type"),
      clear: z.boolean().optional().describe("Clear existing text first (default: true)"),
    },
    async ({ ref, text, clear }) => {
      try {
        const result = await typeText(ref, text, clear);
        if (result.status === "error") {
          return { content: [{ type: "text" as const, text: `Type text failed: ${result.error}` }] };
        }
        return { content: [{ type: "text" as const, text: `Typed "${text}" into ${ref}. Call phone_capture_ui to see the updated screen.` }] };
      } catch (err: any) {
        return { content: [{ type: "text" as const, text: `phone_type_text failed: ${err.message}` }] };
      }
    }
  );
}
