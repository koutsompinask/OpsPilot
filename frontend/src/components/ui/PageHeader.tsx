import { ReactNode } from "react";
import { cn } from "../../lib/cn";

type PageHeaderProps = {
  title: string;
  description?: string;
  breadcrumb?: string;
  actions?: ReactNode;
  className?: string;
};

export function PageHeader({ title, description, breadcrumb, actions, className }: PageHeaderProps) {
  return (
    <header className={cn("mb-6 flex flex-wrap items-start justify-between gap-4", className)}>
      <div className="space-y-1">
        {breadcrumb ? <p className="text-xs font-medium uppercase tracking-[0.18em] text-muted">{breadcrumb}</p> : null}
        <h1 className="text-2xl font-semibold tracking-tight text-foreground md:text-3xl">{title}</h1>
        {description ? <p className="max-w-2xl text-sm text-muted">{description}</p> : null}
      </div>
      {actions ? <div className="flex shrink-0 items-center gap-2">{actions}</div> : null}
    </header>
  );
}
