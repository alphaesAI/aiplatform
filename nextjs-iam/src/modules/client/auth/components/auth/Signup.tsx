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
  SignupFormSchema,
  TSignupFormSchema,
} from "@/modules/entities/schemas/auth";
import {
  Field,
  FieldDescription,
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
import { Eye, EyeOff, Loader2, Mail } from "lucide-react";
import { Button, buttonVariants } from "@/components/ui/button";
import { useServerAction } from "zsa-react";
import { signupAction } from "@/modules/server/presentation/actions/auth";
import { toast } from "sonner";
import { handleZSAError } from "@/modules/client/shared/error/handleZSAError";
import OauthButton from "./OauthButton";
import Link from "next/link";
import AuthSeparator from "./AuthSeparator";
import { Checkbox } from "@/components/ui/checkbox";
import { cn } from "@/lib/utils";

function Signup() {
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [queryString, setQueryString] = useState("");

  useEffect(() => {
    setQueryString(window.location.search);
  }, []);

  const form = useForm<TSignupFormSchema>({
    resolver: zodResolver(SignupFormSchema),
    defaultValues: {
      name: "",
      email: "",
      password: "",
      rememberMe: true,
    },
  });
  const { isSubmitting } = form.formState;

  const { execute } = useServerAction(signupAction, {
    onSuccess: ({ data }) => {
      console.log(data);
      toast.success("Signup successful");
    },
    onError: ({ err }) => {
      handleZSAError<TSignupFormSchema>({
        err,
        form,
        fallbackMessage: "Signup failed",
      });
    },
  });

  function handlePasswordVisibility() {
    setIsPasswordVisible((prev) => !prev);
  }

  async function handleSignup(values: TSignupFormSchema) {
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
        <CardTitle>Sign Up</CardTitle>
        <CardDescription>
          Enter your information to create an account
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleSignup)}>
            <FieldGroup>
              {/* name */}
              <Controller
                control={form.control}
                name="name"
                render={({ field, fieldState }) => (
                  <Field data-invalid={fieldState.invalid}>
                    <FieldLabel htmlFor="name">Name</FieldLabel>
                    <Input
                      {...field}
                      id="name"
                      aria-invalid={fieldState.invalid}
                      placeholder="Name"
                    />
                    {fieldState.invalid && (
                      <FieldError errors={[fieldState.error]} />
                    )}
                  </Field>
                )}
              />

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
                    <FieldLabel htmlFor="password">Password</FieldLabel>
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
                    <FieldDescription>
                      Password must be at least 8 characters long
                    </FieldDescription>
                    {fieldState.invalid && (
                      <FieldError errors={[fieldState.error]} />
                    )}
                  </Field>
                )}
              />

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
                <div className="grid gap-4">
                  <Button type="submit" disabled={isSubmitting}>
                    {isSubmitting ? (
                      <>
                        <Loader2 className="animate-spin" />
                        Create an account
                      </>
                    ) : (
                      "Create an account"
                    )}
                  </Button>
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
              </div>

              <AuthSeparator />

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
            </FieldGroup>
          </form>
        </Form>
      </CardContent>
      <CardFooter className="flex items-center gap-1.5 w-fit mx-auto text-sm text-muted-foreground">
        {"Already have an account?"}
        <Link
          href={`/auth/sign-in${queryString}`}
          className="text-foreground underline underline-offset-2"
        >
          Sign In
        </Link>
      </CardFooter>
    </Card>
  );
}

export default Signup;
