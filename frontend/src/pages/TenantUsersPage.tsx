import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { EmptyState } from "../components/ui/EmptyState";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { PageHeader } from "../components/ui/PageHeader";
import { Panel } from "../components/ui/Panel";
import { Sheet } from "../components/ui/Sheet";
import { createUser, listUsers, type TenantUserRole, type UserResponse } from "../lib/api";

const defaultForm = {
  displayName: "",
  email: "",
  password: "",
  role: "TENANT_MEMBER" as TenantUserRole,
};

export function TenantUsersPage() {
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [displayName, setDisplayName] = useState(defaultForm.displayName);
  const [email, setEmail] = useState(defaultForm.email);
  const [password, setPassword] = useState(defaultForm.password);
  const [role, setRole] = useState<TenantUserRole>(defaultForm.role);
  const [search, setSearch] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
  const [sheetOpen, setSheetOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const loadUsers = useCallback(async () => {
    setError(null);
    setIsLoading(true);

    try {
      const data = await listUsers();
      setUsers(data);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unable to load tenant users.";
      setError(message);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadUsers();
  }, [loadUsers]);

  async function onCreateUser(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    setIsCreating(true);

    try {
      await createUser({ displayName, email, password, role });
      setDisplayName(defaultForm.displayName);
      setEmail(defaultForm.email);
      setPassword(defaultForm.password);
      setRole(defaultForm.role);
      setSheetOpen(false);
      setSuccess("User invited successfully.");
      await loadUsers();
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unable to create user.";
      setError(message);
    } finally {
      setIsCreating(false);
    }
  }

  const filteredUsers = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) {
      return users;
    }

    return users.filter((user) => {
      return user.displayName.toLowerCase().includes(query) || user.email.toLowerCase().includes(query);
    });
  }, [search, users]);

  return (
    <section>
      <PageHeader
        title="Tenant users"
        breadcrumb="Manage"
        description="Search tenant members and invite new operators with role-based access."
        actions={
          <>
            <Button variant="ghost" onClick={() => void loadUsers()}>
              Refresh
            </Button>
            <Button onClick={() => setSheetOpen(true)}>Invite user</Button>
          </>
        }
      />

      {error ? <ErrorState message={error} onRetry={() => void loadUsers()} /> : null}
      {success ? <p className="mb-4 rounded-lg border border-emerald-400 bg-emerald-500 px-3 py-2 text-sm text-white">{success}</p> : null}

      <Panel>
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <input
            type="search"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search by name or email"
            className="app-input md:max-w-sm"
          />
          <Button onClick={() => setSheetOpen(true)}>Invite user</Button>
        </div>

        {isLoading ? <LoadingState label="Loading users..." /> : null}

        {!isLoading && filteredUsers.length === 0 ? (
          <EmptyState
            title="No users found"
            description="Invite your first teammate to start collaborating in OpsPilot."
            actionLabel="Invite user"
            onAction={() => setSheetOpen(true)}
          />
        ) : null}

        {!isLoading && filteredUsers.length > 0 ? (
          <div className="overflow-x-auto rounded-xl border border-border">
            <table className="min-w-full divide-y divide-border text-sm">
              <thead className="bg-surface-elevated">
                <tr className="text-left text-muted">
                  <th className="px-3 py-2.5 font-medium">User</th>
                  <th className="px-3 py-2.5 font-medium">Email</th>
                  <th className="px-3 py-2.5 font-medium">Role</th>
                  <th className="px-3 py-2.5 font-medium">Joined</th>
                  <th className="px-3 py-2.5 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/80 bg-surface">
                {filteredUsers.map((user) => (
                  <tr key={user.userId}>
                    <td className="px-3 py-3">
                      <div className="flex items-center gap-2.5">
                        <span className="inline-flex h-8 w-8 items-center justify-center rounded-full bg-amber-500 text-xs font-semibold text-slate-950">
                          {user.displayName.slice(0, 1).toUpperCase()}
                        </span>
                        <span className="font-medium text-foreground">{user.displayName}</span>
                      </div>
                    </td>
                    <td className="px-3 py-3 text-muted">{user.email}</td>
                    <td className="px-3 py-3">
                      <Badge variant={user.role === "TENANT_ADMIN" ? "info" : "neutral"}>
                        {user.role === "TENANT_ADMIN" ? "Admin" : "Member"}
                      </Badge>
                    </td>
                    <td className="px-3 py-3 text-muted">Not available</td>
                    <td className="px-3 py-3">
                      <button
                        type="button"
                        className="rounded-md border border-border px-2 py-1 text-xs text-muted transition-colors hover:bg-surface-elevated"
                        aria-label={`Actions for ${user.displayName}`}
                      >
                        Actions
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </Panel>

      <Sheet
        open={sheetOpen}
        onClose={() => setSheetOpen(false)}
        title="Invite tenant user"
        description="Create a user account and assign role permissions."
      >
        <form className="space-y-4" onSubmit={onCreateUser}>
          <div>
            <label className="app-label" htmlFor="display-name">
              Display name
            </label>
            <input
              id="display-name"
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              className="app-input"
              required
            />
          </div>

          <div>
            <label className="app-label" htmlFor="user-email">
              Email
            </label>
            <input
              id="user-email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="app-input"
              required
            />
          </div>

          <div>
            <label className="app-label" htmlFor="user-password">
              Password
            </label>
            <input
              id="user-password"
              type="password"
              minLength={8}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="app-input"
              required
            />
          </div>

          <div>
            <label className="app-label" htmlFor="user-role">
              Role
            </label>
            <select
              id="user-role"
              value={role}
              onChange={(event) => setRole(event.target.value as TenantUserRole)}
              className="app-select"
            >
              <option value="TENANT_MEMBER">Member</option>
              <option value="TENANT_ADMIN">Admin</option>
            </select>
          </div>

          <div className="pt-2">
            <Button type="submit" disabled={isCreating} className="w-full">
              {isCreating ? "Inviting..." : "Invite user"}
            </Button>
          </div>
        </form>
      </Sheet>
    </section>
  );
}
