import { useCallback, useEffect, useState } from "react";
import { getAuthClaims } from "../lib/auth";
import { getMyTenant, listUsers, type TenantResponse, type UserResponse } from "../lib/api";

export function DashboardPage() {
  const claims = getAuthClaims();
  const [tenant, setTenant] = useState<TenantResponse | null>(null);
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const loadData = useCallback(async () => {
    setError(null);
    setIsLoading(true);

    try {
      const tenantData = await getMyTenant();
      setTenant(tenantData);

      if (claims?.role === "TENANT_ADMIN") {
        const userData = await listUsers();
        setUsers(userData);
      } else {
        setUsers([]);
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unable to load dashboard data.";
      setError(message);
    } finally {
      setIsLoading(false);
    }
  }, [claims?.role]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  return (
    <section className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-semibold">Phase 2 Dashboard</h2>
          <p className="text-sm text-slate-600">Tenant and user management summary for auth + tenant services.</p>
        </div>
        <button
          type="button"
          onClick={() => void loadData()}
          className="rounded border border-slate-300 px-3 py-2 text-sm font-medium hover:bg-slate-100"
        >
          Refresh
        </button>
      </div>

      {error ? <p className="rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p> : null}

      {isLoading ? (
        <p className="text-sm text-slate-600">Loading dashboard...</p>
      ) : (
        <div className="grid gap-4 md:grid-cols-3">
          <article className="rounded border border-slate-200 bg-white p-4 shadow-sm">
            <h3 className="text-sm font-medium text-slate-600">Tenant Name</h3>
            <p className="mt-2 text-lg font-semibold">{tenant?.name ?? "-"}</p>
          </article>

          <article className="rounded border border-slate-200 bg-white p-4 shadow-sm">
            <h3 className="text-sm font-medium text-slate-600">Tenant ID</h3>
            <p className="mt-2 break-all text-xs text-slate-800">{tenant?.id ?? "-"}</p>
          </article>

          <article className="rounded border border-slate-200 bg-white p-4 shadow-sm">
            <h3 className="text-sm font-medium text-slate-600">Users</h3>
            <p className="mt-2 text-lg font-semibold">{claims?.role === "TENANT_ADMIN" ? users.length : "Admin only"}</p>
          </article>
        </div>
      )}
    </section>
  );
}
