package com.mmmail.server.service;

public final class PassBusinessConstants {

    public static final String ORG_ROLE_OWNER = "OWNER";
    public static final String ORG_ROLE_ADMIN = "ADMIN";
    public static final String ORG_ROLE_MEMBER = "MEMBER";
    public static final String ORG_STATUS_ACTIVE = "ACTIVE";

    public static final String VAULT_ROLE_MANAGER = "MANAGER";
    public static final String VAULT_ROLE_MEMBER = "MEMBER";

    public static final String SCOPE_PERSONAL = "PERSONAL";
    public static final String SCOPE_SHARED = "SHARED";

    public static final String ITEM_TYPE_LOGIN = "LOGIN";
    public static final String ITEM_TYPE_PASSWORD = "PASSWORD";
    public static final String ITEM_TYPE_NOTE = "NOTE";
    public static final String ITEM_TYPE_CARD = "CARD";
    public static final String ITEM_TYPE_ALIAS = "ALIAS";
    public static final String ITEM_TYPE_PASSKEY = "PASSKEY";

    public static final String POLICY_MINIMUM_PASSWORD_LENGTH = "passMinimumPasswordLength";
    public static final String POLICY_MAXIMUM_PASSWORD_LENGTH = "passMaximumPasswordLength";
    public static final String POLICY_REQUIRE_UPPERCASE = "passRequireUppercase";
    public static final String POLICY_REQUIRE_DIGITS = "passRequireDigits";
    public static final String POLICY_REQUIRE_SYMBOLS = "passRequireSymbols";
    public static final String POLICY_ALLOW_MEMORABLE_PASSWORDS = "passAllowMemorablePasswords";
    public static final String POLICY_ALLOW_EXTERNAL_SHARING = "passAllowExternalSharing";
    public static final String POLICY_ALLOW_ITEM_SHARING = "passAllowItemSharing";
    public static final String POLICY_ALLOW_SECURE_LINKS = "passAllowSecureLinks";
    public static final String POLICY_ALLOW_MEMBER_VAULT_CREATION = "passAllowMemberVaultCreation";
    public static final String POLICY_ALLOW_EXPORT = "passAllowExport";
    public static final String POLICY_FORCE_TWO_FACTOR = "passForceTwoFactor";
    public static final String POLICY_ALLOW_PASSKEYS = "passAllowPasskeys";
    public static final String POLICY_ALLOW_ALIASES = "passAllowAliases";

    public static final int DEFAULT_MINIMUM_PASSWORD_LENGTH = 14;
    public static final int DEFAULT_MAXIMUM_PASSWORD_LENGTH = 64;
    public static final boolean DEFAULT_ALLOW_MEMORABLE_PASSWORDS = true;
    public static final boolean DEFAULT_ALLOW_ITEM_SHARING = true;

    private PassBusinessConstants() {
    }
}
