import { betterAuth } from "better-auth";
import { prismaAdapter } from "@better-auth/prisma-adapter";
import { oauthProvider } from "@better-auth/oauth-provider";
import { jwt } from "better-auth/plugins";
import { prisma } from "./prisma";

export const auth = betterAuth({
    // 🔧 Use localhost for ADB reverse compatibility
    baseURL: "http://127.0.0.1:5000",
    secret: process.env.BETTER_AUTH_SECRET,
    
    logger: {
        level: "debug",
    },

    database: prismaAdapter(prisma, {
        provider: "postgresql",
    }),

    emailAndPassword: {
        enabled: true,
    },

    plugins: [
        jwt(), 
        oauthProvider({
            // Updated to match src/app/auth/sign-in and src/app/auth/consent
            loginPage: "/auth/sign-in",
            consentPage: "/auth/consent",
            clients: [
                {
                    clientId: "phia-mobile-app",
                    redirectUris: ["phia://auth/callback"],
                    requirePKCE: true,
                    type: "public",
                    tokenEndpointAuthMethod: "none",
                    skipConsent: true
                }
            ]
        })
    ],

    // This tells the server these origins are safe to talk to
    trustedOrigins: ["phia://auth", "http://127.0.0.1:5000", "http://localhost:5000", "http://192.168.31.175:5000", "http://127.0.0.1:3000", "http://localhost:3000"]
});