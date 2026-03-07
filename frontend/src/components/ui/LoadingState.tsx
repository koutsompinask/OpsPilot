type LoadingStateProps = {
  label?: string;
};

export function LoadingState({ label = "Loading..." }: LoadingStateProps) {
  return (
    <div className="flex items-center gap-3 rounded-xl border border-border bg-surface p-4 text-sm text-muted">
      <span className="inline-block h-2.5 w-2.5 animate-pulse rounded-full bg-accent" aria-hidden />
      <span>{label}</span>
    </div>
  );
}
