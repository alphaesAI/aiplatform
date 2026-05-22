import z from "zod"
import { ZodSocialProviders } from "../../enums/auth/auth.enum"
import { TransportOptionsSchema } from "../transport"
import { BaseSigninOrSignupSchema } from "./base.schema"
import { UserSchema } from "./reusable.schema"

// form schemas (used in client side forms)
export const SignupFormSchema = BaseSigninOrSignupSchema.pick({
  name: true,
  email: true,
  password: true,
  rememberMe: true
})
export type TSignupFormSchema = z.infer<typeof SignupFormSchema>

export const SigninFormSchema = BaseSigninOrSignupSchema.pick({
  email: true,
  password: true,
  rememberMe: true
})
export type TSigninFormSchema = z.infer<typeof SigninFormSchema>

export const ForgetPasswordOrMagicLinkFormSchema =
  BaseSigninOrSignupSchema.pick({
    email: true
  })
export type TForgetPasswordOrMagicLinkFormSchema = z.infer<
  typeof ForgetPasswordOrMagicLinkFormSchema
>

// ------------------------------------------------------- //

// validation schemas (used in and controllers)
export const SignupValidationSchema = BaseSigninOrSignupSchema.pick({
  name: true,
  email: true,
  password: true,
  rememberMe: true
})
export type TSignupValidationSchema = z.infer<typeof SignupValidationSchema>

export const SigninValidationSchema = BaseSigninOrSignupSchema.pick({
  email: true,
  password: true,
  rememberMe: true
})
export type TSigninValidationSchema = z.infer<typeof SigninValidationSchema>

export const SigninWithSocialValidationSchema = z.object({
  provider: ZodSocialProviders
})
export type TSigninWithSocialValidationSchema = z.infer<
  typeof SigninWithSocialValidationSchema
>

export const SendVerificationEmailValidationSchema = z.object({
  email: z.string().email()
})
export type TSendVerificationEmailValidationSchema = z.infer<
  typeof SendVerificationEmailValidationSchema
>

// ------------------------------------------------------- //

// Server Action Schema
export const SignupActionSchema = z.object({
  payload: SignupValidationSchema,
  transportOptions: TransportOptionsSchema.optional()
})
export type TSignupActionSchema = z.infer<typeof SignupActionSchema>

export const SigninActionSchema = z.object({
  payload: SigninValidationSchema,
  transportOptions: TransportOptionsSchema.optional()
})
export type TSigninActionSchema = z.infer<typeof SigninActionSchema>

export const SignoutActionSchema = z.object({
  transportOptions: TransportOptionsSchema.optional()
})
export type TSignoutActionSchema = z.infer<typeof SignoutActionSchema>

export const SendEmailVerificationActionSchema = z.object({
  payload: SendVerificationEmailValidationSchema,
  transportOptions: TransportOptionsSchema.optional()
})
export type TSendEmailVerificationActionSchema = z.infer<
  typeof SendEmailVerificationActionSchema
>

// ------------------------------------------------------- //

// return DTO validator
export const SignupResponseDtoSchema = z.union([
  z.object({
    token: z.null(),
    user: UserSchema
  }),
  z.object({
    token: z.string(),
    user: UserSchema
  })
])
export type TSignupResponseDtoSchema = z.infer<typeof SignupResponseDtoSchema>

export const SigninResponseDtoSchema = z.object({
  redirect: z.boolean(),
  token: z.string(),
  url: z.string().optional(),
  user: UserSchema
})
export type TSigninResponseDtoSchema = z.infer<typeof SigninResponseDtoSchema>

export const SigninWithSocialResponseDtoSchema = z.union([
  z.object({
    redirect: z.boolean(),
    token: z.string(),
    url: z.undefined(),
    user: UserSchema
  }),
  z.object({
    redirect: z.boolean(),
    url: z.string()
  })
])
export type TSigninWithSocialResponseDtoSchema = z.infer<
  typeof SigninWithSocialResponseDtoSchema
>

export const SignoutResponseDtoSchema = z.object({
  success: z.boolean(),
  url: z.string().nullable()
})
export type TSignoutResponseDtoSchema = z.infer<typeof SignoutResponseDtoSchema>

export const SendEmailVerificationDtoSchema = z.object({
  success: z.boolean()
})
export type TSendEmailVerificationDtoSchema = z.infer<
  typeof SendEmailVerificationDtoSchema
>
