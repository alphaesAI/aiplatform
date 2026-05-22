import { requireRole } from "@/modules/server/shared/auth/require-role";

async function UsersPage() {
  await requireRole(["superadmin"]);

  return (
    <div className="space-y-8 w-full">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold">Users</h1>
        <p className="text-sm">
          Manage user identities, roles, and access across organizations.
        </p>
      </div>
    </div>
  );
}

export default UsersPage;
