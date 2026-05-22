import { auth } from "@/lib/auth";
import { toNextJsHandler } from "better-auth/next-js";
import { NextRequest, NextResponse } from "next/server";

export const dynamic = "force-dynamic";
export const revalidate = 0;

const { GET: getHandler, POST: postHandler } = toNextJsHandler(auth);

export async function POST(req: NextRequest) {
    const isTokenReq = req.nextUrl.pathname.endsWith("/oauth2/token");
    if (isTokenReq) {
        console.log("--- 🕵️ SENIOR INSPECTOR: TOKEN REQUEST ---");
        console.log("Headers:", Object.fromEntries(req.headers.entries()));
        
        const clonedReq = req.clone();
        try {
            const formData = await clonedReq.formData();
            console.log("Body Params:", Object.fromEntries(formData.entries()));
        } catch (e) {
            console.log("Body is not FormData");
        }
    }

    const res = await postHandler(req);

    if (!res.ok) {
        const clonedRes = res.clone();
        const errBody = await clonedRes.text();
        console.error(`--- ❌ ERROR (${res.status}) ---`);
        console.error(errBody);
    }

    return res;
}

export async function GET(req: NextRequest) {
    return getHandler(req);
}
