declare namespace Api {
  namespace Settings {
    interface UserProfile {
      displayName: string;
      signature: string;
      timezone: string;
      preferredLocale: string;
      mailAddressMode: string;
      autoSaveSeconds: number;
      undoSendSeconds: number;
      driveVersionRetentionCount: number;
      driveVersionRetentionDays: number;
    }

    interface SecuritySettings {
      mfaEnabled: boolean;
      recoveryEmail: string;
    }

    interface DeviceSession {
      id: string;
      deviceName: string;
      lastActiveAt: string;
      current: boolean;
    }

    interface NotificationSettings {
      emailDigest: boolean;
      productUpdates: boolean;
    }

    interface WebPushPublicKey {
      publicKey: string;
    }

    interface WebPushRegistrationPayload {
      auth: string;
      endpoint: string;
      label?: string;
      p256dh: string;
      userAgent?: string;
    }

    interface WebPushSubscription {
      subscriptionId: number;
      endpointHash: string;
      label: string | null;
      lastSuccessAt: string;
      lastFailureAt: string;
      lastErrorMessage: string;
      createdAt: string;
      updatedAt: string;
    }

    interface WebPushTestDelivery {
      deliveryId: string;
      attempted: number;
      delivered: number;
    }
  }
}
