import { ReactNode, useEffect } from "react";
import { cn } from "../../lib/cn";

type SheetProps = {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: ReactNode;
  widthClassName?: string;
};

export function Sheet({ open, onClose, title, description, children, widthClassName = "max-w-xl" }: SheetProps) {
  useEffect(() => {
    if (!open) {
      return;
    }

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
      }
    }

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [onClose, open]);

  if (!open) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-40 flex justify-end bg-black/45 backdrop-blur-sm">
      <button aria-label="Close panel" className="h-full flex-1 cursor-default" onClick={onClose} />
      <aside className={cn("h-full w-full overflow-y-auto border-l border-border bg-surface p-6 shadow-soft", widthClassName)}>
        <div className="mb-5 flex items-start justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold text-foreground">{title}</h2>
            {description ? <p className="mt-1 text-sm text-muted">{description}</p> : null}
          </div>
          <button
            type="button"
            aria-label="Close"
            className="rounded-md border border-border px-2 py-1 text-sm text-muted hover:bg-surface-elevated"
            onClick={onClose}
          >
            Close
          </button>
        </div>
        {children}
      </aside>
    </div>
  );
}
