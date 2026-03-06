import { FormEvent, useCallback, useEffect, useState } from "react";
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
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);
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
      setSuccess("User created successfully.");
      await loadUsers();
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unable to create user.";
      setError(message);
    } finally {
      setIsCreating(false);
    }
  }

  return (
    <section className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-semibold">Tenant Users</h2>
          <p className="text-sm text-slate-600">List existing users and create new tenant users.</p>
        </div>
        <button
          type="button"
          onClick={() => void loadUsers()}
          className="rounded border border-slate-300 px-3 py-2 text-sm font-medium hover:bg-slate-100"
        >
          Refresh
        </button>
      </div>

      <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h3 className="text-lg font-semibold">Add User</h3>
        <form className="mt-4 grid gap-4 md:grid-cols-2" onSubmit={onCreateUser}>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700" htmlFor="display-name">
              Display Name
            </label>
            <input
              id="display-name"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm"
              required
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700" htmlFor="user-email">
              Email
            </label>
            <input
              id="user-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm"
              required
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700" htmlFor="user-password">
              Password
            </label>
            <input
              id="user-password"
              type="password"
              minLength={8}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm"
              required
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700" htmlFor="user-role">
              Role
            </label>
            <select
              id="user-role"
              value={role}
              onChange={(e) => setRole(e.target.value as TenantUserRole)}
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm"
            >
              <option value="TENANT_MEMBER">TENANT_MEMBER</option>
              <option value="TENANT_ADMIN">TENANT_ADMIN</option>
            </select>
          </div>

          <div className="md:col-span-2">
            <button
              type="submit"
              disabled={isCreating}
              className="inline-flex rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60"
            >
              {isCreating ? "Creating..." : "Create User"}
            </button>
          </div>
        </form>

        {error ? <p className="mt-3 text-sm text-red-600">{error}</p> : null}
        {success ? <p className="mt-3 text-sm text-green-700">{success}</p> : null}
      </div>

      <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h3 className="text-lg font-semibold">Current Users</h3>

        {isLoading ? (
          <p className="mt-4 text-sm text-slate-600">Loading users...</p>
        ) : users.length === 0 ? (
          <p className="mt-4 text-sm text-slate-600">No users found.</p>
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead>
                <tr className="text-left text-slate-600">
                  <th className="px-3 py-2 font-medium">Display Name</th>
                  <th className="px-3 py-2 font-medium">Email</th>
                  <th className="px-3 py-2 font-medium">Role</th>
                  <th className="px-3 py-2 font-medium">User ID</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {users.map((user) => (
                  <tr key={user.userId}>
                    <td className="px-3 py-2">{user.displayName}</td>
                    <td className="px-3 py-2">{user.email}</td>
                    <td className="px-3 py-2">{user.role}</td>
                    <td className="px-3 py-2 text-xs text-slate-600">{user.userId}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </section>
  );
}
