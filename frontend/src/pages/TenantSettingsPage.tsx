import { FormEvent, useEffect, useMemo, useState } from "react";
import { Button } from "../components/ui/Button";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { PageHeader } from "../components/ui/PageHeader";
import { Panel } from "../components/ui/Panel";
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

  const lineNumbers = useMemo(() => {
    return Array.from({ length: Math.max(settingsJson.split("\n").length, 6) }, (_, index) => index + 1);
  }, [settingsJson]);

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
      setSuccess("Settings saved.");
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unable to update tenant settings.";
      setError(message);
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <section>
      <PageHeader
        title="Tenant settings"
        breadcrumb="Manage"
        description="Configure organization-level identity and advanced runtime settings."
      />

      {error ? <ErrorState message={error} /> : null}
      {success ? <p className="mb-4 rounded-lg border border-emerald-400 bg-emerald-500 px-3 py-2 text-sm text-white">{success}</p> : null}
      {isLoading ? <LoadingState label="Loading tenant settings..." /> : null}

      {!isLoading ? (
        <form className="space-y-5" onSubmit={onSubmit}>
          <Panel>
            <h2 className="text-lg font-semibold text-foreground">General</h2>
            <p className="mt-1 text-sm text-muted">Update your tenant display name.</p>
            <div className="mt-4">
              <label className="app-label" htmlFor="tenant-name">
                Tenant name
              </label>
              <input
                id="tenant-name"
                value={name}
                onChange={(event) => setName(event.target.value)}
                className="app-input md:max-w-xl"
                required
              />
            </div>
          </Panel>

          <Panel>
            <h2 className="text-lg font-semibold text-foreground">Advanced</h2>
            <p className="mt-1 text-sm text-muted">Provide JSON settings used by backend services.</p>

            <div className="mt-4 overflow-hidden rounded-xl border border-sky-400 bg-[#15263a]">
              <div className="grid grid-cols-[3rem_1fr]">
                <div className="border-r border-sky-400 bg-[#102033] px-2 py-3 font-mono text-xs text-sky-300">
                  {lineNumbers.map((line) => (
                    <p key={line} className="text-right leading-6">
                      {line}
                    </p>
                  ))}
                </div>
                <textarea
                  id="tenant-settings"
                  value={settingsJson}
                  onChange={(event) => setSettingsJson(event.target.value)}
                  className="min-h-52 w-full resize-y bg-[#15263a] px-3 py-3 font-mono text-xs leading-6 text-foreground outline-none"
                  spellCheck={false}
                />
              </div>
            </div>
            <p className="mt-2 text-xs text-muted">Character count: {settingsJson.length}</p>
          </Panel>

          <Panel className="border-rose-400">
            <h2 className="inline-flex rounded-full border border-rose-400 bg-rose-500 px-3 py-1 text-sm font-semibold text-white">Danger zone</h2>
            <p className="mt-1 text-sm text-muted">Destructive tenant actions are not available yet in this phase.</p>
          </Panel>

          <div className="flex items-center justify-end">
            <Button type="submit" disabled={isSaving}>
              {isSaving ? "Saving..." : "Save settings"}
            </Button>
          </div>
        </form>
      ) : null}
    </section>
  );
}
