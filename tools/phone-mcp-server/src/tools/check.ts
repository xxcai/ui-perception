import { z } from "zod";
import { check, uncheck } from "../phone-client.js";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";

export function registerCheck(server: McpServer) {
  server.tool(
    "phone_check",
    "Checks a checkbox identified by ref. Idempotent — sets to checked state.",
    { ref: z.string().describe("The checkbox element's ref") },
    async ({ ref }) => {
      try {
        const result = await check(ref);
        if (result.status === "error") {
          return { content: [{ type: "text" as const, text: `Check failed: ${result.error}` }] };
        }
        return { content: [{ type: "text" as const, text: `Checked ${ref}. Call phone_capture_ui to verify.` }] };
      } catch (err: any) {
        return { content: [{ type: "text" as const, text: `phone_check failed: ${err.message}` }] };
      }
    }
  );
}

export function registerUncheck(server: McpServer) {
  server.tool(
    "phone_uncheck",
    "Unchecks a checkbox identified by ref. Idempotent — sets to unchecked state.",
    { ref: z.string().describe("The checkbox element's ref") },
    async ({ ref }) => {
      try {
        const result = await uncheck(ref);
        if (result.status === "error") {
          return { content: [{ type: "text" as const, text: `Uncheck failed: ${result.error}` }] };
        }
        return { content: [{ type: "text" as const, text: `Unchecked ${ref}. Call phone_capture_ui to verify.` }] };
      } catch (err: any) {
        return { content: [{ type: "text" as const, text: `phone_uncheck failed: ${err.message}` }] };
      }
    }
  );
}
