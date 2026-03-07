import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { clearTokens, getAuthClaims } from "../lib/auth";

const phaseTwoNavItems = [
  { to: "/dashboard", label: "Dashboard" },
  { to: "/tenant-users", label: "Tenant Users" },
  { to: "/tenant-settings", label: "Tenant Settings" },
  { to: "/documents", label: "Documents" },
];

const upcomingNavItems = [
  { to: "/chat", label: "Chat" },
  { to: "/tickets", label: "Tickets" },
  { to: "/analytics", label: "Analytics" },
];

export function AppLayout() {
  const navigate = useNavigate();
  const claims = getAuthClaims();

  function onLogout() {
    clearTokens();
    navigate("/login", { replace: true });
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto max-w-6xl px-4 py-4">
          <div className="mb-4 flex items-center justify-between gap-4">
            <div>
              <h1 className="text-xl font-semibold">OpsPilot Admin</h1>
              <p className="text-xs text-slate-600">
                {claims?.email ?? "Unknown user"}
                {claims?.role ? ` · ${claims.role}` : ""}
              </p>
            </div>
            <button
              type="button"
              onClick={onLogout}
              className="rounded border border-slate-300 px-3 py-2 text-sm font-medium hover:bg-slate-100"
            >
              Logout
            </button>
          </div>

          <div className="flex flex-wrap items-center gap-2 text-sm">
            {phaseTwoNavItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `rounded px-3 py-2 ${isActive ? "bg-slate-900 text-white" : "hover:bg-slate-100"}`
                }
              >
                {item.label}
              </NavLink>
            ))}
            <span className="mx-1 text-slate-400">|</span>
            {upcomingNavItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `rounded px-3 py-2 ${isActive ? "bg-slate-900 text-white" : "text-slate-500 hover:bg-slate-100"}`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  );
}
