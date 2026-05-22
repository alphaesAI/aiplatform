import { createAuthClient } from "better-auth/react"

export const authClient = createAuthClient({
    // 🚀 FIXED: Use localhost for ADB compatibility
    baseURL: "http://127.0.0.1:5000"
})

// You can use these hooks in your UI components later
export const { signIn, signUp, useSession, signOut } = authClient;