import { ReactNode } from "react";
import { cn } from "../../lib/cn";

type BadgeVariant = "success" | "warning" | "error" | "neutral" | "info";

type BadgeProps = {
  variant?: BadgeVariant;
  children: ReactNode;
  className?: string;
};

const variantClasses: Record<BadgeVariant, string> = {
  success: "bg-emerald-500 text-white border border-emerald-400",
  warning: "bg-amber-500 text-slate-950 border border-amber-400",
  error: "bg-rose-500 text-white border border-rose-400",
  neutral: "bg-slate-600 text-white border border-slate-500",
  info: "bg-sky-500 text-white border border-sky-400",
};

export function Badge({ variant = "neutral", children, className }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide",
        variantClasses[variant],
        className,
      )}
    >
      {children}
    </span>
  );
}
