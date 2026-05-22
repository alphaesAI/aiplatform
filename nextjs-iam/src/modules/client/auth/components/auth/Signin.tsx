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
  SigninFormSchema,
  TSigninFormSchema,
} from "@/modules/entities/schemas/auth";
import {
  Field,
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field";
import { Form } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { useState, useEffect } from "react";
import {
  InputGroup,
  InputGroupAddon,
  InputGroupInput,
} from "@/components/ui/input-group";
import { Eye, EyeOff, Key, Loader2, Mail } from "lucide-react";
import { Button, buttonVariants } from "@/components/ui/button";
import { useServerAction } from "zsa-react";
import { signinAction } from "@/modules/server/presentation/actions/auth";
import { toast } from "sonner";
import { handleZSAError } from "@/modules/client/shared/error/handleZSAError";
import Link from "next/link";
import OauthButton from "./OauthButton";
import AuthSeparator from "./AuthSeparator";
import { Checkbox } from "@/components/ui/checkbox";
import { cn } from "@/lib/utils";

function Signin() {
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [queryString, setQueryString] = useState("");

  useEffect(() => {
    setQueryString(window.location.search);
  }, []);

  const form = useForm<TSigninFormSchema>({
    resolver: zodResolver(SigninFormSchema),
    defaultValues: {
      email: "",
      password: "",
      rememberMe: true,
    },
  });
  const { isSubmitting } = form.formState;

  const { execute } = useServerAction(signinAction, {
    onSuccess: () => {
      toast.success("Signin successful");
    },
    onError: ({ err }) => {
      handleZSAError<TSigninFormSchema>({
        err,
        form,
        fallbackMessage: "Signin failed",
      });
    },
  });

  function handlePasswordVisibility() {
    setIsPasswordVisible((prev) => !prev);
  }

  async function handleSignin(values: TSigninFormSchema) {
    // 🕵️ SENIOR FIX: Strip 'prompt=login' so the redirect actually completes
    const urlParams = new URLSearchParams(window.location.search);
    urlParams.delete("prompt");
    const cleanParams = urlParams.toString();

    await execute({
      payload: values,
      transportOptions: {
        shouldRedirect: cleanParams ? true : undefined,
        url: cleanParams ? `/api/auth/oauth2/authorize?${cleanParams}` : undefined,
      },
    });
  }

  return (
    <Card className="max-w-sm w-full mx-auto">
      <CardHeader>
        <CardTitle>Sign In</CardTitle>
        <CardDescription>
          Enter your email below to login to your account
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleSignin)}>
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

              {/* password */}
              <Controller
                control={form.control}
                name="password"
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid}>
                    <div className="flex items-center justify-between gap-2 flex-wrap">
                      <FieldLabel htmlFor="password">Password</FieldLabel>
                      <FieldLabel asChild>
                        <Link
                          href="/auth/forget-password"
                          className="hover:underline"
                        >
                          Forgot your password?
                        </Link>
                      </FieldLabel>
                    </div>
                    <InputGroup>
                      <InputGroupInput
                        {...field}
                        id="password"
                        aria-invalid={fieldState.invalid}
                        placeholder="Password"
                        autoComplete="off"
                        type={isPasswordVisible ? "text" : "password"}
                      />
                      <InputGroupAddon
                        align="inline-end"
                        className="cursor-pointer"
                        onClick={handlePasswordVisibility}
                      >
                        {isPasswordVisible ? <Eye /> : <EyeOff />}
                      </InputGroupAddon>
                    </InputGroup>
                    {fieldState.invalid && (
                      <FieldError errors={[fieldState.error]} />
                    )}
                  </Field>
                )}
              />

              <div className="grid gap-4">
                <div className="space-y-2.5">
                  <Controller
                    control={form.control}
                    name="rememberMe"
                    render={({ field, fieldState }) => (
                      <Field>
                        <div className="flex items-center gap-2">
                          <Checkbox
                            id="rememberme"
                            defaultChecked={field.value}
                            onCheckedChange={(checked) => {
                              field.onChange(checked);
                            }}
                          />
                          <FieldLabel
                            htmlFor="rememberme"
                            className="font-normal text-sm"
                          >
                            Remember me
                          </FieldLabel>
                        </div>
                        {fieldState.invalid && (
                          <FieldError errors={[fieldState.error]} />
                        )}
                      </Field>
                    )}
                  />
                  <Button
                    type="submit"
                    disabled={isSubmitting}
                    className="w-full"
                  >
                    {isSubmitting ? (
                      <>
                        <Loader2 className="animate-spin" />
                        Sign In
                      </>
                    ) : (
                      "Sign In"
                    )}
                  </Button>
                </div>
                <Link
                  href="/auth/magic-link"
                  className={cn(
                    buttonVariants({ variant: "secondary" }),
                    "w-full",
                  )}
                >
                  <Mail /> Sign in with Magic Link
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
          href={`/auth/sign-up${queryString}`}
          className="text-foreground underline underline-offset-2"
        >
          Sign Up
        </Link>
      </CardFooter>
    </Card>
  );
}

export default Signin;
