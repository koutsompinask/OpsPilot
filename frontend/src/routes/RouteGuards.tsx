import { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { clearTokens, isAccessTokenValid } from "../lib/auth";

type GuardProps = {
  children: ReactNode;
};

function hasValidSession(): boolean {
  const valid = isAccessTokenValid();
  if (!valid) {
    clearTokens();
  }
  return valid;
}

export function RequireAuth({ children }: GuardProps) {
  const location = useLocation();

  if (!hasValidSession()) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <>{children}</>;
}

export function PublicOnly({ children }: GuardProps) {
  if (hasValidSession()) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
}
