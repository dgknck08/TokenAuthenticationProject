import { formatPrice } from "@/lib/utils";
import { describe, expect, it } from "vitest";

describe("formatPrice", () => {
  it("formats numbers as currency", () => {
    expect(formatPrice(1299)).toContain("1,299");
  });
});
