import { PrismaClient } from "@prisma/client";

// STEP 1: Set the DB URL manually before creating the client
process.env.DATABASE_URL = "postgresql://postgres:123@localhost:5432/iam-backend";

// STEP 2: Create the client with NO options (it will use the ENV above)
const prisma = new PrismaClient();

async function main() {
  console.log("\n--- 🛡️ Accessing Database... ---");
  
  // Using 'any' to stop TypeScript from arguing about model names
  const model = (prisma as any).oauthClient; 

  if (!model) {
    console.log("❌ Error: Could not find 'oauthClient' model in your Prisma schema.");
    return;
  }

  const clients = await model.findMany();
  
  console.log("--- 📊 Current OAuth Clients in DB ---");
  
  if (clients.length === 0) {
    console.log("⚠️ No clients found! The database is clean.");
    console.log("👉 ACTION: Restart your 'pnpm dev' server now to recreate it.");
  } else {
    clients.forEach((c: any) => {
      console.log(`✅ ID: ${c.clientId} | Type: ${c.type} | PKCE: ${c.requirePKCE}`);
    });
  }
  
  console.log("--------------------------------------\n");
}

main()
  .catch((e) => console.error("❌ Script Error:", e))
  .finally(async () => await prisma.$disconnect());