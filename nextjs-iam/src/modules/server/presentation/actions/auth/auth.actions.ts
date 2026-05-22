"use server";

import {
  SendEmailVerificationActionSchema,
  SigninActionSchema,
  SigninWithSocialValidationSchema,
  SignoutActionSchema,
  SignupActionSchema,
} from "@/modules/entities/schemas/auth";
import { createServerAction } from "zsa";
import {
  signinController,
  signinWithSocialController,
  signoutController,
  signupController,
  TSigninControllerOutput,
  TSigninWithSocialControllerOutput,
  TSignoutControllerOutput,
  TSignupControllerOutput,
} from "@/modules/server/core/auth/interface-adapters/controllers/auth";
import { runWithTransport } from "@/modules/server/presentation/transport/runWithTransport";
import {
  sendEmailVerificationController,
  TSendEmailVerificationControllerOutPut,
} from "@/modules/server/core/auth/interface-adapters/controllers/auth/sendEmailVerification.controller";
import { isEmailVerificationEnabled } from "@/modules/server/auth-provider/auth-server";

/**
 * Server action acting as a transport layer between client and server.
 * Input validation and business logic are delegated to controllers and use cases.
 */

// Server actions act as a transport layer only.
// Input validation is handled in controllers to preserve clean architecture boundaries.
export const signupAction = createServerAction()
  .input(SignupActionSchema, { skipInputParsing: true })
  .handler(async ({ input }) => {
    return await runWithTransport<TSignupControllerOutput>(async () => {
      const data = await signupController(input.payload);

      return {
        result: data,
        transport: {
          ...input.transportOptions,
          shouldRedirect: input.transportOptions?.shouldRedirect ?? true,
          url:
            input.transportOptions?.url ??
            (data.user.emailVerified
              ? "/"
              : isEmailVerificationEnabled
                ? `/auth/email-verification?email=${data.user.email}`
                : "/"),
        },
      };
    });
  });

export const signinAction = createServerAction()
  .input(SigninActionSchema, { skipInputParsing: true })
  .handler(async ({ input }) => {
    return await runWithTransport<TSigninControllerOutput>(async () => {
      const data = await signinController(input.payload);

      return {
        result: data,
        transport: {
          ...input.transportOptions,
          url:
            input.transportOptions?.url ??
            data.url ??
            input.transportOptions?.url,
          shouldRedirect:
            input.transportOptions?.shouldRedirect ??
            data.redirect ??
            input.transportOptions?.shouldRedirect,
        },
      };
    });
  });

export const signinWithSocialAction = createServerAction()
  .input(SigninWithSocialValidationSchema, { skipInputParsing: true })
  .handler(async ({ input }) => {
    return await runWithTransport<TSigninWithSocialControllerOutput>(
      async () => {
        const data = await signinWithSocialController(input);

        return {
          result: data,
          transport: {
            url: data.redirect ? data.url : null,
            shouldRedirect: Boolean(data.redirect && data.url),
          },
        };
      },
    );
  });

export const signoutAction = createServerAction()
  .input(SignoutActionSchema, { skipInputParsing: true })
  .handler(async ({ input }) => {
    return await runWithTransport<TSignoutControllerOutput>(async () => {
      const data = await signoutController();

      return {
        result: data,
        transport: {
          ...input.transportOptions,
          url: data.url ?? input.transportOptions?.url,
          shouldRedirect:
            data.success ?? input.transportOptions?.shouldRedirect,
        },
      };
    });
  });

export const sendEmailVerificationAction = createServerAction()
  .input(SendEmailVerificationActionSchema, { skipInputParsing: true })
  .handler(async ({ input }) => {
    return await runWithTransport<TSendEmailVerificationControllerOutPut>(
      async () => {
        const data = await sendEmailVerificationController(input.payload);

        return {
          result: data,
          transport: input.transportOptions,
        };
      },
    );
  });
