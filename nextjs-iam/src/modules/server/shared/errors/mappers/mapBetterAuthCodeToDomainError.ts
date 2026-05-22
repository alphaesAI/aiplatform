import {
  InvalidCredentialsError,
  UserAlreadyExistsError,
  EmailNotVerifiedError,
  SessionExpiredError,
  AccountNotFoundError,
  PasswordPolicyError,
  AuthConfigurationError
} from "@/modules/server/shared/errors/auth/commonAuthErrors"

import type { TBetterAuthErrorCode } from "@/modules/server/auth-provider/betterauth-error-codes"

export function mapBetterAuthCodeToDomainError(code: TBetterAuthErrorCode) {
  switch (code) {
    // --- INVALID CREDENTIALS ---
    case "INVALID_PASSWORD":
    case "INVALID_EMAIL":
    case "INVALID_EMAIL_OR_PASSWORD":
    case "CREDENTIAL_ACCOUNT_NOT_FOUND":
    case "VALIDATION_ERROR":
      throw new InvalidCredentialsError()

    // --- ACCOUNT / USER EXISTS ---
    case "USER_ALREADY_EXISTS":
    case "USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL":
    case "LINKED_ACCOUNT_ALREADY_EXISTS":
    case "SOCIAL_ACCOUNT_ALREADY_LINKED":
      throw new UserAlreadyExistsError()

    // --- EMAIL NOT VERIFIED ---
    case "EMAIL_NOT_VERIFIED":
    case "EMAIL_ALREADY_VERIFIED":
    case "VERIFICATION_EMAIL_NOT_ENABLED":
      throw new EmailNotVerifiedError()

    // --- SESSION / TOKEN ISSUES ---
    case "SESSION_EXPIRED":
    case "SESSION_NOT_FRESH":
    case "INVALID_TOKEN":
      throw new SessionExpiredError()

    // --- ACCOUNT NOT FOUND ---
    case "USER_NOT_FOUND":
    case "ACCOUNT_NOT_FOUND":
    case "USER_EMAIL_NOT_FOUND":
      throw new AccountNotFoundError()

    // --- PASSWORD POLICY ---
    case "PASSWORD_TOO_SHORT":
    case "PASSWORD_TOO_LONG":
    case "USER_ALREADY_HAS_PASSWORD":
      throw new PasswordPolicyError()

    // --- AUTH CONFIGURATION ERRORS ---
    case "PROVIDER_NOT_FOUND":
    case "ID_TOKEN_NOT_SUPPORTED":
    case "INVALID_ORIGIN":
    case "INVALID_CALLBACK_URL":
    case "INVALID_REDIRECT_URL":
    case "INVALID_ERROR_CALLBACK_URL":
    case "INVALID_NEW_USER_CALLBACK_URL":
    case "CALLBACK_URL_REQUIRED":
    case "MISSING_OR_NULL_ORIGIN":
      throw new AuthConfigurationError()

    default:
      // fallback — should never happen if BetterAuth updates its codes
      throw new AuthConfigurationError()
  }
}
