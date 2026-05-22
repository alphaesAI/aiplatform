import { IOAuthClientService } from "@/modules/server/core/admin/domain/interfaces/oauthclient.service.interface";
import {
  TCreateOAuthClientPayload,
  TUpdateOAuthClientPayload,
  TDeleteOAuthClientPayload,
  TGetOAuthClientPayload,
} from "@/modules/entities/types/admin/oauthclient.type";
import {
  TGetOAuthClientsResponseDtoSchema,
  GetOAuthClientsResponseDtoSchema,
  TCreateOAuthClientResponseDtoSchema,
  CreateOAuthClientResponseDtoSchema,
} from "@/modules/entities/schemas/admin/oauthclient/oauthclient.schema";
import { auth } from "@/modules/server/auth-provider/auth";
import { headers } from "next/headers";

export class OAuthClientService implements IOAuthClientService {
  async createOAuthClient(
    payload: TCreateOAuthClientPayload,
  ): Promise<TCreateOAuthClientResponseDtoSchema> {
    try {
      // type AdminCreateOAuthClientParams = Parameters<
      //   typeof auth.api.adminCreateOAuthClient
      // >[0];

      const res = await auth.api.adminCreateOAuthClient({
        headers: await headers(),
        body: {
          ...payload,
        },
      });

      console.log(res);

      return await CreateOAuthClientResponseDtoSchema.parseAsync(res);
    } catch (error) {
      throw error;
    }
  }

  async updateOAuthClient(payload: TUpdateOAuthClientPayload): Promise<void> {
    //   try {
    //     type AdminCreateOAuthUpdateParams =
    // Parameters<typeof auth.api.updateOAuthClient>[0];
    //     const res = await auth.api.updateOAuthClient({
    //       headers: await headers(),
    //       body: {
    //         client_id: payload.client_id,
    //         update: {
    //         }
    //       }
    //     })
    //   } catch (error) {
    //     throw error;
    //   }
    throw new Error("Method not implemented.");
  }

  async deleteOAuthClient(payload: TDeleteOAuthClientPayload): Promise<void> {
    throw new Error("Method not implemented.");
  }

  async getOAuthClient(payload: TGetOAuthClientPayload): Promise<void> {
    throw new Error("Method not implemented.");
  }

  async getOAuthClients(): Promise<TGetOAuthClientsResponseDtoSchema> {
    try {
      const res = await auth.api.getOAuthClients({
        headers: await headers(),
      });
      const data = await GetOAuthClientsResponseDtoSchema.parseAsync(res);
      return data;
    } catch (error) {
      throw error;
    }
  }
}

/* 
Create:
{
  client_id: 'TpWGUgbLVUwveZOGRmZFxRvlKRkOkVal',
  client_secret: 'hhtAgyzOkcdhVStKWheqMOFNQvPMCoVM',
  client_secret_expires_at: 0,
  scope: '',
  user_id: 'OOmJmoyqIS06rdTYKC9kCJGVFf4DMQgK',
  client_id_issued_at: 1773925254,
  client_name: 'Test Client',
  client_uri: undefined,
  logo_uri: undefined,
  contacts: [],
  tos_uri: undefined,
  policy_uri: undefined,
  software_id: undefined,
  software_version: undefined,
  software_statement: undefined,
  redirect_uris: [ 'http://localhost:5000/admin/oauth-clients' ],
  post_logout_redirect_uris: [],
  token_endpoint_auth_method: 'client_secret_basic',
  grant_types: [ 'authorization_code' ],
  response_types: [ 'code' ],
  public: false,
  type: undefined,
  disabled: false,
  skip_consent: undefined,
  enable_end_session: undefined,
  require_pkce: undefined,
  subject_type: undefined,
  reference_id: undefined
}
*/
