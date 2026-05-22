import { createAuthClient } from "better-auth/react";
import {
  jwtClient,
  organizationClient,
  adminClient,
} from "better-auth/client/plugins";
import { oauthProviderClient } from "@better-auth/oauth-provider/client";

export const authClient = createAuthClient({
  /** The base URL of the server (optional if you're using the same domain) */
  baseURL: process.env.BETTER_AUTH_URL,
  plugins: [
    jwtClient(),
    organizationClient(),
    adminClient(),
    oauthProviderClient(),
  ],
});

export const { useSession } = authClient;
