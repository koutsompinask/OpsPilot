import { HTMLAttributes } from "react";
import { cn } from "../../lib/cn";

type PanelProps = HTMLAttributes<HTMLDivElement>;

export function Panel({ className, ...props }: PanelProps) {
  return (
    <section
      className={cn("rounded-2xl border border-border bg-surface-elevated p-6 shadow-soft card-enter", className)}
      {...props}
    />
  );
}
