import React from "react";
import ReactDOM from "react-dom/client";
import { createBrowserRouter, Navigate, RouterProvider } from "react-router-dom";
import "./index.css";
import { AppLayout } from "./layouts/AppLayout";
import { AnalyticsPage } from "./pages/AnalyticsPage";
import { ChatPage } from "./pages/ChatPage";
import { DashboardPage } from "./pages/DashboardPage";
import { DocumentsPage } from "./pages/DocumentsPage";
import { LoginPage } from "./pages/LoginPage";
import { TenantSettingsPage } from "./pages/TenantSettingsPage";
import { TicketsPage } from "./pages/TicketsPage";

const router = createBrowserRouter([
  { path: "/", element: <Navigate to="/login" replace /> },
  { path: "/login", element: <LoginPage /> },
  {
    path: "/",
    element: <AppLayout />,
    children: [
      { path: "/dashboard", element: <DashboardPage /> },
      { path: "/documents", element: <DocumentsPage /> },
      { path: "/chat", element: <ChatPage /> },
      { path: "/tickets", element: <TicketsPage /> },
      { path: "/analytics", element: <AnalyticsPage /> },
      { path: "/tenant-settings", element: <TenantSettingsPage /> },
    ],
  },
]);

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>,
);