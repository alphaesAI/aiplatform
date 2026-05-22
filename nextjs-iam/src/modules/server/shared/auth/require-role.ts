import { redirect } from "next/navigation";
import { getServerSession } from "../../auth-provider/auth-server";

export async function requireRole(roles: string[]) {
  const session = await getServerSession();

  if (!session?.user) {
    redirect("/login");
  }

  if (!session.user.role || !roles.includes(session.user.role)) {
    throw new Error("Forbidden");
  }

  return session;
}
