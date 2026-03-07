import { Button } from "./Button";

type ErrorStateProps = {
  message: string;
  retryLabel?: string;
  onRetry?: () => void;
};

export function ErrorState({ message, retryLabel = "Try again", onRetry }: ErrorStateProps) {
  return (
    <div className="rounded-xl border border-rose-400 bg-rose-500 p-4 text-sm text-white">
      <p>{message}</p>
      {onRetry ? (
        <div className="mt-3">
          <Button variant="ghost" size="sm" onClick={onRetry}>
            {retryLabel}
          </Button>
        </div>
      ) : null}
    </div>
  );
}
