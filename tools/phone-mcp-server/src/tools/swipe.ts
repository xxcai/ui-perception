import { z } from "zod";
import { swipe } from "../phone-client.js";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";

export function registerSwipe(server: McpServer) {
  server.tool(
    "phone_swipe",
    "Swipes on the phone screen in the specified direction. Direction = finger movement: 'up' means finger slides up, content scrolls down.",
    {
      direction: z.enum(["up", "down", "left", "right"]).describe("Finger movement direction"),
      ref: z.string().optional().describe("Optional: swipe within a specific scrollable element"),
    },
    async ({ direction, ref }) => {
      try {
        const result = await swipe(direction, ref);
        if (result.status === "error") {
          return { content: [{ type: "text" as const, text: `Swipe failed: ${result.error}` }] };
        }
        return { content: [{ type: "text" as const, text: `Swiped ${direction}${ref ? ` on ${ref}` : ""}. Call phone_capture_ui to see the updated screen.` }] };
      } catch (err: any) {
        return { content: [{ type: "text" as const, text: `phone_swipe failed: ${err.message}` }] };
      }
    }
  );
}
