import { headers } from "next/headers";
import { IOAuthConsentService } from "../../domain/interfaces/oauthconsent.service.interface";
import { auth } from "@/modules/server/auth-provider/auth";

export class OAuthConsentService implements IOAuthConsentService {
  async getOAuthConsents(): Promise<void> {
    try {
      // type AdminCreateOAuthClientParams = Parameters<
      //   typeof auth.api.getOAuthConsents
      // >[0];

      const data = await auth.api.getOAuthConsents({
        // This endpoint requires session cookies.
        headers: await headers(),
      });

      console.log(data);
    } catch (error) {
      throw error;
    }
  }
}
