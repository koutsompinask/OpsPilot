import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Button } from "../components/ui/Button";
import { register } from "../lib/api";
import { saveTokens } from "../lib/auth";

export function RegisterPage() {
  const navigate = useNavigate();
  const [tenantName, setTenantName] = useState("");
  const [adminName, setAdminName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      const tokens = await register({ tenantName, adminName, email, password });
      saveTokens(tokens);
      navigate("/dashboard");
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unable to register.";
      setError(message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="grid min-h-screen bg-bg lg:grid-cols-[1.05fr_0.95fr]">
      <section className="relative hidden overflow-hidden border-r border-border lg:block">
        <div className="absolute inset-0 bg-gradient-to-tr from-amber-400/20 via-transparent to-sky-400/25" />
        <div className="relative flex h-full flex-col justify-between px-14 py-16">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.2em] text-amber-400">OpsPilot</p>
            <h1 className="mt-8 max-w-xl text-5xl font-semibold leading-tight text-foreground">Create your tenant workspace in minutes.</h1>
            <p className="mt-5 max-w-lg text-base text-muted">
              Onboard your team, upload operational knowledge, and start assisting customers with AI-backed responses.
            </p>
          </div>
          <div className="rounded-2xl border border-border bg-surface/80 p-6 backdrop-blur">
            <p className="text-sm text-muted">Registration creates your tenant admin account and issues an authenticated session.</p>
          </div>
        </div>
      </section>

      <section className="flex items-center justify-center px-4 py-10 md:px-8">
        <div className="w-full max-w-md rounded-2xl border border-border bg-surface p-7 shadow-soft md:p-8">
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted">Set up account</p>
          <h2 className="mt-2 text-3xl font-semibold text-foreground">Register tenant</h2>
          <p className="mt-2 text-sm text-muted">Create the first admin identity for your organization.</p>

          <form className="mt-7 space-y-4" onSubmit={onSubmit}>
            <div>
              <label className="app-label" htmlFor="tenantName">
                Tenant Name
              </label>
              <input
                id="tenantName"
                value={tenantName}
                onChange={(e) => setTenantName(e.target.value)}
                className="app-input"
                required
              />
            </div>

            <div>
              <label className="app-label" htmlFor="adminName">
                Admin Name
              </label>
              <input
                id="adminName"
                value={adminName}
                onChange={(e) => setAdminName(e.target.value)}
                className="app-input"
                required
              />
            </div>

            <div>
              <label className="app-label" htmlFor="email">
                Admin Email
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="app-input"
                required
              />
            </div>

            <div>
              <label className="app-label" htmlFor="password">
                Password
              </label>
              <input
                id="password"
                type="password"
                minLength={8}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="app-input"
                required
              />
            </div>

            {error ? <p className="rounded-lg border border-rose-400 bg-rose-500 px-3 py-2 text-sm text-white">{error}</p> : null}

            <Button type="submit" disabled={isSubmitting} className="w-full">
              {isSubmitting ? "Creating account..." : "Create account"}
            </Button>
          </form>

          <p className="mt-5 text-sm text-muted">
            Already have an account?{" "}
            <Link to="/login" className="font-semibold text-amber-400 hover:text-amber-300">
              Sign in
            </Link>
          </p>
        </div>
      </section>
    </div>
  );
}
