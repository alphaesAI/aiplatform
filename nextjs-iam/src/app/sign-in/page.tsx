"use client";

import { useState, useEffect, Suspense } from "react";
import { authClient } from "@/lib/auth-client"; 
import { useSearchParams, useRouter } from "next/navigation";

function SignInForm() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    
    const searchParams = useSearchParams();
    const router = useRouter();
    const { data: session } = authClient.useSession();

    // 🚀 NEW: Screen Hint Logic - Redirect to real signup if requested
    useEffect(() => {
        const hint = searchParams.get("screen_hint");
        if (hint === "signup") {
            router.replace(`/auth/sign-up?${searchParams.toString()}`);
        }
    }, [searchParams, router]);

    // 🚀 THE LOOP BREAKER: This effect runs when you are successfully logged in
    useEffect(() => {
        if (session) {
            // 1. Get the current URL parameters (client_id, redirect_uri, etc.)
            const params = new URLSearchParams(searchParams.toString());
            
            // 2. 🛑 IMPORTANT: Remove "prompt" so the server doesn't ask for login again
            params.delete("prompt"); 

            console.log("Session found! Redirecting back to OAuth flow...");
            
            // 3. Send the user back to the authorize endpoint to finish the connection to the app
            window.location.href = `/api/auth/oauth2/authorize?${params.toString()}`;
        }
    }, [session, searchParams]);

    const handleSignIn = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError("");

        const { error: authError } = await authClient.signIn.email({
            email,
            password,
            // Reloading the current URL will trigger the session check above
            callbackURL: window.location.href, 
        });

        if (authError) {
            setError(authError.message || "Invalid email or password");
        }
        setLoading(false);
    };

    // If session is active, show a simple redirecting message
    if (session) {
        return (
            <div className="flex flex-col items-center justify-center min-h-screen bg-white p-6 text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4"></div>
                <p className="text-xl font-semibold text-black">Redirecting back to PHIA App...</p>
            </div>
        );
    }

    return (
        <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50 p-6">
            <div className="w-full max-w-md bg-white rounded-2xl shadow-xl p-8 border border-gray-100">
                <h1 className="text-3xl font-bold text-black mb-2">PHIA</h1>
                <p className="text-gray-500 mb-8 font-medium">Sign in to sync your health data.</p>

                <form onSubmit={handleSignIn} className="space-y-4">
                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-1">Email Address</label>
                        <input
                            type="email"
                            className="w-full px-4 py-3 rounded-lg border border-gray-300 outline-none focus:ring-2 focus:ring-blue-500 text-black bg-white"
                            placeholder="surya@test.com"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-1">Password</label>
                        <input
                            type="password"
                            className="w-full px-4 py-3 rounded-lg border border-gray-300 outline-none focus:ring-2 focus:ring-blue-500 text-black bg-white"
                            placeholder="••••••••"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </div>

                    {error && (
                        <div className="bg-red-50 text-red-600 p-3 rounded-lg text-sm font-medium border border-red-100">
                            {error}
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 rounded-lg transition-all duration-200 disabled:opacity-50 mt-4 shadow-md active:scale-95"
                    >
                        {loading ? "Verifying..." : "Sign In"}
                    </button>
                </form>

                <div className="mt-8 pt-6 border-t border-gray-100 text-center">
                    <p className="text-sm text-gray-500">
                        Don't have an account?{" "}
                        <button 
                            onClick={() => router.push(`/auth/sign-up?${searchParams.toString()}`)}
                            className="text-blue-600 font-bold cursor-pointer hover:underline bg-transparent border-none p-0"
                        >
                            Create one
                        </button>
                    </p>
                </div>
            </div>
        </div>
    );
}

// Next.js requires Suspense for useSearchParams
export default function SignInPage() {
    return (
        <Suspense fallback={<div className="text-black p-10">Loading...</div>}>
            <SignInForm />
        </Suspense>
    );
}