import { useMemo, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { clearTokens, getAuthClaims } from "../lib/auth";
import { cn } from "../lib/cn";

type NavItem = {
  to: string;
  label: string;
};

type NavGroup = {
  title: string;
  items: NavItem[];
};

const navGroups: NavGroup[] = [
  {
    title: "Core",
    items: [
      { to: "/dashboard", label: "Dashboard" },
      { to: "/chat", label: "Chat" },
    ],
  },
  {
    title: "Manage",
    items: [
      { to: "/documents", label: "Documents" },
      { to: "/tenant-users", label: "Users" },
      { to: "/tenant-settings", label: "Settings" },
    ],
  },
  {
    title: "Insights",
    items: [
      { to: "/tickets", label: "Tickets" },
      { to: "/analytics", label: "Analytics" },
    ],
  },
];

const routeTitles: Record<string, string> = {
  "/dashboard": "Dashboard",
  "/chat": "AI Chat",
  "/documents": "Documents",
  "/tenant-users": "Tenant Users",
  "/tenant-settings": "Tenant Settings",
  "/tickets": "Tickets",
  "/analytics": "Analytics",
};

function SidebarContent({ onNavigate }: { onNavigate?: () => void }) {
  const claims = getAuthClaims();
  const navigate = useNavigate();

  function onLogout() {
    clearTokens();
    navigate("/login", { replace: true });
  }

  return (
    <div className="flex h-full flex-col">
      <div className="border-b border-border px-5 py-5">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-amber-400">OpsPilot</p>
        <h1 className="mt-2 text-lg font-semibold text-foreground">Operations Console</h1>
        <p className="mt-1 text-xs text-muted">{claims?.tenant_id ?? "Unknown tenant"}</p>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-4">
        <div className="space-y-4">
          {navGroups.map((group, groupIndex) => (
            <section
              key={group.title}
              className="motion-safe:animate-sidebar-in"
              style={{ animationDelay: `${groupIndex * 70}ms` }}
            >
              <p className="px-2 text-[11px] font-semibold uppercase tracking-[0.16em] text-muted">{group.title}</p>
              <div className="mt-2 space-y-1">
                {group.items.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    onClick={onNavigate}
                    className={({ isActive }) =>
                      cn(
                        "relative flex items-center rounded-lg px-3 py-2.5 text-sm font-medium transition-colors duration-150",
                        isActive
                          ? "bg-surface-elevated text-foreground before:absolute before:inset-y-1 before:left-0 before:w-1 before:rounded-r before:bg-accent"
                          : "text-muted hover:bg-surface-elevated hover:text-foreground",
                      )
                    }
                  >
                    {item.label}
                  </NavLink>
                ))}
              </div>
            </section>
          ))}
        </div>
      </nav>

      <div className="border-t border-border p-4">
        <div className="rounded-xl border border-border bg-surface-elevated p-3">
          <div className="flex items-center gap-3">
            <div className="inline-flex h-10 w-10 items-center justify-center rounded-full bg-amber-500 text-sm font-semibold text-slate-950">
              {(claims?.email ?? "U").slice(0, 1).toUpperCase()}
            </div>
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-foreground">{claims?.email ?? "Unknown user"}</p>
              <Badge variant={claims?.role === "TENANT_ADMIN" ? "info" : "neutral"}>{claims?.role ?? "No role"}</Badge>
            </div>
          </div>
          <Button variant="ghost" size="sm" className="mt-3 w-full" onClick={onLogout}>
            Logout
          </Button>
        </div>
      </div>
    </div>
  );
}

export function AppLayout() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const location = useLocation();

  const pageTitle = useMemo(() => routeTitles[location.pathname] ?? "OpsPilot", [location.pathname]);

  return (
    <div className="min-h-screen bg-transparent text-foreground">
      <div className="mx-auto flex min-h-screen w-full max-w-[1920px]">
        <aside className="hidden w-60 border-r border-border bg-gradient-to-b from-surface via-surface to-surface-elevated lg:block">
          <SidebarContent />
        </aside>

        {mobileMenuOpen ? (
          <div className="fixed inset-0 z-50 lg:hidden">
            <div className="absolute inset-0 bg-black/40" onClick={() => setMobileMenuOpen(false)} aria-hidden />
            <aside className="relative h-full w-72 border-r border-border bg-surface">
              <SidebarContent onNavigate={() => setMobileMenuOpen(false)} />
            </aside>
          </div>
        ) : null}

        <div className="flex min-w-0 flex-1 flex-col">
          <header className="sticky top-0 z-20 border-b border-border bg-gradient-to-r from-bg/95 via-surface/70 to-bg/95 px-4 py-3 backdrop-blur md:px-6">
            <div className="flex items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <Button variant="ghost" size="sm" className="lg:hidden" onClick={() => setMobileMenuOpen(true)}>
                  Menu
                </Button>
                <div>
                  <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted">OpsPilot</p>
                  <h2 className="text-lg font-semibold text-foreground">{pageTitle}</h2>
                </div>
              </div>
            </div>
          </header>

          <main className="flex-1 p-4 md:p-6">
            <div className="motion-safe-enter">
              <Outlet />
            </div>
          </main>
        </div>
      </div>
    </div>
  );
}
