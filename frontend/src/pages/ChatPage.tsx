import { FormEvent, useMemo, useState } from "react";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { EmptyState } from "../components/ui/EmptyState";
import { ErrorState } from "../components/ui/ErrorState";
import { PageHeader } from "../components/ui/PageHeader";
import { Panel } from "../components/ui/Panel";
import { TypingText } from "../components/ui/TypingText";
import { askChat, type ChatAskResponse } from "../lib/api";

type ChatMessage =
  | {
      id: string;
      role: "user";
      text: string;
    }
  | {
      id: string;
      role: "assistant";
      payload: ChatAskResponse;
    };

function confidenceVariant(confidence: number): "success" | "warning" | "error" {
  if (confidence >= 0.75) {
    return "success";
  }
  if (confidence >= 0.55) {
    return "warning";
  }
  return "error";
}

const suggestions = [
  "What are our check-in and check-out policies?",
  "Summarize the refund policy for customers.",
  "What should support do when no source is found?",
];

export function ChatPage() {
  const [question, setQuestion] = useState("");
  const [topK, setTopK] = useState(4);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [typedAssistantIds, setTypedAssistantIds] = useState<Set<string>>(new Set());

  const hasMessages = messages.length > 0;

  async function submitQuestion(rawQuestion: string) {
    const trimmed = rawQuestion.trim();
    if (!trimmed) {
      setError("Question is required.");
      return;
    }

    setError(null);
    setIsSubmitting(true);

    const userMessage: ChatMessage = {
      id: `u-${Date.now()}`,
      role: "user",
      text: trimmed,
    };

    setMessages((current) => [...current, userMessage]);
    setQuestion("");

    try {
      const data = await askChat({ question: trimmed, topK });
      const assistantMessage: ChatMessage = {
        id: `a-${Date.now()}`,
        role: "assistant",
        payload: data,
      };
      setMessages((current) => [...current, assistantMessage]);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unable to ask chat question.";
      setError(message);
    } finally {
      setIsSubmitting(false);
    }
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await submitQuestion(question);
  }

  const content = useMemo(() => {
    if (!hasMessages) {
      return (
        <EmptyState
          title="Start a conversation"
          description="Ask policy or operational questions to validate your knowledge base coverage."
        />
      );
    }

    return (
      <div className="space-y-4">
        {messages.map((message) => {
          if (message.role === "user") {
            return (
              <div key={message.id} className="flex justify-end">
                <div className="max-w-[85%] rounded-2xl rounded-br-md bg-amber-500 px-4 py-3 text-sm text-slate-950 shadow-soft md:max-w-[70%]">
                  {message.text}
                </div>
              </div>
            );
          }

          const lowConfidence = message.payload.confidence < 0.55;
          return (
            <div key={message.id} className="flex justify-start">
              <div className="card-enter max-w-[95%] rounded-2xl rounded-bl-md border border-border bg-surface p-4 shadow-soft md:max-w-[80%]">
                <p className="chat-answer-enter text-sm leading-6 text-foreground">
                  {typedAssistantIds.has(message.id) ? (
                    <span className="whitespace-pre-wrap">{message.payload.answer}</span>
                  ) : (
                    <TypingText
                      text={message.payload.answer}
                      speedMs={14}
                      onComplete={() => {
                        setTypedAssistantIds((current) => {
                          if (current.has(message.id)) {
                            return current;
                          }
                          const next = new Set(current);
                          next.add(message.id);
                          return next;
                        });
                      }}
                    />
                  )}
                </p>

                <div className="mt-4 space-y-2">
                  <div className="flex items-center gap-2">
                    <Badge variant={confidenceVariant(message.payload.confidence)}>
                      Confidence {message.payload.confidence.toFixed(3)}
                    </Badge>
                    {message.payload.ticketCreated ? <Badge variant="info">Ticket created</Badge> : null}
                  </div>
                  <div className="h-2 overflow-hidden rounded-full bg-sky-700/40">
                    <div
                      className="h-full rounded-full bg-gradient-to-r from-sky-400 to-amber-400"
                      style={{ width: `${Math.max(6, Math.min(100, message.payload.confidence * 100))}%` }}
                    />
                  </div>
                </div>

                {lowConfidence ? (
                  <p className="mt-3 rounded-lg border border-amber-400 bg-amber-500 px-3 py-2 text-xs text-slate-950">
                    Confidence is low. Verify this answer before using it as a final support response.
                  </p>
                ) : null}

                <div className="mt-4">
                  <p className="text-xs font-semibold uppercase tracking-wide text-muted">Sources</p>
                  {message.payload.sources.length === 0 ? (
                    <p className="mt-2 text-xs text-muted">No sources returned.</p>
                  ) : (
                    <div className="mt-2 flex flex-wrap gap-2">
                      {message.payload.sources.map((source, index) => (
                        <span
                          key={`${source.document}-${source.chunkId}-${index}`}
                          className="rounded-full border border-border bg-surface-elevated px-2.5 py-1 text-xs text-muted"
                        >
                          {source.document} · {source.chunkId}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          );
        })}

        {isSubmitting ? (
          <div className="flex justify-start">
            <div className="rounded-2xl border border-border bg-surface px-4 py-3 text-sm text-muted">Assistant is thinking...</div>
          </div>
        ) : null}
      </div>
    );
  }, [hasMessages, isSubmitting, messages, typedAssistantIds]);

  return (
    <section className="flex h-[calc(100vh-8.5rem)] min-h-[34rem] flex-col">
      <PageHeader
        title="AI chat"
        breadcrumb="Core"
        description="Ask grounded questions and inspect confidence, source chunks, and ticket fallback behavior."
      />

      {error ? <ErrorState message={error} /> : null}

      {!hasMessages ? (
        <div className="mb-4 flex flex-wrap gap-2">
          {suggestions.map((suggestion) => (
            <button
              key={suggestion}
              type="button"
              onClick={() => {
                setQuestion(suggestion);
                setError(null);
              }}
              className="rounded-full border border-border bg-surface px-3 py-1.5 text-xs text-muted transition-colors hover:border-amber-400 hover:text-foreground"
            >
              {suggestion}
            </button>
          ))}
        </div>
      ) : null}

      <Panel className="flex min-h-0 flex-1 flex-col">
        <div className="min-h-0 flex-1 overflow-y-auto pr-1">{content}</div>

        <form className="mt-4 border-t border-border pt-4" onSubmit={onSubmit}>
          <div className="grid gap-3 md:grid-cols-[1fr_auto_auto] md:items-end">
            <div>
              <label htmlFor="question" className="app-label">
                Your question
              </label>
              <textarea
                id="question"
                value={question}
                onChange={(event) => setQuestion(event.target.value)}
                className="app-textarea min-h-24"
                placeholder="Ask a support or operations question..."
                required
              />
            </div>

            <div>
              <label htmlFor="topk" className="app-label">
                Top-K
              </label>
              <input
                id="topk"
                type="number"
                min={1}
                max={10}
                value={topK}
                onChange={(event) => setTopK(Number(event.target.value) || 1)}
                className="app-input w-24"
              />
            </div>

            <Button type="submit" disabled={isSubmitting} className="md:mb-0.5">
              {isSubmitting ? "Asking..." : "Send"}
            </Button>
          </div>
        </form>
      </Panel>
    </section>
  );
}
