import { PrismaClient } from "@prisma/client";
import * as dotenv from "dotenv";

dotenv.config();

const prisma = new PrismaClient({
  datasources: {
    db: {
      url: process.env.DATABASE_URL,
    },
  },
});

async function main() {
  const clientId = "phia-android-app";
  const clientSecret = "phia_secret_2026"; 

  console.log(`Checking for OAuth client: ${clientId}...`);

  const existingClient = await prisma.oauthClient.findFirst({
    where: { clientId },
  });

  if (existingClient) {
    console.log("Client already exists. Updating...");
    await prisma.oauthClient.update({
      where: { id: existingClient.id },
      data: {
        clientSecret,
        redirectUris: "phia://auth/callback",
        type: "public",
        requirePKCE: false,
      },
    });
  } else {
    console.log("Creating new OAuth client...");
    await prisma.oauthClient.create({
      data: {
        name: "PHIA Android App",
        clientId,
        clientSecret,
        redirectUris: "phia://auth/callback",
        type: "public",
        requirePKCE: false,
        metadata: "{}",
      },
    });
  }

  console.log("✅ OAuth Client registered successfully!");
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
