import {
  TSignupResponseDtoSchema,
  TSigninResponseDtoSchema,
  TSignoutResponseDtoSchema,
  TSigninWithSocialResponseDtoSchema,
  TSendEmailVerificationDtoSchema
} from "@/modules/entities/schemas/auth"
import {
  TSendEmailVerificationPayload,
  TSigninEmailPayload,
  TSigninWithSocialPayload,
  TSignupEmailPayload
} from "@/modules/entities/types/auth"

export interface IAuthService {
  signUpWithEmail(
    payload: TSignupEmailPayload
  ): Promise<TSignupResponseDtoSchema>
  signInWithEmail(
    payload: TSigninEmailPayload
  ): Promise<TSigninResponseDtoSchema>
  signInWithSocial(
    payload: TSigninWithSocialPayload
  ): Promise<TSigninWithSocialResponseDtoSchema>
  signOut(): Promise<TSignoutResponseDtoSchema>
  sendEmailVerification(
    payload: TSendEmailVerificationPayload
  ): Promise<TSendEmailVerificationDtoSchema>
}
