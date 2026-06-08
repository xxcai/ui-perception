import { execFile } from "node:child_process";
import type { Plugin, ToolDef } from "@opencode-ai/plugin";
import { tool } from "@opencode-ai/plugin";

// ── ADB Bridge ──────────────────────────────────────────

const DEFAULT_PORT = 9700;
const ADB_TIMEOUT = 5000;
let forwardingActive = false;

function execCmd(
  cmd: string,
  args: string[],
  timeout: number
): Promise<{ code: number; stdout: string; stderr: string }> {
  return new Promise((resolve) => {
    const proc = execFile(cmd, args, { timeout, shell: true }, (error, stdout, stderr) => {
      resolve({
        code: error ? (error as NodeJS.ErrnoException).code === "ETIMEDOUT" ? -1 : 1 : 0,
        stdout: stdout ?? "",
        stderr: stderr ?? "",
      });
    });
    proc.on("error", () => resolve({ code: -1, stdout: "", stderr: "command not found" }));
  });
}

async function ensureAdbForward(): Promise<void> {
  if (forwardingActive) return;
  const check = await execCmd("adb", ["devices"], ADB_TIMEOUT);
  if (check.code !== 0) {
    throw new Error("ADB not found. Install Android SDK platform-tools and ensure adb is in PATH.");
  }
  const forward = await execCmd("adb", ["forward", `tcp:${DEFAULT_PORT}`, `tcp:${DEFAULT_PORT}`], ADB_TIMEOUT);
  if (forward.code !== 0) {
    throw new Error(`ADB forward failed: ${forward.stderr}`);
  }
  forwardingActive = true;
}

// ── Phone Client ────────────────────────────────────────

const BASE_URL = "http://localhost:9700";
const TIMEOUT_MS = 10_000;

async function phoneRequest(path: string, init?: RequestInit): Promise<any> {
  await ensureAdbForward();
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const response = await fetch(`${BASE_URL}${path}`, { ...init, signal: controller.signal });
    if (!response.ok) {
      const body = await response.text();
      throw new Error(`HTTP ${response.status}: ${body}`);
    }
    return await response.json();
  } finally {
    clearTimeout(timeout);
  }
}

async function phonePost(path: string, body: Record<string, unknown>): Promise<any> {
  return phoneRequest(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}

// ── Tools ───────────────────────────────────────────────

const phone_capture_ui = tool({
  description:
    "Captures the current phone screen UI and returns it as structured YAML text showing the view hierarchy with roles, names, states, and interaction refs.",
  args: {},
  async execute() {
    try {
      const result = await phoneRequest("/capture");
      if (result.status === "error") return `Capture failed: ${result.error}`;
      const yaml = result.result?.yaml ?? "No YAML content";
      const activity = result.result?.activity ?? "unknown";
      return `Phone UI captured (Activity: ${activity}):\n\n${yaml}`;
    } catch (err: any) {
      return `phone_capture_ui failed: ${err.message}`;
    }
  },
});

const phone_click = tool({
  description:
    "Clicks an interactive element on the phone screen identified by its ref (e.g. 'n1'). The ref comes from the YAML output of phone_capture_ui.",
  args: {
    ref: tool.schema.string().describe("The ref identifier from YAML, e.g. 'n1'"),
  },
  async execute({ ref }) {
    try {
      const result = await phonePost("/click", { ref });
      if (result.status === "error") return `Click failed: ${result.error}`;
      return `Clicked element ${ref}. Call phone_capture_ui to see the updated screen.`;
    } catch (err: any) {
      return `phone_click failed: ${err.message}`;
    }
  },
});

const phone_swipe = tool({
  description:
    "Swipes on the phone screen in the specified direction. Direction = finger movement: 'up' means finger slides up, content scrolls down.",
  args: {
    direction: tool.schema.enum(["up", "down", "left", "right"]).describe("Finger movement direction"),
    ref: tool.schema.string().optional().describe("Optional: swipe within a specific scrollable element"),
  },
  async execute({ direction, ref }) {
    try {
      const body: Record<string, unknown> = { direction };
      if (ref) body.ref = ref;
      const result = await phonePost("/swipe", body);
      if (result.status === "error") return `Swipe failed: ${result.error}`;
      return `Swiped ${direction}${ref ? ` on ${ref}` : ""}. Call phone_capture_ui to see the updated screen.`;
    } catch (err: any) {
      return `phone_swipe failed: ${err.message}`;
    }
  },
});

const phone_type_text = tool({
  description:
    "Types text into an input field on the phone screen identified by its ref. Clears existing text by default.",
  args: {
    ref: tool.schema.string().describe("The input element's ref identifier"),
    text: tool.schema.string().describe("Text to type"),
    clear: tool.schema.boolean().optional().describe("Clear existing text first (default: true)"),
  },
  async execute({ ref, text, clear }) {
    try {
      const result = await phonePost("/type_text", { ref, text, clear: clear ?? true });
      if (result.status === "error") return `Type text failed: ${result.error}`;
      return `Typed text into ${ref}. Call phone_capture_ui to see the updated screen.`;
    } catch (err: any) {
      return `phone_type_text failed: ${err.message}`;
    }
  },
});

const phone_long_press = tool({
  description: "Long presses an element on the phone screen identified by its ref.",
  args: {
    ref: tool.schema.string().describe("The element's ref identifier"),
    duration: tool.schema.number().optional().describe("Press duration in ms (default: 500)"),
  },
  async execute({ ref, duration }) {
    try {
      const result = await phonePost("/long_press", { ref, duration: duration ?? 500 });
      if (result.status === "error") return `Long press failed: ${result.error}`;
      return `Long pressed ${ref}. Call phone_capture_ui to see the updated screen.`;
    } catch (err: any) {
      return `phone_long_press failed: ${err.message}`;
    }
  },
});

const phone_check = tool({
  description: "Sets a checkbox to checked state (idempotent).",
  args: {
    ref: tool.schema.string().describe("The checkbox element's ref identifier"),
  },
  async execute({ ref }) {
    try {
      const result = await phonePost("/check", { ref });
      if (result.status === "error") return `Check failed: ${result.error}`;
      return `Checked ${ref}. Call phone_capture_ui to see the updated screen.`;
    } catch (err: any) {
      return `phone_check failed: ${err.message}`;
    }
  },
});

const phone_uncheck = tool({
  description: "Sets a checkbox to unchecked state (idempotent).",
  args: {
    ref: tool.schema.string().describe("The checkbox element's ref identifier"),
  },
  async execute({ ref }) {
    try {
      const result = await phonePost("/uncheck", { ref });
      if (result.status === "error") return `Uncheck failed: ${result.error}`;
      return `Unchecked ${ref}. Call phone_capture_ui to see the updated screen.`;
    } catch (err: any) {
      return `phone_uncheck failed: ${err.message}`;
    }
  },
});

const phone_select_option = tool({
  description: "Selects an option in a dropdown/combobox on the phone screen.",
  args: {
    ref: tool.schema.string().describe("The select element's ref identifier"),
    value: tool.schema.string().describe("The option value to select"),
  },
  async execute({ ref, value }) {
    try {
      const result = await phonePost("/select_option", { ref, value });
      if (result.status === "error") return `Select option failed: ${result.error}`;
      return `Selected '${value}' in ${ref}. Call phone_capture_ui to see the updated screen.`;
    } catch (err: any) {
      return `phone_select_option failed: ${err.message}`;
    }
  },
});

const phone_press_key = tool({
  description: "Presses the back key. Dismisses keyboard, closes dialogs, navigates back.",
  args: {
    key: tool.schema.literal("back").describe("Only 'back' is supported"),
  },
  async execute({ key }) {
    try {
      const result = await phonePost("/press_key", { key });
      if (result.status === "error") return `Press key failed: ${result.error}`;
      return `Pressed ${key}. Call phone_capture_ui to see the updated screen.`;
    } catch (err: any) {
      return `phone_press_key failed: ${err.message}`;
    }
  },
});

// ── Plugin Export ───────────────────────────────────────

export const PhoneAutoTest: Plugin = async ({ client }) => {
  return {
    tool: {
      phone_capture_ui,
      phone_click,
      phone_swipe,
      phone_type_text,
      phone_long_press,
      phone_check,
      phone_uncheck,
      phone_select_option,
      phone_press_key,
    },

    "tool.execute.before": async (input, output) => {
      if (input.tool.startsWith("phone_")) {
        await client.app.log({
          body: {
            service: "phone-auto-test",
            level: "info",
            message: `tool call: ${input.tool}`,
            extra: { args: output.args },
          },
        });
      }
    },

    "tool.execute.after": async (input, output) => {
      if (input.tool.startsWith("phone_")) {
        await client.app.log({
          body: {
            service: "phone-auto-test",
            level: "info",
            message: `tool result: ${input.tool}`,
            extra: { result: output.result },
          },
        });
      }
    },
  };
};
