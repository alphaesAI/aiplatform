import { ColumnDef } from "@tanstack/react-table";
import { Edit, EllipsisVertical, Trash2 } from "lucide-react";
import { TanstackTableColumnSorting } from "@/modules/client/shared/components/table/tanstack-table-column-sorting";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { cn } from "@/lib/utils";
import { buttonVariants } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { adminStore } from "../../stores/admin.store";
import { formatSmartDate } from "@/modules/shared/helper";
import { TOAuthClient } from "../../types/oauthclient.type";

export const oauthClientsTableColumn = (): ColumnDef<TOAuthClient>[] => [
  {
    header: ({ column }) => {
      const isSorted = column.getIsSorted();

      return (
        <TanstackTableColumnSorting
          label="Client Name"
          column={column}
          isSorted={isSorted}
        />
      );
    },
    accessorKey: "client_name",
  },
  {
    header: "Client ID",
    accessorKey: "client_id",
    cell({ row }) {
      return (
        <div className="flex items-center gap-2">
          <span
          // className={cn(
          //   buttonVariants({
          //     size: "sm",
          //     variant: "default",
          //     className:
          //       "cursor-default h-6 rounded-lg bg-primary/15 text-primary hover:bg-primary/20 hover:text-primary",
          //   }),
          // )}
          >
            {row.original.client_id}
          </span>
          {/* <Copy
            onClick={() =>
              navigator.clipboard.writeText(row.original.client_id)
            }
            className="size-4"
          /> */}
        </div>
      );
    },
  },
  {
    header: "Type",
    accessorKey: "public",
    cell({ row }) {
      return (
        <Badge
          className={cn(
            buttonVariants({
              size: "sm",
              variant: "default",
              className:
                "cursor-default h-6 rounded-lg bg-primary/15 text-primary hover:bg-primary/20 hover:text-primary",
            }),
          )}
        >
          {row.original.public ? "Public" : "Confidential"}
        </Badge>
      );
    },
  },
  {
    header: "Grant Types",
    accessorKey: "grant_types",
    cell({ row }) {
      return (
        <Badge
          className={cn(
            buttonVariants({
              size: "sm",
              variant: "default",
              className:
                "cursor-default h-6 rounded-lg bg-primary/15 text-primary hover:bg-primary/20 hover:text-primary",
            }),
          )}
        >
          {row.original.grant_types?.join(", ") ?? "-"}
        </Badge>
      );
    },
  },
  {
    header: "Scopes",
    accessorKey: "scope",
    cell({ row }) {
      const scopes = row.original.scope?.split(" ") || [];

      return row.original.scope ? (
        <div className="flex items-center gap-2 flex-wrap w-70">
          {scopes.map((scope, i) => (
            <Badge
              key={i}
              className={cn(
                buttonVariants({
                  size: "sm",
                  variant: "default",
                  className:
                    "cursor-default h-6 rounded-lg bg-primary/15 text-primary hover:bg-primary/20 hover:text-primary",
                }),
              )}
            >
              {scope}
            </Badge>
          ))}
        </div>
      ) : (
        "-"
      );
    },
  },
  {
    header: "Status",
    accessorKey: "disabled",
    cell: ({ row }) => {
      const disabled: boolean = row.getValue("disabled");
      return (
        <Badge
          className={cn(
            buttonVariants({
              size: "sm",
              variant: "default",
              className:
                "cursor-default h-6 rounded-lg bg-primary/15 text-primary hover:bg-primary/20 hover:text-primary",
            }),
          )}
        >
          {disabled ? "Disabled" : "Active"}
        </Badge>
      );
    },
  },
  {
    header: "Created At",
    accessorKey: "client_id_issued_at",
    cell: ({ row }) => {
      const createdAt = row.original.client_id_issued_at;
      const formattedDate = formatSmartDate(createdAt);
      return <span>{formattedDate}</span>;
    },
  },
  {
    header: "ACTIONS",
    id: "actions",
    cell: ({ row }) => {
      const oauthClientData = row.original;
      const openModal = adminStore((state) => state.onOpen);

      return (
        <DropdownMenu>
          <DropdownMenuTrigger
            className={cn(
              buttonVariants({ size: "icon", variant: "ghost" }),
              "rounded-full",
            )}
          >
            <EllipsisVertical />
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start" side="left">
            <DropdownMenuItem
              className="cursor-pointer space-x-2"
              onClick={() =>
                openModal({
                  type: "editOAuthClient",
                })
              }
            >
              <div className="flex items-center gap-2">
                <Edit />
                Edit
              </div>
            </DropdownMenuItem>
            <DropdownMenuItem
              className="cursor-pointer space-x-2 text-rose-600 hover:!text-rose-600 dark:text-rose-500 dark:hover:!text-rose-500"
              onClick={() =>
                openModal({
                  type: "deleteOAuthClient",
                })
              }
            >
              <div className="flex items-center gap-2">
                <Trash2 className="text-rose-600 dark:text-rose-500" />
                Delete
              </div>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      );
    },
  },
];
