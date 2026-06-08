import { z } from "zod";
import { clickElement } from "../phone-client.js";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";

export function registerClick(server: McpServer) {
  server.tool(
    "phone_click",
    "Clicks an interactive element on the phone screen identified by its ref (e.g. 'n1'). The ref comes from the YAML output of phone_capture_ui.",
    { ref: z.string().describe("The ref identifier from YAML, e.g. 'n1'") },
    async ({ ref }) => {
      try {
        const result = await clickElement(ref);
        if (result.status === "error") {
          return { content: [{ type: "text" as const, text: `Click failed: ${result.error}` }] };
        }
        return { content: [{ type: "text" as const, text: `Clicked element ${ref}. Call phone_capture_ui to see the updated screen.` }] };
      } catch (err: any) {
        return { content: [{ type: "text" as const, text: `phone_click failed: ${err.message}` }] };
      }
    }
  );
}
