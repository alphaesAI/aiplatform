import { PrismaClient } from "./src/modules/server/prisma/generated/prisma";
import { PrismaPg } from '@prisma/adapter-pg';
import { Pool } from 'pg';
import * as dotenv from "dotenv";

dotenv.config();

const connectionString = process.env.DATABASE_URL ?? '';
const pool = new Pool({ connectionString });
const adapter = new PrismaPg(pool);
const prisma = new PrismaClient({ adapter });

async function testConfig() {
  console.log("🔍 Checking OAuth client configuration...\n");
  
  const client = await prisma.oauthClient.findFirst({
    where: { clientId: "phia-mobile-app" }
  });
  
  if (!client) {
    console.log("❌ Client not found!");
    return;
  }
  
  console.log("✅ Client found!");
  console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
  console.log(`Client ID:        ${client.clientId}`);
  console.log(`Name:             ${client.name}`);
  console.log(`Type:             ${client.type}`);
  console.log(`Public:           ${client.public}`);
  console.log(`Require PKCE:     ${client.requirePKCE}`);
  console.log(`Client Secret:    ${client.clientSecret ? "SET (should be null)" : "NULL ✅"}`);
  console.log(`Redirect URIs:    ${client.redirectUris.join(", ")}`);
  console.log(`Grant Types:      ${client.grantTypes.join(", ")}`);
  console.log(`Response Types:   ${client.responseTypes.join(", ")}`);
  console.log(`Token Auth:       ${client.tokenEndpointAuthMethod}`);
  console.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
  
  const isCorrect = 
    client.type === "public" &&
    client.public === true &&
    client.requirePKCE === true &&
    client.clientSecret === null &&
    client.tokenEndpointAuthMethod === "none";
  
  if (isCorrect) {
    console.log("✅ Configuration is CORRECT!");
    console.log("✅ Mobile app should now work with PKCE\n");
  } else {
    console.log("⚠️  Configuration needs adjustment");
    console.log("Run: pnpm oauth:register-mobile\n");
  }
}

testConfig()
  .catch(console.error)
  .finally(() => prisma.$disconnect());
