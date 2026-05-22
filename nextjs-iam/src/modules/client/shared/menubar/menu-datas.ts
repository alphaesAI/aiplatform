import {
  BriefcaseMedical,
  Building,
  CalendarClock,
  CalendarPlus,
  FileSliders,
  LayoutDashboard,
  Settings2,
  ShieldUser,
  Stethoscope,
  UserCog,
  UserRoundCog,
} from "lucide-react";

export const adminSidebarData = {
  navGroups: [
    {
      title: "OVERVIEW",
      items: [
        {
          title: "Dashboard",
          url: "/admin",
          icon: "layout-dashboard",
        },
      ],
    },
    {
      title: "IDENTITY",
      items: [
        {
          title: "Users",
          url: "/admin/users",
          icon: "users",
        },
        {
          title: "Organizations",
          url: "/admin/organizations",
          icon: "building-2",
        },
        {
          title: "Groups",
          url: "/admin/groups",
          icon: "users",
        },
      ],
    },
    {
      title: "ACCESS CONTROL",
      items: [
        {
          title: "Roles",
          url: "/admin/roles",
          icon: "shield",
        },
        {
          title: "Permissions",
          url: "/admin/permissions",
          icon: "lock",
        },
      ],
    },
    {
      title: "APPLICATION",
      items: [
        {
          title: "OAuth Clients",
          url: "/admin/oauth-clients",
          icon: "globe",
        },
        {
          title: "Service Accounts",
          url: "/admin/service-accounts",
          icon: "bot",
        },
        {
          title: "API Keys",
          url: "/admin/api-keys",
          icon: "key",
        },
      ],
    },
    {
      title: "SECURITY",
      items: [
        {
          title: "Sessions",
          url: "/admin/sessions",
          icon: "activity",
        },
        {
          title: "Audit Logs",
          url: "/admin/audit-logs",
          icon: "scroll-text",
        },
        {
          title: "Security Policies",
          url: "/admin/security-policies",
          icon: "shield-check",
        },
      ],
    },
    // {
    //   title: "Admin Management",
    //   icon: "user-star",
    //   items: [
    //     {
    //       title: "Manage Apps",
    //       url: "/bezs/admin/manage-apps",
    //       icon: LayoutDashboard,
    //     },
    //     {
    //       title: "Manage Organizations",
    //       url: "/bezs/admin/manage-organizations",
    //       icon: Building,
    //     },
    //     {
    //       title: "Manage Roles",
    //       url: "/bezs/admin/manage-roles",
    //       icon: UserRoundCog,
    //     },
    //     {
    //       title: "RBAC",
    //       url: "/bezs/admin/rbac",
    //       icon: ShieldUser,
    //     },
    //     {
    //       title: "Preference Templates",
    //       url: "/bezs/admin/manage-preferences",
    //       icon: FileSliders,
    //     },
    //   ],
    // },
  ],
};

export const telemedicineSidebarData = {
  navGroups: [
    {
      title: "Admin Management",
      items: [
        {
          title: "Manage Doctors",
          url: "/bezs/telemedicine/admin/manage-doctors",
          icon: LayoutDashboard,
        },
      ],
    },
    {
      title: "Doctor",
      items: [
        {
          title: "Dashboard",
          url: "/bezs/telemedicine/doctor",
          icon: LayoutDashboard,
        },
        {
          title: "Professional Settings",
          icon: Stethoscope,
          items: [
            {
              title: "Services",
              url: "/bezs/telemedicine/doctor/settings/services",
              icon: BriefcaseMedical,
            },
            {
              title: "Availability",
              url: "/bezs/telemedicine/doctor/settings/availability",
              icon: CalendarClock,
            },
          ],
        },
        {
          title: "Settings",
          icon: Settings2,
          items: [
            {
              title: "Profile",
              url: "/bezs/telemedicine/doctor/settings/profile",
              icon: UserCog,
            },
          ],
        },
      ],
    },
    {
      title: "Patient",
      items: [
        {
          title: "Dashboard",
          url: "/bezs/telemedicine/patient",
          icon: LayoutDashboard,
        },
        {
          title: "Profile",
          url: "/bezs/telemedicine/patient/profile",
          icon: UserCog,
        },
        {
          title: "Book Appointment",
          url: "/bezs/telemedicine/patient/appointments/book",
          icon: CalendarPlus,
        },
        {
          title: "Appointment Intake",
          url: "/bezs/telemedicine/patient/askai",
          icon: CalendarPlus,
        },
      ],
    },
    // {
    //   title: "Others",
    //   items: [
    //     {
    //       title: "Settings",
    //       icon: Settings,
    //       items: [
    //         {
    //           title: "Profile",
    //           url: "/bezs/telemedicine/doctor/profile",
    //           icon: UserCog,
    //         },
    //         {
    //           title: "Availability",
    //           url: "/bezs/telemedicine/doctor/availability",
    //           icon: CalendarClock,
    //         },
    //       ],
    //     },
    //   ],
    // },
  ],
};
