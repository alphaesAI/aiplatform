import { PrismaClient } from "@prisma/client";
import * as bcrypt from "bcryptjs";

// STEP 1: Set the DB URL manually
process.env.DATABASE_URL = "postgresql://postgres:123@localhost:5432/iam-backend";

const prisma = new PrismaClient();

async function main() {
    const email = "surya@test.com";
    const password = "password123";
    const hashedPassword = await bcrypt.hash(password, 10);

    console.log("\n🚀 Starting User Creation...");

    // Using 'any' to stop TypeScript from complaining about model names
    const userModel = (prisma as any).user;
    const accountModel = (prisma as any).account;

    // 1. Create or Find the User
    const user = await userModel.upsert({
        where: { email: email },
        update: {},
        create: {
            name: "Surya",
            email: email,
            emailVerified: true,
            createdAt: new Date(),
            updatedAt: new Date(),
        }
    });

    console.log(`👤 User '${user.name}' is ready.`);

    // 2. Create the Login Credentials (the 'account' table)
    // We use upsert here too in case you run the script twice
    await accountModel.upsert({
        where: { 
            providerId_accountId: {
                providerId: "email",
                accountId: email
            }
        },
        update: {
            password: hashedPassword
        },
        create: {
            userId: user.id,
            accountId: email,
            providerId: "email",
            password: hashedPassword,
            createdAt: new Date(),
            updatedAt: new Date(),
        }
    });

    console.log("🔐 Password linked successfully.");
    console.log(`\n✅ EVERYTHING READY!`);
    console.log(`Email: ${email}`);
    console.log(`Password: ${password}\n`);
}

main()
    .catch((e) => console.error("❌ Script Error:", e))
    .finally(async () => await prisma.$disconnect());