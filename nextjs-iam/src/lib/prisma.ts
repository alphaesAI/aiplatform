import { Pool } from 'pg';
import { PrismaPg } from '@prisma/adapter-pg';
import { PrismaClient } from '@prisma/client';

const globalForPrisma = globalThis as unknown as {
  prisma: PrismaClient | undefined;
};

const connectionString = process.env.DATABASE_URL;

// 1. Create the Postgres Pool
const pool = new Pool({ connectionString });

// 2. Create the Adapter
const adapter = new PrismaPg(pool);

// 3. Initialize Prisma with the Adapter
export const prisma =
  globalForPrisma.prisma ??
  new PrismaClient({
    adapter, // This tells Prisma how to connect
    log: ['query', 'error', 'warn'],
  });

if (process.env.NODE_ENV !== "production") globalForPrisma.prisma = prisma;