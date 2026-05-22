"use client";

import { signoutAction } from "@/modules/server/presentation/actions/auth";
import { useServerAction } from "zsa-react";
import { TSession } from "../../types/auth-types";
import { Button } from "@/components/ui/button";
import Link from "next/link";

function Home({ session }: { session: TSession | null }) {
  const { execute: executeSignoutAction, isPending: isSignoutActionPending } =
    useServerAction(signoutAction);

  async function handleSignout() {
    await executeSignoutAction({});
  }

  return (
    <div className="my-6 px-4 max-w-md mx-auto">
      <div className="text-center space-y-6">
        {session ? (
          <>
            <div className="text-2xl">Welcome {session.user.name}</div>
            <Button
              variant="destructive"
              onClick={handleSignout}
              disabled={isSignoutActionPending}
            >
              Sign Out
            </Button>
          </>
        ) : (
          <>
            <div className="text-2xl">You are not logged in</div>
            <Button asChild size="lg">
              <Link href="/auth/sign-in">Sign In / Sign Up</Link>
            </Button>
          </>
        )}
      </div>
    </div>
  );
}

export default Home;
