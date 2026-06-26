export class Bounds {
  readonly left: number;
  readonly top: number;
  readonly right: number;
  readonly bottom: number;

  constructor(left: number, top: number, right: number, bottom: number) {
    this.left = left;
    this.top = top;
    this.right = right;
    this.bottom = bottom;
  }

  static fromLtRb(left: number, top: number, right: number, bottom: number): Bounds {
    return new Bounds(left, top, right, bottom);
  }

  static fromXYWidthHeight(x: number, y: number, width: number, height: number): Bounds {
    return new Bounds(
      Math.round(x),
      Math.round(y),
      Math.round(x + width),
      Math.round(y + height),
    );
  }

  get width(): number {
    return this.right - this.left;
  }

  get height(): number {
    return this.bottom - this.top;
  }

  get centerX(): number {
    return Math.floor((this.left + this.right) / 2);
  }

  get centerY(): number {
    return Math.floor((this.top + this.bottom) / 2);
  }

  get isValid(): boolean {
    return this.right > this.left && this.bottom > this.top;
  }

  get snapshotValue(): string {
    return `${this.left},${this.top},${this.right},${this.bottom}`;
  }

  static parse(value: string | null | undefined): Bounds | null {
    if (value === null || value === undefined) {
      return null;
    }
    const trimmed = value.trim();
    if (trimmed.length === 0) {
      return null;
    }
    const re = /\[(-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)\](?:,\s*)?\[(-?\d+(?:\.\d+)?),\s*(-?\d+(?:\.\d+)?)\]/;
    const match = trimmed.match(re);
    if (match === null || match.length !== 5) {
      return null;
    }
    const l = Math.round(Number.parseFloat(match[1]));
    const t = Math.round(Number.parseFloat(match[2]));
    const r = Math.round(Number.parseFloat(match[3]));
    const b = Math.round(Number.parseFloat(match[4]));
    if (Number.isNaN(l) || Number.isNaN(t) || Number.isNaN(r) || Number.isNaN(b)) {
      return null;
    }
    return new Bounds(l, t, r, b);
  }

  equals(other: Bounds): boolean {
    return this.left === other.left
      && this.top === other.top
      && this.right === other.right
      && this.bottom === other.bottom;
  }
}
