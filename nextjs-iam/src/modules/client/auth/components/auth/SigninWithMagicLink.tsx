"use client";

import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  ForgetPasswordOrMagicLinkFormSchema,
  TForgetPasswordOrMagicLinkFormSchema,
} from "@/modules/entities/schemas/auth";
import {
  Field,
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field";
import { Form } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Key, Loader2, Lock } from "lucide-react";
import { Button, buttonVariants } from "@/components/ui/button";
import { useServerAction } from "zsa-react";
import { signinAction } from "@/modules/server/presentation/actions/auth";
import { toast } from "sonner";
import { handleZSAError } from "@/modules/client/shared/error/handleZSAError";
import OauthButton from "./OauthButton";
import AuthSeparator from "./AuthSeparator";
import Link from "next/link";
import { cn } from "@/lib/utils";

function SigninWithMagicLink() {
  const form = useForm<TForgetPasswordOrMagicLinkFormSchema>({
    resolver: zodResolver(ForgetPasswordOrMagicLinkFormSchema),
    defaultValues: {
      email: "",
    },
  });
  const { isSubmitting } = form.formState;

  const { execute } = useServerAction(signinAction, {
    onSuccess: () => {
      toast.success("Password reset link sent to the given email.");
    },
    onError: ({ err }) => {
      handleZSAError<TForgetPasswordOrMagicLinkFormSchema>({
        err,
        form,
        fallbackMessage: "Failed to send reset link",
      });
    },
  });

  async function handleForgetPassword(
    values: TForgetPasswordOrMagicLinkFormSchema
  ) {}

  return (
    <Card className="max-w-sm w-full mx-auto">
      <CardHeader>
        <CardTitle>Magic Link</CardTitle>
        <CardDescription>
          Enter your email to receive a magic link
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleForgetPassword)}>
            <FieldGroup>
              {/* email */}
              <Controller
                control={form.control}
                name="email"
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid}>
                    <FieldLabel htmlFor="email">Email</FieldLabel>
                    <Input
                      {...field}
                      id="email"
                      aria-invalid={fieldState.invalid}
                      placeholder="me@example.com"
                    />
                    {fieldState.invalid && (
                      <FieldError errors={[fieldState.error]} />
                    )}
                  </Field>
                )}
              />

              <div className="grid gap-4">
                <Button
                  type="submit"
                  disabled={isSubmitting}
                  className="w-full"
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="animate-spin" />
                      Sending...
                    </>
                  ) : (
                    "Send magic link"
                  )}
                </Button>
                <Link
                  href="/auth/sign-in"
                  className={cn(buttonVariants({ variant: "secondary" }))}
                >
                  <Lock /> Sign in with Password
                </Link>
              </div>

              <AuthSeparator />

              <div className="grid gap-4">
                <div className="flex items-center justify-between gap-4">
                  <OauthButton
                    oauthName="google"
                    label="Google"
                    isFormSubmitting={isSubmitting}
                  />
                  <OauthButton
                    oauthName="github"
                    label="GitHub"
                    isFormSubmitting={isSubmitting}
                  />
                </div>
                <Button variant="secondary">
                  <Key /> Sign in with Passkey
                </Button>
              </div>
            </FieldGroup>
          </form>
        </Form>
      </CardContent>
      <CardFooter className="flex items-center gap-1.5 w-fit mx-auto text-sm text-muted-foreground">
        {"Don't have an account?"}
        <Link
          href="/auth/sign-up"
          className="text-foreground underline underline-offset-2"
        >
          Sign Up
        </Link>
      </CardFooter>
    </Card>
  );
}

export default SigninWithMagicLink;
