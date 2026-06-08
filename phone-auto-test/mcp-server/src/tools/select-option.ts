import { z } from "zod";
import { selectOption } from "../phone-client.js";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";

export function registerSelectOption(server: McpServer) {
  server.tool(
    "phone_select_option",
    "Selects an option in a dropdown/combobox identified by ref.",
    {
      ref: z.string().describe("The select element's ref"),
      value: z.string().describe("The option value to select"),
    },
    async ({ ref, value }) => {
      try {
        const result = await selectOption(ref, value);
        if (result.status === "error") {
          return { content: [{ type: "text" as const, text: `Select option failed: ${result.error}` }] };
        }
        return { content: [{ type: "text" as const, text: `Selected "${value}" in ${ref}. Call phone_capture_ui to see the updated screen.` }] };
      } catch (err: any) {
        return { content: [{ type: "text" as const, text: `phone_select_option failed: ${err.message}` }] };
      }
    }
  );
}
