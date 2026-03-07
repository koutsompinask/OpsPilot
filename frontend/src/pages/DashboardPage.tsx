import { useCallback, useEffect, useMemo, useState } from "react";
import { Card } from "../components/ui/Card";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { PageHeader } from "../components/ui/PageHeader";
import { Panel } from "../components/ui/Panel";
import { Button } from "../components/ui/Button";
import { getMyTenant, listDocuments, listUsers, type TenantResponse, type UserResponse } from "../lib/api";
import { getAuthClaims } from "../lib/auth";

function formatShortDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleDateString();
}

export function DashboardPage() {
  const claims = getAuthClaims();
  const isAdmin = claims?.role === "TENANT_ADMIN";

  const [tenant, setTenant] = useState<TenantResponse | null>(null);
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [documentsCount, setDocumentsCount] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    setError(null);
    setIsLoading(true);

    try {
      const [tenantData, docs] = await Promise.all([getMyTenant(), listDocuments()]);
      setTenant(tenantData);
      setDocumentsCount(docs.length);

      if (isAdmin) {
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
  }, [isAdmin]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const metrics = useMemo(
    () => [
      {
        label: "Total documents",
        value: String(documentsCount),
        trend: "+2 this week",
        tone: "border-sky-400 bg-sky-500 text-white",
      },
      {
        label: "Total users",
        value: isAdmin ? String(users.length) : "Member",
        trend: isAdmin ? "Active tenant users" : "Admin managed",
        tone: "border-emerald-400 bg-emerald-500 text-white",
      },
      {
        label: "Tickets open",
        value: "0",
        trend: "Support workflow pending",
        tone: "border-amber-400 bg-amber-500 text-slate-950",
      },
      {
        label: "Chat queries today",
        value: "0",
        trend: "Analytics pending",
        tone: "border-amber-400 bg-amber-500 text-slate-950",
      },
    ],
    [documentsCount, isAdmin, users.length],
  );

  return (
    <section>
      <PageHeader
        title={tenant?.name ?? "Tenant dashboard"}
        breadcrumb="Core"
        description="Operational snapshot of your workspace, users, and document knowledge base."
        actions={
          <Button variant="ghost" onClick={() => void loadData()}>
            Refresh
          </Button>
        }
      />

      {error ? <ErrorState message={error} onRetry={() => void loadData()} /> : null}
      {isLoading ? <LoadingState label="Loading dashboard..." /> : null}

      {!isLoading && !error ? (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {metrics.map((metric) => (
              <Card key={metric.label} className={`border ${metric.tone}`}>
                <p className="text-xs uppercase tracking-[0.16em] text-muted">{metric.label}</p>
                <p className="mt-3 text-3xl font-semibold">{metric.value}</p>
                <p className="mt-2 text-xs text-muted">{metric.trend}</p>
              </Card>
            ))}
          </div>

          <div className="mt-5 grid gap-5 xl:grid-cols-[1.5fr_1fr]">
            <Panel>
              <h3 className="text-lg font-semibold text-foreground">Recent user activity</h3>
              <p className="mt-1 text-sm text-muted">Latest tenant members currently in your workspace.</p>
              <div className="mt-4 space-y-3">
                {users.slice(0, 5).map((user) => (
                  <div key={user.userId} className="flex items-center justify-between rounded-xl border border-border bg-surface px-3 py-2.5">
                    <div>
                      <p className="text-sm font-medium text-foreground">{user.displayName}</p>
                      <p className="text-xs text-muted">{user.email}</p>
                    </div>
                    <p className="text-xs uppercase tracking-wide text-muted">{user.role}</p>
                  </div>
                ))}
                {users.length === 0 ? <p className="text-sm text-muted">No user activity available yet.</p> : null}
              </div>
            </Panel>

            <Panel>
              <h3 className="text-lg font-semibold text-foreground">Quick actions</h3>
              <p className="mt-1 text-sm text-muted">Jump to the workflows used most often.</p>
              <div className="mt-4 space-y-3 text-sm text-foreground">
                <div className="rounded-xl border border-sky-400 bg-sky-500 p-3 text-white">
                  <p className="font-medium">Upload a new knowledge document</p>
                  <p className="mt-1 text-xs text-muted">Keep AI answers grounded in current policies.</p>
                </div>
                <div className="rounded-xl border border-emerald-400 bg-emerald-500 p-3 text-white">
                  <p className="font-medium">Invite a tenant user</p>
                  <p className="mt-1 text-xs text-muted">Onboard operators with role-based access.</p>
                </div>
                <div className="rounded-xl border border-amber-400 bg-amber-500 p-3 text-slate-950">
                  <p className="font-medium">Ask the assistant</p>
                  <p className="mt-1 text-xs text-muted">Validate coverage with representative customer questions.</p>
                </div>
                <p className="pt-2 text-xs text-muted">Tenant ID: {tenant?.id ?? "-"}</p>
                <p className="text-xs text-muted">Last refreshed: {formatShortDate(new Date().toISOString())}</p>
              </div>
            </Panel>
          </div>
        </>
      ) : null}
    </section>
  );
}
