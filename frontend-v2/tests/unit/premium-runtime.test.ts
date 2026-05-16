import { describe, expect, it } from "vitest";
import { HttpRequestError } from "@/service/request/http";
import {
  isPremiumGateError,
  resolveOptionalRuntimeNotice,
  resolvePremiumNotice,
} from "@/shared/utils/premium-runtime";

function createHttpError(status: number, message: string) {
  return new HttpRequestError({
    message,
    payload: JSON.stringify({ message }),
    status,
    statusText: status === 403 ? "Forbidden" : "Server Error",
  });
}

describe("premium runtime notices", () => {
  it("detects entitlement-related 403 failures only", () => {
    expect(isPremiumGateError(createHttpError(403, "premium entitlement required"))).toBe(true);
    expect(isPremiumGateError(createHttpError(403, "not supported for current plan"))).toBe(true);
    expect(isPremiumGateError(createHttpError(500, "premium entitlement required"))).toBe(false);
    expect(isPremiumGateError(new Error("premium entitlement required"))).toBe(false);
  });

  it("keeps premium failures on the product notice and exposes runtime failures", () => {
    const premiumError = createHttpError(403, "premium entitlement required");
    const runtimeError = new Error("network timeout");

    expect(resolvePremiumNotice(premiumError, "Upgrade required")).toBe("Upgrade required");
    expect(resolvePremiumNotice(runtimeError, "Upgrade required")).toBe("network timeout");
    expect(resolvePremiumNotice("unknown", "Upgrade required")).toBe("Upgrade required");
  });

  it("resolves optional runtime groups without hiding real failures", () => {
    const premiumError = createHttpError(403, "premium entitlement required");
    const runtimeError = new Error("mail sync failed");

    expect(resolveOptionalRuntimeNotice([], "Upgrade required")).toBe("");
    expect(
      resolveOptionalRuntimeNotice(
        [{ status: "rejected", reason: premiumError }],
        "Upgrade required",
      ),
    ).toBe("Upgrade required");
    expect(
      resolveOptionalRuntimeNotice(
        [
          { status: "fulfilled", value: "ok" },
          { status: "rejected", reason: runtimeError },
        ],
        "Upgrade required",
      ),
    ).toBe("mail sync failed");
  });
});
