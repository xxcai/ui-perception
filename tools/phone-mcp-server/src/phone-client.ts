import { ensureAdbForward } from "./adb-bridge.js";

const BASE_URL = "http://localhost:9700";
const TIMEOUT_MS = 10_000;

async function request(path: string, init?: RequestInit): Promise<any> {
  await ensureAdbForward();

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), TIMEOUT_MS);

  try {
    const response = await fetch(`${BASE_URL}${path}`, {
      ...init,
      signal: controller.signal,
    });
    if (!response.ok) {
      const body = await response.text();
      throw new Error(`HTTP ${response.status}: ${body}`);
    }
    return await response.json();
  } finally {
    clearTimeout(timeout);
  }
}

async function post(path: string, body: Record<string, unknown>): Promise<any> {
  return request(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}

export async function captureUi(): Promise<any> {
  return request("/capture");
}

export async function clickElement(ref: string): Promise<any> {
  return post("/click", { ref });
}

export async function swipe(direction: string, ref?: string): Promise<any> {
  const body: Record<string, unknown> = { direction };
  if (ref) body.ref = ref;
  return post("/swipe", body);
}

export async function typeText(ref: string, text: string, clear = true): Promise<any> {
  return post("/type_text", { ref, text, clear });
}

export async function longPress(ref: string, duration = 500): Promise<any> {
  return post("/long_press", { ref, duration });
}

export async function check(ref: string): Promise<any> {
  return post("/check", { ref });
}

export async function uncheck(ref: string): Promise<any> {
  return post("/uncheck", { ref });
}

export async function selectOption(ref: string, value: string): Promise<any> {
  return post("/select_option", { ref, value });
}

export async function pressKey(key: "back"): Promise<any> {
  return post("/press_key", { key });
}
