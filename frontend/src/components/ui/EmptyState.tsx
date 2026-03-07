import { ReactNode } from "react";
import { Button } from "./Button";

type EmptyStateProps = {
  icon?: ReactNode;
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
};

export function EmptyState({ icon, title, description, actionLabel, onAction }: EmptyStateProps) {
  return (
    <div className="rounded-2xl border border-dashed border-border bg-surface p-10 text-center">
      {icon ? <div className="mx-auto mb-4 inline-flex h-12 w-12 items-center justify-center rounded-full bg-surface-elevated">{icon}</div> : null}
      <h3 className="text-lg font-semibold text-foreground">{title}</h3>
      <p className="mx-auto mt-2 max-w-md text-sm text-muted">{description}</p>
      {actionLabel && onAction ? (
        <div className="mt-5">
          <Button onClick={onAction}>{actionLabel}</Button>
        </div>
      ) : null}
    </div>
  );
}
