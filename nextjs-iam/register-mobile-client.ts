import { PrismaClient } from "./src/modules/server/prisma/generated/prisma";
import { PrismaPg } from '@prisma/adapter-pg';
import { Pool } from 'pg';
import * as dotenv from "dotenv";
import { randomUUID } from "crypto";

dotenv.config();

const connectionString = process.env.DATABASE_URL ?? '';
const pool = new Pool({ connectionString });
const adapter = new PrismaPg(pool);
const prisma = new PrismaClient({ adapter });

/**
 * Register a mobile app as a PUBLIC OAuth client with PKCE
 * 
 * Mobile apps CANNOT securely store secrets, so they use PKCE instead.
 * This follows OAuth 2.0 RFC 8252 (OAuth for Native Apps)
 */
async function registerMobileClient() {
  const clientId = "phia-mobile-app";
  const clientName = "PHIA Health Tracker (Android)";
  const redirectUri = "phia://auth/callback";

  console.log(`📱 Registering mobile OAuth client: ${clientId}...\n`);

  const existingClient = await prisma.oauthClient.findFirst({
    where: { clientId },
  });

  if (existingClient) {
    console.log("⚠️  Client already exists. Updating to ensure correct configuration...\n");
    
    await prisma.oauthClient.update({
      where: { id: existingClient.id },
      data: {
        name: clientName,
        type: "public",
        public: true,
        requirePKCE: true,
        clientSecret: null, // ✅ No secret for mobile apps
        redirectUris: [redirectUri],
        grantTypes: ["authorization_code", "refresh_token"],
        responseTypes: ["code"],
        tokenEndpointAuthMethod: "none", // ✅ PKCE replaces client authentication
        scopes: ["openid", "profile", "email", "offline_access"],
        metadata: {
          platform: "android",
          pkce_required: true,
          description: "Mobile app using PKCE for secure authentication"
        },
      },
    });
    
    console.log("✅ Client updated successfully!\n");
  } else {
    console.log("Creating new mobile client...\n");
    
    await prisma.oauthClient.create({
      data: {
        id: randomUUID(),
        name: clientName,
        clientId,
        type: "public",
        public: true,
        requirePKCE: true,
        clientSecret: null, // ✅ No secret for mobile apps
        redirectUris: [redirectUri],
        grantTypes: ["authorization_code", "refresh_token"],
        responseTypes: ["code"],
        tokenEndpointAuthMethod: "none", // ✅ PKCE replaces client authentication
        scopes: ["openid", "profile", "email", "offline_access"],
        metadata: {
          platform: "android",
          pkce_required: true,
          description: "Mobile app using PKCE for secure authentication"
        },
      },
    });
    
    console.log("✅ Client created successfully!\n");
  }

  console.log("📋 Client Configuration:");
  console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
  console.log(`Client ID:       ${clientId}`);
  console.log(`Client Type:     public (mobile app)`);
  console.log(`PKCE Required:   YES ✅`);
  console.log(`Client Secret:   NONE (uses PKCE instead)`);
  console.log(`Redirect URI:    ${redirectUri}`);
  console.log(`Grant Types:     authorization_code, refresh_token`);
  console.log(`Response Types:  code`);
  console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
  
  console.log("✅ Mobile client registered successfully!");
  console.log("🔒 Security: PKCE ensures secure authentication without storing secrets in the app.\n");
}

registerMobileClient()
  .catch((e) => {
    console.error("❌ Registration failed:", e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
