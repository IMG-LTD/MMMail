import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const files = {
  hostedBadge: new URL("../src/design-system/components/HostedBadge.vue", import.meta.url),
  permissionGate: new URL("../src/design-system/components/PermissionGate.vue", import.meta.url),
  premiumBadge: new URL("../src/design-system/components/PremiumBadge.vue", import.meta.url),
  premiumGate: new URL("../src/design-system/components/PremiumGate.vue", import.meta.url),
  productAccessGate: new URL(
    "../src/design-system/components/ProductAccessGate.vue",
    import.meta.url,
  ),
};

test("v2.1 boundary badges render explicit Premium and Hosted states", async () => {
  const [premiumBadge, hostedBadge] = await Promise.all([
    readFile(files.premiumBadge, "utf8"),
    readFile(files.hostedBadge, "utf8"),
  ]);

  assert.match(premiumBadge, /StatusBadge/);
  assert.match(premiumBadge, /tone="premium"/);
  assert.match(premiumBadge, /premium-badge/);
  assert.match(premiumBadge, /label\?: string/);

  assert.match(hostedBadge, /StatusBadge/);
  assert.match(hostedBadge, /tone="hosted"/);
  assert.match(hostedBadge, /hosted-badge/);
  assert.match(hostedBadge, /label\?: string/);
});

test("v2.1 access gates expose visible locked states and action events", async () => {
  const [premiumGate, permissionGate, productAccessGate] = await Promise.all([
    readFile(files.premiumGate, "utf8"),
    readFile(files.permissionGate, "utf8"),
    readFile(files.productAccessGate, "utf8"),
  ]);

  assert.match(premiumGate, /allowed: boolean/);
  assert.match(premiumGate, /defineEmits/);
  assert.match(premiumGate, /upgrade: \[\]/);
  assert.match(premiumGate, /EmptyState/);
  assert.match(premiumGate, /premium-gate/);
  assert.match(premiumGate, /<slot v-if="allowed"/);

  assert.match(permissionGate, /allowed: boolean/);
  assert.match(permissionGate, /requestAccess: \[\]/);
  assert.match(permissionGate, /EmptyState/);
  assert.match(permissionGate, /permission-gate/);
  assert.match(permissionGate, /<slot v-if="allowed"/);

  assert.match(productAccessGate, /enabled: boolean/);
  assert.match(productAccessGate, /productKey: string/);
  assert.match(productAccessGate, /requestAccess: \[\]/);
  assert.match(productAccessGate, /EmptyState/);
  assert.match(productAccessGate, /product-access-gate/);
  assert.match(productAccessGate, /<slot v-if="enabled"/);
});
