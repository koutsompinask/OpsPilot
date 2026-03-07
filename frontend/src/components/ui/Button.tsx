import { ButtonHTMLAttributes, ReactNode } from "react";
import { cn } from "../../lib/cn";

type ButtonVariant = "primary" | "ghost" | "destructive" | "success" | "warning";
type ButtonSize = "sm" | "md" | "lg";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
  leftIcon?: ReactNode;
};

const variantClasses: Record<ButtonVariant, string> = {
  primary:
    "bg-amber-500 text-slate-950 shadow-soft hover:bg-amber-400 focus-visible:ring-amber-300 disabled:bg-amber-700",
  ghost:
    "bg-sky-500 text-white border border-sky-400 hover:bg-sky-400 focus-visible:ring-sky-300",
  destructive:
    "bg-rose-500 text-white shadow-soft hover:bg-rose-400 focus-visible:ring-rose-300 disabled:bg-rose-700",
  success:
    "bg-emerald-500 text-white shadow-soft hover:bg-emerald-400 focus-visible:ring-emerald-300 disabled:bg-emerald-700",
  warning:
    "bg-amber-500 text-slate-950 shadow-soft hover:bg-amber-400 focus-visible:ring-amber-300 disabled:bg-amber-700",
};

const sizeClasses: Record<ButtonSize, string> = {
  sm: "h-9 px-3 text-xs",
  md: "h-10 px-4 text-sm",
  lg: "h-11 px-5 text-sm",
};

export function Button({
  variant = "primary",
  size = "md",
  className,
  leftIcon,
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-all duration-150 ease-out",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950",
        "disabled:cursor-not-allowed disabled:opacity-70",
        variantClasses[variant],
        sizeClasses[size],
        className,
      )}
      {...props}
    >
      {leftIcon ? <span className="inline-flex h-4 w-4 items-center justify-center">{leftIcon}</span> : null}
      <span>{children}</span>
    </button>
  );
}
