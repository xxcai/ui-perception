#!/usr/bin/env node

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";

import { registerCapture } from "./tools/capture.js";
import { registerClick } from "./tools/click.js";
import { registerSwipe } from "./tools/swipe.js";
import { registerTypeText } from "./tools/type-text.js";
import { registerLongPress } from "./tools/long-press.js";
import { registerCheck, registerUncheck } from "./tools/check.js";
import { registerSelectOption } from "./tools/select-option.js";
import { registerPressKey } from "./tools/press-key.js";

const server = new McpServer({
  name: "phone-mcp-server",
  version: "1.0.0",
});

registerCapture(server);
registerClick(server);
registerSwipe(server);
registerTypeText(server);
registerLongPress(server);
registerCheck(server);
registerUncheck(server);
registerSelectOption(server);
registerPressKey(server);

const transport = new StdioServerTransport();
server.connect(transport);
