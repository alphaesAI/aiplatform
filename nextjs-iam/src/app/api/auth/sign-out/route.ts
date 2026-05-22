import { auth } from "@/lib/auth";
import { nextCookies } from "better-auth/next-js";
import { NextRequest, NextResponse } from "next/server";

export const GET = async (request: NextRequest) => {
    const callbackUrl = request.nextUrl.searchParams.get("callback_url") || "/";
    
    // 1. Perform the logout on the server side
    await auth.api.signOut({
        headers: await nextCookies()
    });

    // 2. Create the response and EXPLICITLY clear the session cookie
    const response = NextResponse.redirect(callbackUrl);
    
    // Clear the common better-auth session cookie names
    response.cookies.set("better-auth.session_token", "", { expires: new Date(0) });
    response.cookies.set("better-auth.session_cookie", "", { expires: new Date(0) });

    return response;
};
