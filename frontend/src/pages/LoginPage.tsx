import { FormEvent, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Button } from "../components/ui/Button";
import { TypingText } from "../components/ui/TypingText";
import { login } from "../lib/api";
import { saveTokens } from "../lib/auth";

type LocationState = {
  from?: {
    pathname?: string;
  };
};

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as LocationState | null;

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [badgeTypedDone, setBadgeTypedDone] = useState(false);
  const [titleTypedDone, setTitleTypedDone] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      const tokens = await login(email, password);
      saveTokens(tokens);
      navigate(state?.from?.pathname ?? "/dashboard", { replace: true });
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unable to login.";
      setError(message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="grid min-h-screen bg-bg lg:grid-cols-[1.1fr_0.9fr]">
      <section className="relative hidden overflow-hidden border-r border-border lg:block">
        <div className="absolute inset-0 bg-gradient-to-br from-amber-400/25 via-transparent to-sky-400/20" />
        <div className="relative flex h-full flex-col justify-between px-14 py-16">
          <div>
            <p className="landing-badge-enter text-sm font-semibold uppercase tracking-[0.2em] text-amber-400">
              <TypingText text="OpsPilot" speedMs={70} onComplete={() => setBadgeTypedDone(true)} />
            </p>
            <h1 className="landing-title-enter mt-8 max-w-xl text-5xl font-semibold leading-tight text-foreground">
              {badgeTypedDone ? (
                <TypingText
                  text="Support operations with clarity, speed, and confidence."
                  speedMs={22}
                  onComplete={() => setTitleTypedDone(true)}
                />
              ) : null}
            </h1>
            <p className="landing-copy-enter mt-5 max-w-lg text-base text-muted">
              {titleTypedDone ? (
                <TypingText
                  text="Manage your tenant, keep your knowledge base fresh, and answer customer questions with grounded AI."
                  speedMs={16}
                />
              ) : null}
            </p>
          </div>
          <div className="landing-panel-enter rounded-2xl border border-border bg-surface/80 p-6 backdrop-blur">
            <p className="text-sm text-muted">Designed for daily operator workflows across chat, documents, and team management.</p>
          </div>
        </div>
      </section>

      <section className="flex items-center justify-center px-4 py-10 md:px-8">
        <div className="w-full max-w-md rounded-2xl border border-border bg-surface p-7 shadow-soft md:p-8">
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted">Welcome back</p>
          <h2 className="mt-2 text-3xl font-semibold text-foreground">Sign in</h2>
          <p className="mt-2 text-sm text-muted">Use your tenant credentials to access the OpsPilot console.</p>

          <form className="mt-7 space-y-4" onSubmit={onSubmit}>
            <div>
              <label className="app-label" htmlFor="email">
                Email
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
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="app-input"
                required
              />
            </div>

            {error ? <p className="rounded-lg border border-rose-400 bg-rose-500 px-3 py-2 text-sm text-white">{error}</p> : null}

            <Button type="submit" disabled={isSubmitting} className="w-full">
              {isSubmitting ? "Signing in..." : "Sign in"}
            </Button>
          </form>

          <p className="mt-5 text-sm text-muted">
            New tenant admin?{" "}
            <Link to="/register" className="font-semibold text-amber-400 hover:text-amber-300">
              Create account
            </Link>
          </p>
        </div>
      </section>
    </div>
  );
}
