import React from "react";
import ReactDOM from "react-dom/client";
import { createBrowserRouter, Navigate, RouterProvider } from "react-router-dom";
import "./index.css";
import { AppLayout } from "./layouts/AppLayout";
import { isAccessTokenValid } from "./lib/auth";
import { AnalyticsPage } from "./pages/AnalyticsPage";
import { ChatPage } from "./pages/ChatPage";
import { DashboardPage } from "./pages/DashboardPage";
import { DocumentsPage } from "./pages/DocumentsPage";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { TenantSettingsPage } from "./pages/TenantSettingsPage";
import { TenantUsersPage } from "./pages/TenantUsersPage";
import { TicketsPage } from "./pages/TicketsPage";
import { PublicOnly, RequireAuth } from "./routes/RouteGuards";

function HomeRedirect() {
  return <Navigate to={isAccessTokenValid() ? "/dashboard" : "/login"} replace />;
}

const router = createBrowserRouter([
  { path: "/", element: <HomeRedirect /> },
  {
    path: "/login",
    element: (
      <PublicOnly>
        <LoginPage />
      </PublicOnly>
    ),
  },
  {
    path: "/register",
    element: (
      <PublicOnly>
        <RegisterPage />
      </PublicOnly>
    ),
  },
  {
    path: "/",
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
    children: [
      { path: "/dashboard", element: <DashboardPage /> },
      { path: "/tenant-users", element: <TenantUsersPage /> },
      { path: "/tenant-settings", element: <TenantSettingsPage /> },
      { path: "/documents", element: <DocumentsPage /> },
      { path: "/chat", element: <ChatPage /> },
      { path: "/tickets", element: <TicketsPage /> },
      { path: "/analytics", element: <AnalyticsPage /> },
    ],
  },
]);

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>,
);
