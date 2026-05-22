import {
  TCreateOAuthClientResponseDtoSchema,
  TGetOAuthClientsResponseDtoSchema,
} from "@/modules/entities/schemas/admin/oauthclient/oauthclient.schema";
import {
  TCreateOAuthClientPayload,
  TUpdateOAuthClientPayload,
  TDeleteOAuthClientPayload,
  TGetOAuthClientPayload,
} from "@/modules/entities/types/admin/oauthclient.type";

export interface IOAuthClientService {
  createOAuthClient(
    payload: TCreateOAuthClientPayload,
  ): Promise<TCreateOAuthClientResponseDtoSchema>;
  updateOAuthClient(payload: TUpdateOAuthClientPayload): Promise<void>;
  deleteOAuthClient(payload: TDeleteOAuthClientPayload): Promise<void>;
  getOAuthClient(payload: TGetOAuthClientPayload): Promise<void>;
  getOAuthClients(): Promise<TGetOAuthClientsResponseDtoSchema>;
}
