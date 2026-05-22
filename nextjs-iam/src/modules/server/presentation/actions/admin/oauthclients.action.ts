"use server";

import { createServerAction } from "zsa";
import { runWithTransport } from "../../transport/runWithTransport";
import { CreateOAuthClientActionSchema } from "@/modules/entities/schemas/admin/oauthclient/oauthclient.schema";
import {
  createOAuthClientController,
  getOAuthClientsController,
  TCreateOAuthClientControllerOutput,
  TGetOAuthClientsControllerOutput,
} from "@/modules/server/core/admin/interface-adapters/controllers/oauthclient";

export const getOAuthClientsAction = createServerAction().handler(async () => {
  return await runWithTransport<TGetOAuthClientsControllerOutput>(async () => {
    const data = await getOAuthClientsController();

    return {
      result: data,
    };
  });
});

export const createOAuthClientAction = createServerAction()
  .input(CreateOAuthClientActionSchema, { skipInputParsing: true })
  .handler(async ({ input }) => {
    return await runWithTransport<TCreateOAuthClientControllerOutput>(
      async () => {
        const data = await createOAuthClientController(input.payload);

        return {
          result: data,
          transport: input.transportOptions,
        };
      },
    );
  });
