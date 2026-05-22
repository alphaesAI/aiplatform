import { TSocialProviders } from "../../enums/auth/auth.enum"

// backend schemas (used in class methods and interfaces)
// payloads
export type TBaseSigninOrSignupPayload = {
  name: string
  email: string
  password: string
  rememberMe: boolean
}

export type TSignupEmailPayload = Pick<
  TBaseSigninOrSignupPayload,
  "email" | "name" | "password" | "rememberMe"
>

export type TSigninEmailPayload = Pick<
  TBaseSigninOrSignupPayload,
  "email" | "password" | "rememberMe"
>

export type TSigninWithSocialPayload = {
  provider: TSocialProviders
}

export type TSendEmailVerificationPayload = {
  email: string
}
