"use client";

import { useFormContext, useFieldArray } from "react-hook-form";

import { Button } from "@/components/ui/button";
import { SelectItem } from "@/components/ui/select";

import { Loader2, Plus, Trash } from "lucide-react";

import {
  Field,
  FieldDescription,
  FieldError,
  FieldGroup,
  FieldLabel,
  FieldLegend,
  FieldSet,
} from "@/components/ui/field";

import { DialogClose, DialogFooter } from "@/components/ui/dialog";
import { TCreateOAuthClientFormSchema } from "@/modules/entities/schemas/admin/oauthclient/oauthclient.schema";
import {
  FormInput,
  FormSelect,
  FormSwitch,
} from "../../shared/custom-form-fields";
import { useEffect } from "react";

interface OAuthClientCreateFormProps {
  onSubmit: (data: TCreateOAuthClientFormSchema) => Promise<void>;
  onCancel: () => void;
}

export function OAuthClientCreateForm({
  onSubmit,
  onCancel,
}: OAuthClientCreateFormProps) {
  const form = useFormContext<TCreateOAuthClientFormSchema>();

  const {
    formState: { isSubmitting, errors },
    control,
  } = form;

  const handleSubmit = async (data: TCreateOAuthClientFormSchema) => {
    await onSubmit(data);
  };

  // 🔹 Field arrays
  const redirectUris = useFieldArray({
    control: control as never,
    name: "redirect_uris",
  });

  const postLogoutRedirectUris = useFieldArray({
    control: control as never,
    name: "post_logout_redirect_uris",
  });

  useEffect(() => {
    if (redirectUris.fields.length === 0) {
      redirectUris.append("");
    }
  }, [redirectUris.fields, redirectUris]);

  return (
    <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-6">
      {/* Basic Info */}
      <FieldGroup>
        <FieldSet>
          <FieldLegend>Basic Information</FieldLegend>
          <FieldGroup className="grid sm:grid-cols-2 gap-4">
            <FormInput
              control={control}
              name="client_name"
              label="Client Name"
              placeholder="My App"
              description="Human-readable name of your application."
              descriptionPlace="bottom"
            />

            <FormInput
              control={control}
              name="scope"
              label="Scope"
              placeholder="openid profile email"
            />

            <FormInput
              control={control}
              name="client_uri"
              label="Client URL"
              placeholder="https://example.com"
            />

            <FormInput
              control={control}
              name="logo_uri"
              label="Logo URL"
              placeholder="https://example.com/logo.png"
            />
          </FieldGroup>
        </FieldSet>
      </FieldGroup>

      {/* Redirects */}
      <FieldGroup>
        <FieldSet>
          <FieldLegend>Redirect Configuration</FieldLegend>

          {/* Redirect URIs */}
          <Field>
            <FieldLabel>Redirect URIs</FieldLabel>
            <FieldDescription>
              Allowed callback URLs after login (required).
            </FieldDescription>

            <div className="space-y-2">
              {redirectUris.fields.map((item, index) => (
                <div key={item.id} className="flex gap-2 items-center">
                  <FormInput
                    control={control}
                    name={`redirect_uris.${index}`}
                    placeholder="https://app.com/callback"
                  />

                  <Button
                    type="button"
                    variant="destructive"
                    size="icon"
                    onClick={() => redirectUris.remove(index)}
                    className="self-end"
                  >
                    <Trash size={16} />
                  </Button>
                </div>
              ))}

              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => redirectUris.append("")}
                className="flex items-center gap-2"
              >
                <Plus size={16} />
                Add Redirect URI
              </Button>
            </div>

            <FieldError errors={[errors.redirect_uris]} />
          </Field>

          {/* Post Logout Redirect URIs */}
          <Field>
            <FieldLabel>Post Logout Redirect URIs</FieldLabel>
            <FieldDescription>
              Where users are redirected after logout.
            </FieldDescription>

            <div className="space-y-2">
              {postLogoutRedirectUris.fields.map((item, index) => (
                <div key={item.id} className="flex gap-2 items-center">
                  <FormInput
                    control={control}
                    name={`post_logout_redirect_uris.${index}`}
                    placeholder="https://app.com/logout"
                  />

                  <Button
                    type="button"
                    variant="destructive"
                    size="icon"
                    onClick={() => postLogoutRedirectUris.remove(index)}
                    className="self-end"
                  >
                    <Trash size={16} />
                  </Button>
                </div>
              ))}

              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => postLogoutRedirectUris.append("")}
                className="flex items-center gap-2"
              >
                <Plus size={16} />
                Add Logout URI
              </Button>
            </div>
          </Field>
        </FieldSet>
      </FieldGroup>

      {/* OAuth Config */}
      <FieldGroup>
        <FieldSet>
          <FieldLegend>OAuth Configuration</FieldLegend>
          <FieldGroup className="grid sm:grid-cols-2 gap-4">
            {/* Auth Method */}

            <FormSelect
              control={form.control}
              name="token_endpoint_auth_method"
              label="Auth Method"
              placeholder="Select method"
              className="w-full"
            >
              <SelectItem value="none">None</SelectItem>
              <SelectItem value="client_secret_basic">
                client_secret_basic
              </SelectItem>
              <SelectItem value="client_secret_post">
                client_secret_post
              </SelectItem>
            </FormSelect>

            {/* Client Type */}
            <FormSelect
              control={form.control}
              name="type"
              label="Client Type"
              placeholder="Select type"
              className="w-full"
            >
              <SelectItem value="web">Web</SelectItem>
              <SelectItem value="native">Native</SelectItem>
              <SelectItem value="user-agent-based">SPA</SelectItem>
            </FormSelect>

            {/* Subject Type */}
            <FormSelect
              control={form.control}
              name="subject_type"
              label="Subject Type"
              placeholder="Select subject type"
              className="w-full"
            >
              <SelectItem value="public">Public</SelectItem>
              <SelectItem value="pairwise">Pairwise</SelectItem>
            </FormSelect>
          </FieldGroup>
        </FieldSet>
      </FieldGroup>

      {/* Security */}
      <FieldGroup>
        <FieldSet>
          <FieldLegend>Security Settings</FieldLegend>
          <FieldGroup className="grid sm:grid-cols-2 gap-4">
            <FormSwitch
              control={form.control}
              name="require_pkce"
              label="Require PKCE"
            />

            <FormSwitch
              control={form.control}
              name="skip_consent"
              label="Skip Consent"
            />

            <FormSwitch
              control={form.control}
              name="enable_end_session"
              label="Enable Logout"
            />
          </FieldGroup>
        </FieldSet>
      </FieldGroup>

      {/* Actions */}
      <DialogFooter>
        <DialogClose asChild>
          <Button type="button" size="sm" variant="outline" onClick={onCancel}>
            Cancel
          </Button>
        </DialogClose>

        <Button disabled={isSubmitting} size="sm" type="submit">
          {isSubmitting ? (
            <>
              <Loader2 className="animate-spin" /> Creating Client
            </>
          ) : (
            "Create Client"
          )}
        </Button>
      </DialogFooter>
    </form>
  );
}
