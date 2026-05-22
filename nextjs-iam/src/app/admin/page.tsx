import { Button } from "@/components/ui/button";
import { requireRole } from "@/modules/server/shared/auth/require-role";

async function AdminPage() {
  await requireRole(["superadmin"]);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Admin</h1>
        <Button>Add User</Button>
      </div>
    </div>
  );
}

export default AdminPage;
