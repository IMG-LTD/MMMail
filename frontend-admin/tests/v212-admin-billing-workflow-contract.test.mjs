import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 admin billing service exposes quote, checkout, payment, and subscription APIs', async () => {
  const service = await read('src/service/api/billing.ts');

  assert.match(service, /createSuiteBillingQuote/);
  assert.match(service, /\/api\/v1\/suite\/billing\/quote/);
  assert.match(service, /createSuiteCheckoutDraft/);
  assert.match(service, /\/api\/v1\/suite\/billing\/checkout-draft/);
  assert.match(service, /addSuiteBillingPaymentMethod/);
  assert.match(service, /\/api\/v1\/suite\/billing\/payment-methods/);
  assert.match(service, /setSuiteBillingDefaultPaymentMethod/);
  assert.match(service, /\/api\/v1\/suite\/billing\/payment-methods\/default/);
  assert.match(service, /executeSuiteBillingSubscriptionAction/);
});

test('v2.1.2 admin billing routes require BILLING_ADMIN role', async () => {
  const routes = await read('src/router/routes/index.ts');

  for (const routeName of [
    'admin_billing',
    'admin_billing_subscriptions',
    'admin_billing_invoices',
    'admin_billing_payment_methods',
    'admin_billing_offers'
  ]) {
    assert.match(routes, new RegExp(`name: '${routeName}'`));
  }

  assert.match(routes, /path: '\/admin\/billing'/);
  assert.match(routes, /roles: \['BILLING_ADMIN'\]/);
  assert.match(routes, /component: 'layout\.base\$view\.admin'/);
});

test('v2.1.2 admin page binds billing offers, quote preview, draft creation, and payment methods', async () => {
  const page = await read('src/views/admin/index.vue');

  assert.match(page, /listSuitePricingOffers/);
  assert.match(page, /createSuiteBillingQuote/);
  assert.match(page, /createSuiteCheckoutDraft/);
  assert.match(page, /addSuiteBillingPaymentMethod/);
  assert.match(page, /setSuiteBillingDefaultPaymentMethod/);
  assert.match(page, /subscriptionActionDrawerOpen/);
  assert.match(page, /quotePreview/);
  assert.match(page, /paymentMethodModel/);
  assert.match(page, /NDrawer/);
  assert.match(page, /NForm/);
});
