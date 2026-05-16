declare namespace Api {
  namespace Billing {
    type BillingCycle = 'MONTHLY' | 'ANNUAL';
    type PaymentMethodType = 'CARD' | 'PAYPAL' | 'BITCOIN' | 'CASH';
    type CommercialLicenseState = 'MISSING' | 'ACTIVE' | 'EXPIRED' | 'INVALID';

    interface QuotePayload {
      offerCode: string;
      billingCycle: BillingCycle;
      seatCount: number;
    }

    interface CheckoutDraftPayload extends QuotePayload {
      organizationName?: string;
      domainName?: string;
    }

    interface PaymentMethodPayload {
      methodType: PaymentMethodType;
      displayLabel: string;
      brand?: string;
      lastFour?: string;
      expiresAt?: string;
      makeDefault?: boolean;
    }

    interface DefaultPaymentMethodPayload {
      paymentMethodId: number;
    }

    interface SubscriptionActionPayload {
      actionCode: string;
    }

    interface CommercialLicenseUploadPayload {
      licenseKey: string;
    }

    interface CommercialLicenseStatus {
      orgId: string;
      state: CommercialLicenseState;
      edition: string;
      features: string[];
      externalBillingStatus: string;
      expiresAt?: string;
      syncedAt?: string;
      requiredAction?: string;
      message?: string;
    }

    interface PricingOffer {
      originalPriceValue: string;
      priceNote: string;
      billingCycles: BillingCycle[];
      defaultSeatCount: number;
      seatEditable: boolean;
      organizationRequired: boolean;
      marketingBadge: string;
      highlights: string[];
      enabledProducts: string[];
    }

    interface InvoiceLine {
      label: string;
      amountCents: number;
    }

    interface InvoiceSummary {
      currencyCode: string;
      billingCycle: string;
      seatCount: number;
      billingMonths: number;
      subtotalCents: number;
      discountCents: number;
      totalCents: number;
      lineItems: InvoiceLine[];
    }

    interface EntitlementSummary {
      offerCode: string;
      linkedPlanCode: string;
      primaryProductCode: string;
      supportTier: string;
      workspaceMode: string;
      seatCount: number;
      prioritySupport: boolean;
      unlockedProducts: string[];
      highlights: string[];
    }

    interface OnboardingSummary {
      onboardingMode: string;
      nextAction: string;
      organizationRequired: boolean;
      checklistCodes: string[];
    }

    interface Quote {
      offerCode: string;
      offerName: string;
      quoteStatus: string;
      checkoutMode: string;
      currencyCode: string;
      billingCycle: string;
      seatCount: number;
      marketingBadge: string;
      invoiceSummary: InvoiceSummary;
      entitlementSummary: EntitlementSummary;
      onboardingSummary: OnboardingSummary;
    }

    interface CheckoutDraft {
      offerName?: string;
      quoteStatus?: string;
      checkoutMode?: string;
      currencyCode?: string;
      invoiceSummary?: InvoiceSummary;
      entitlementSummary?: EntitlementSummary;
      onboardingSummary?: OnboardingSummary;
    }
  }
}
