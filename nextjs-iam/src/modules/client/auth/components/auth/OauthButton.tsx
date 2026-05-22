"use client";

import { Button } from "@/components/ui/button";
import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { FcGoogle } from "react-icons/fc";
import { FaGithub } from "react-icons/fa";
import { TSocialProviders } from "@/modules/entities/enums/auth/auth.enum";
import { useServerAction } from "zsa-react";
import { signinWithSocialAction } from "@/modules/server/presentation/actions/auth/auth.actions";
import { handleZSAError } from "@/modules/client/shared/error/handleZSAError";

type Props = {
  oauthName: TSocialProviders;
  label: string;
  isFormSubmitting: boolean;
  className?: string;
};

const OauthButton = ({
  oauthName,
  label,
  isFormSubmitting,
  className,
}: Props) => {
  const { execute, isPending } = useServerAction(signinWithSocialAction, {
    onError({ err }) {
      handleZSAError({
        err,
        fallbackMessage: "Failed to signin",
      });
    },
  });

  return (
    <Button
      variant="outline"
      disabled={isPending || isFormSubmitting}
      className={cn(
        "flex-1 items-center justify-center cursor-pointer border-2",
        className
      )}
      onClick={async () => {
        await execute({ provider: oauthName });
      }}
    >
      <span className="pointer-events-none">
        {!isPending ? (
          oauthName === "google" ? (
            <FcGoogle size={18} aria-hidden="true" />
          ) : (
            <FaGithub size={18} aria-hidden="true" />
          )
        ) : (
          <Loader2 className="animate-spin" />
        )}
      </span>
      {label}
    </Button>
  );
};

export default OauthButton;
