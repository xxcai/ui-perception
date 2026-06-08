import { execFile } from "node:child_process";

const DEFAULT_PORT = 9700;
const ADB_TIMEOUT = 5000;

let forwardingActive = false;

function execCmd(
  cmd: string,
  args: string[],
  timeout: number
): Promise<{ code: number; stdout: string; stderr: string }> {
  return new Promise((resolve) => {
    const proc = execFile(cmd, args, { timeout }, (error, stdout, stderr) => {
      resolve({
        code: error ? (error as NodeJS.ErrnoException).code === "ETIMEDOUT" ? -1 : 1 : 0,
        stdout: stdout ?? "",
        stderr: stderr ?? "",
      });
    });
    proc.on("error", () => resolve({ code: -1, stdout: "", stderr: "command not found" }));
  });
}

export async function ensureAdbForward(): Promise<void> {
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

export function resetForwardingState(): void {
  forwardingActive = false;
}
