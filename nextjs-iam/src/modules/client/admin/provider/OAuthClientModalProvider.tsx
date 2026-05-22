"use client";

import { useSyncExternalStore } from "react";
import { CreateOAuthClientModal } from "../modals/oauth-clients/CreateOAuthClientModal";

export const OAuthClientModalProvider = () => {
  const isMounted = useSyncExternalStore(
    () => () => {},
    () => true,
    () => false
  );

  if (!isMounted) return null;

  return (
    <>
      <CreateOAuthClientModal />
    </>
  );
};
