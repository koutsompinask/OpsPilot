import { useEffect, useRef, useState } from "react";
import { cn } from "../../lib/cn";

type TypingTextProps = {
  text: string;
  className?: string;
  speedMs?: number;
  startDelayMs?: number;
  onComplete?: () => void;
};

export function TypingText({
  text,
  className,
  speedMs = 18,
  startDelayMs = 0,
  onComplete,
}: TypingTextProps) {
  const [visibleCount, setVisibleCount] = useState(0);
  const completeRef = useRef(false);
  const onCompleteRef = useRef(onComplete);

  useEffect(() => {
    onCompleteRef.current = onComplete;
  }, [onComplete]);

  useEffect(() => {
    completeRef.current = false;
    setVisibleCount(0);

    if (!text) {
      onCompleteRef.current?.();
      completeRef.current = true;
      return;
    }

    const shouldReduceMotion =
      typeof window !== "undefined" &&
      window.matchMedia &&
      window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    if (shouldReduceMotion) {
      setVisibleCount(text.length);
      onCompleteRef.current?.();
      completeRef.current = true;
      return;
    }

    let startTimer: number | undefined;
    let charTimer: number | undefined;

    const startTyping = () => {
      let count = 0;
      charTimer = window.setInterval(() => {
        count += 1;
        setVisibleCount(count);

        if (count >= text.length) {
          if (charTimer) {
            window.clearInterval(charTimer);
          }
          if (!completeRef.current) {
            completeRef.current = true;
            onCompleteRef.current?.();
          }
        }
      }, Math.max(8, speedMs));
    };

    startTimer = window.setTimeout(startTyping, Math.max(0, startDelayMs));

    return () => {
      if (startTimer) {
        window.clearTimeout(startTimer);
      }
      if (charTimer) {
        window.clearInterval(charTimer);
      }
    };
  }, [speedMs, startDelayMs, text]);

  return <span className={cn("whitespace-pre-wrap", className)}>{text.slice(0, visibleCount)}</span>;
}
