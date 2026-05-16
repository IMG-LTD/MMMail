import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import StatusBadge from "@/design-system/components/StatusBadge.vue";

describe("StatusBadge", () => {
  it("renders the default neutral badge", () => {
    const wrapper = mount(StatusBadge, {
      props: {
        label: "ready",
      },
    });

    expect(wrapper.text()).toBe("ready");
    expect(wrapper.classes()).toContain("status-badge--neutral");
    expect(wrapper.classes()).not.toContain("status-badge--compact");
  });

  it("renders compact tone variants", () => {
    const wrapper = mount(StatusBadge, {
      props: {
        compact: true,
        label: "hosted",
        tone: "hosted",
      },
    });

    expect(wrapper.classes()).toContain("status-badge--hosted");
    expect(wrapper.classes()).toContain("status-badge--compact");
  });
});
