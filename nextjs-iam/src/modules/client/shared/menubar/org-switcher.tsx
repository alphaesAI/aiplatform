"use client";

import * as LucideIcons from "lucide-react";
import { ChevronsUpDown, Plus } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuShortcut,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar";
import { useState } from "react";
import { Badge } from "@/components/ui/badge";

type TProps = {
  orgs: {
    name: string;
    id: string;
    metadata: string | null;
    slug: string;
    logo: string | null;
  }[];
  currentOrgId?: string | null;
};

export function OrgSwitcher({ orgs, currentOrgId }: TProps) {
  const { isMobile } = useSidebar();
  const [activeOrg, setActiveOrg] = useState(orgs[0]);

  const getActiveIcon = (icon?: string | null) => {
    const IconComponent =
      LucideIcons[icon as keyof typeof LucideIcons] || LucideIcons.LayoutGrid;
    return IconComponent as React.ComponentType<{ className?: string }>;
  };

  const ActiveIcon = getActiveIcon(activeOrg.logo);

  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <SidebarMenuButton
              size="lg"
              className="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground"
            >
              <div className="bg-sidebar-primary text-sidebar-primary-foreground flex aspect-square size-8 items-center justify-center rounded-lg">
                <ActiveIcon className="size-4" />
              </div>
              <div className="grid flex-1 text-start text-sm leading-tight">
                <span className="truncate font-semibold">{activeOrg.name}</span>
                {/* <span className="truncate text-xs">{activeTeam.plan}</span> */}
              </div>
              <ChevronsUpDown className="ms-auto" />
            </SidebarMenuButton>
          </DropdownMenuTrigger>
          <DropdownMenuContent
            className="w-(--radix-dropdown-menu-trigger-width) min-w-56 rounded-lg"
            align="start"
            side={isMobile ? "bottom" : "right"}
            sideOffset={4}
          >
            <DropdownMenuLabel className="text-muted-foreground text-xs">
              Organizations
            </DropdownMenuLabel>
            {orgs.map((org, index) => {
              const Icon = getActiveIcon(org.logo);

              return (
                <DropdownMenuItem
                  key={org.name}
                  onClick={() => setActiveOrg(org)}
                  className="gap-2 p-2 justify-between"
                >
                  <div className="flex items-center gap-2">
                    <div className="flex size-6 items-center justify-center rounded-sm border">
                      <Icon className="size-4 shrink-0" />
                    </div>
                    {org.name}
                  </div>
                  {/* <DropdownMenuShortcut>⌘{index + 1}</DropdownMenuShortcut> */}
                  <Badge className="bg-green-400 text-black">Active</Badge>
                </DropdownMenuItem>
              );
            })}
            {/* <DropdownMenuSeparator />
            <DropdownMenuItem className="gap-2 p-2">
              <div className="bg-background flex size-6 items-center justify-center rounded-md border">
                <Plus className="size-4" />
              </div>
              <div className="text-muted-foreground font-medium">Add team</div>
            </DropdownMenuItem> */}
          </DropdownMenuContent>
        </DropdownMenu>
      </SidebarMenuItem>
    </SidebarMenu>
  );
}
