import { Link } from "react-router-dom";

export function LoginPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 px-4">
      <div className="w-full max-w-md rounded-lg border border-slate-200 bg-white p-8 shadow-sm">
        <h1 className="mb-2 text-2xl font-semibold">OpsPilot Login</h1>
        <p className="mb-6 text-sm text-slate-600">Phase 1 placeholder login screen.</p>
        <Link
          to="/dashboard"
          className="inline-flex rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700"
        >
          Continue to Dashboard
        </Link>
      </div>
    </div>
  );
}