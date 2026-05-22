import { BASE_ERROR_CODES } from "better-auth"

export const BETTERAUTH_ERROR_CODES = BASE_ERROR_CODES
export type TBetterAuthErrorCode = keyof typeof BETTERAUTH_ERROR_CODES

export type TBetterAuthSdkError = {
  status?: unknown
  code?: unknown
  statusCode?: unknown
  body?: {
    code?: unknown
    message?: unknown
  }
}

export function isBetterAuthErrorCode(
  code: unknown
): code is TBetterAuthErrorCode {
  return (
    // INFO: code in BETTERAUTH_ERROR_CODES -> "in" will also match inherited keys (rare but possible).
    typeof code === "string" &&
    Object.prototype.hasOwnProperty.call(BETTERAUTH_ERROR_CODES, code)
  )
}
