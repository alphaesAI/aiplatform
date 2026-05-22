"use client";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { toast } from "sonner";
import { useAdminStore } from "../../stores/admin.store";
import { useServerAction } from "zsa-react";
import { useForm, FormProvider } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  CreateOAuthClientFormSchema,
  TCreateOAuthClientFormSchema,
} from "@/modules/entities/schemas/admin/oauthclient/oauthclient.schema";
import { OAuthClientCreateForm } from "../../forms/OAuthClientCreateForm";
import { createOAuthClientAction } from "@/modules/server/presentation/actions/admin";
import { handleZSAError } from "@/modules/client/shared/error/handleZSAError";

export const CreateOAuthClientModal = () => {
  //   const session = useSession();
  const closeModal = useAdminStore((state) => state.onClose);
  const modalType = useAdminStore((state) => state.type);
  const isOpen = useAdminStore((state) => state.isOpen);

  const isModalOpen = isOpen && modalType === "createOAuthClient";

  const form = useForm<TCreateOAuthClientFormSchema>({
    resolver: zodResolver(CreateOAuthClientFormSchema),
    defaultValues: {
      redirect_uris: [],
      post_logout_redirect_uris: undefined,
    },
    shouldUnregister: false,
  });

  const { execute } = useServerAction(createOAuthClientAction, {
    onSuccess({ data }) {
      console.log(data);
      toast.success("OAuth Client Created.");
      handleCloseModal();
    },
    onError({ err }) {
      handleZSAError<TCreateOAuthClientFormSchema>({
        err,
        form,
        fallbackMessage: "Failed to create OAuth Client",
      });
    },
  });

  async function handleCreateOAuthClient(values: TCreateOAuthClientFormSchema) {
    // if (!session || !session.data?.user || !session.data.user?.currentOrgId) {
    //   return;
    // }

    const cleanedPostLogout = values.post_logout_redirect_uris
      ?.map((v) => v.trim())
      .filter(Boolean);

    const cleaned = {
      ...values,
      post_logout_redirect_uris:
        cleanedPostLogout && cleanedPostLogout.length > 0
          ? cleanedPostLogout
          : undefined,
    };

    await execute({
      payload: cleaned,
      transportOptions: { shouldRevalidate: true, url: "/admin/oauth-clients" },
    });
  }

  function handleCloseModal() {
    form.reset();
    closeModal();
  }

  return (
    <Dialog open={isModalOpen} onOpenChange={handleCloseModal}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Add OAuth Client</DialogTitle>
          <DialogDescription>
            Fill in the App settings details to add a new one to your
            collection.
          </DialogDescription>
        </DialogHeader>
        <FormProvider {...form}>
          <OAuthClientCreateForm
            onCancel={handleCloseModal}
            onSubmit={handleCreateOAuthClient}
          />
        </FormProvider>
      </DialogContent>
    </Dialog>
  );
};
