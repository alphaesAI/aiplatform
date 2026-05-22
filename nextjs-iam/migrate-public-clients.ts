import { PrismaClient } from "./src/modules/server/prisma/generated/prisma";
import { PrismaPg } from '@prisma/adapter-pg';
import { Pool } from 'pg';
import * as dotenv from "dotenv";

dotenv.config();

const connectionString = process.env.DATABASE_URL ?? '';
const pool = new Pool({ connectionString });
const adapter = new PrismaPg(pool);
const prisma = new PrismaClient({ adapter });

async function main() {
  console.log("🔧 Migrating OAuth clients to support public clients with PKCE...\n");

  // Update phia-mobile-app to be a public client
  const phiaClient = await prisma.oauthClient.findFirst({
    where: { 
      OR: [
        { clientId: "phia-mobile-app" },
        { clientId: "phia-android-app" }
      ]
    },
  });

  if (phiaClient) {
    console.log(`✅ Found existing client: ${phiaClient.clientId}`);
    
    await prisma.oauthClient.update({
      where: { id: phiaClient.id },
      data: {
        type: "public",
        public: true,
        requirePKCE: true,
        clientSecret: null, // Remove secret for public clients
        grantTypes: ["authorization_code", "refresh_token"],
        responseTypes: ["code"],
        tokenEndpointAuthMethod: "none", // No authentication required
      },
    });
    
    console.log(`✅ Updated ${phiaClient.clientId} to public client with PKCE\n`);
  } else {
    console.log("⚠️  No existing PHIA client found. Will be created by registration script.\n");
  }

  console.log("✅ Migration completed successfully!");
}

main()
  .catch((e) => {
    console.error("❌ Migration failed:", e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
