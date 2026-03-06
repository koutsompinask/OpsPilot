import { FormEvent, useEffect, useState } from "react";
import { getMyTenant, updateMyTenant } from "../lib/api";

export function TenantSettingsPage() {
  const [name, setName] = useState("");
  const [settingsJson, setSettingsJson] = useState("{}");
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    async function loadTenant() {
      setError(null);
      setIsLoading(true);

      try {
        const tenant = await getMyTenant();
        setName(tenant.name ?? "");
        setSettingsJson(tenant.settingsJson ?? "{}");
      } catch (err) {
        const message = err instanceof Error ? err.message : "Unable to load tenant settings.";
        setError(message);
      } finally {
        setIsLoading(false);
      }
    }

    void loadTenant();
  }, []);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    setIsSaving(true);

    try {
      JSON.parse(settingsJson);
    } catch {
      setError("Settings JSON must be valid JSON.");
      setIsSaving(false);
      return;
    }

    try {
      const updated = await updateMyTenant({ name, settingsJson });
      setName(updated.name ?? "");
      setSettingsJson(updated.settingsJson ?? "{}");
      setSuccess("Tenant settings updated.");
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unable to update tenant settings.";
      setError(message);
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-2xl font-semibold">Tenant Settings</h2>
      <p className="mt-1 text-sm text-slate-600">Update tenant name and JSON settings via `PUT /tenants/me`.</p>

      {isLoading ? (
        <p className="mt-6 text-sm text-slate-600">Loading tenant settings...</p>
      ) : (
        <form className="mt-6 space-y-4" onSubmit={onSubmit}>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700" htmlFor="tenant-name">
              Tenant Name
            </label>
            <input
              id="tenant-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full rounded border border-slate-300 px-3 py-2 text-sm"
              required
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700" htmlFor="tenant-settings">
              Settings JSON
            </label>
            <textarea
              id="tenant-settings"
              value={settingsJson}
              onChange={(e) => setSettingsJson(e.target.value)}
              className="min-h-40 w-full rounded border border-slate-300 px-3 py-2 font-mono text-xs"
            />
          </div>

          {error ? <p className="text-sm text-red-600">{error}</p> : null}
          {success ? <p className="text-sm text-green-700">{success}</p> : null}

          <button
            type="submit"
            disabled={isSaving}
            className="inline-flex rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60"
          >
            {isSaving ? "Saving..." : "Save Changes"}
          </button>
        </form>
      )}
    </section>
  );
}
