import { execFile } from "node:child_process";
import { appendFileSync, mkdirSync } from "node:fs";
import { join } from "node:path";
import type { Plugin } from "@opencode-ai/plugin";
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

// ── Tool Helper ─────────────────────────────────────────

async function phoneAction(name: string, path: string, body: Record<string, unknown>, ok: string) {
  try {
    const result = await phonePost(path, body);
    if (result.status === "error") return `${name} failed: ${result.error}`;
    return ok;
  } catch (err: any) {
    return `${name} failed: ${err.message}`;
  }
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
  args: { ref: tool.schema.string().describe("The ref identifier from YAML, e.g. 'n1'") },
  async execute({ ref }) {
    return phoneAction("phone_click", "/click", { ref }, `Clicked ${ref}. Call phone_capture_ui to see the updated screen.`);
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
    const body: Record<string, unknown> = { direction };
    if (ref) body.ref = ref;
    return phoneAction("phone_swipe", "/swipe", body, `Swiped ${direction}. Call phone_capture_ui to see the updated screen.`);
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
    return phoneAction("phone_type_text", "/type_text", { ref, text, clear: clear ?? true }, `Typed into ${ref}. Call phone_capture_ui to see the updated screen.`);
  },
});

const phone_long_press = tool({
  description: "Long presses an element on the phone screen identified by its ref.",
  args: {
    ref: tool.schema.string().describe("The element's ref identifier"),
    duration: tool.schema.number().optional().describe("Press duration in ms (default: 500)"),
  },
  async execute({ ref, duration }) {
    return phoneAction("phone_long_press", "/long_press", { ref, duration: duration ?? 500 }, `Long pressed ${ref}. Call phone_capture_ui to see the updated screen.`);
  },
});

const phone_check = tool({
  description: "Sets a checkbox to checked state (idempotent).",
  args: { ref: tool.schema.string().describe("The checkbox element's ref identifier") },
  async execute({ ref }) {
    return phoneAction("phone_check", "/check", { ref }, `Checked ${ref}. Call phone_capture_ui to see the updated screen.`);
  },
});

const phone_uncheck = tool({
  description: "Sets a checkbox to unchecked state (idempotent).",
  args: { ref: tool.schema.string().describe("The checkbox element's ref identifier") },
  async execute({ ref }) {
    return phoneAction("phone_uncheck", "/uncheck", { ref }, `Unchecked ${ref}. Call phone_capture_ui to see the updated screen.`);
  },
});

const phone_select_option = tool({
  description: "Selects an option in a dropdown/combobox on the phone screen.",
  args: {
    ref: tool.schema.string().describe("The select element's ref identifier"),
    value: tool.schema.string().describe("The option value to select"),
  },
  async execute({ ref, value }) {
    return phoneAction("phone_select_option", "/select_option", { ref, value }, `Selected '${value}' in ${ref}. Call phone_capture_ui to see the updated screen.`);
  },
});

const phone_press_key = tool({
  description: "Presses the back key. Dismisses keyboard, closes dialogs, navigates back.",
  args: { key: tool.schema.literal("back").describe("Only 'back' is supported") },
  async execute({ key }) {
    return phoneAction("phone_press_key", "/press_key", { key }, `Pressed ${key}. Call phone_capture_ui to see the updated screen.`);
  },
});

// ── Session Logger ──────────────────────────────────────

const LOG_DIR = "test-logs";
let logFilePath: string | null = null;

function appendLog(type: string, data: Record<string, unknown>) {
  if (!logFilePath) return;
  appendFileSync(logFilePath, JSON.stringify({ ts: new Date().toISOString(), type, data }) + "\n", "utf8");
}

// ── Plugin Export ───────────────────────────────────────

export const PhoneAutoTest: Plugin = async ({ directory }) => {
  const logDir = join(directory ?? process.cwd(), LOG_DIR);
  mkdirSync(logDir, { recursive: true });
  const timestamp = new Date().toISOString().replace(/[:.]/g, "-");
  logFilePath = join(logDir, `${timestamp}.jsonl`);
  appendLog("session_start", {});

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
        appendLog("tool_call", { tool: input.tool, args: output.args });
      }
    },

    "tool.execute.after": async (input, output) => {
      if (input.tool.startsWith("phone_")) {
        appendLog("tool_result", { tool: input.tool, result: output.result });
      }
    },
  };
};
