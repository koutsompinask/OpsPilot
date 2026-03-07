import { FormEvent, useState } from "react";
import { askChat, type ChatAskResponse } from "../lib/api";

export function ChatPage() {
  const [question, setQuestion] = useState("");
  const [topK, setTopK] = useState(4);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [response, setResponse] = useState<ChatAskResponse | null>(null);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!question.trim()) {
      setError("Question is required.");
      return;
    }

    setIsSubmitting(true);
    setError(null);
    setResponse(null);

    try {
      const data = await askChat({ question: question.trim(), topK });
      setResponse(data);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unable to ask chat question.";
      setError(message);
    } finally {
      setIsSubmitting(false);
    }
  }

  const lowConfidence = response !== null && response.confidence < 0.55;

  return (
    <section className="space-y-6">
      <div>
        <h2 className="text-2xl font-semibold">Chat Interface</h2>
        <p className="text-sm text-slate-600">Ask a question and get a grounded answer with source citations.</p>
      </div>

      <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <form className="space-y-4" onSubmit={onSubmit}>
          <div>
            <label htmlFor="question" className="mb-1 block text-sm font-medium text-slate-700">
              Question
            </label>
            <textarea
              id="question"
              value={question}
              onChange={(event) => setQuestion(event.target.value)}
              className="min-h-28 w-full rounded border border-slate-300 px-3 py-2 text-sm"
              placeholder="Example: What time is hotel check-in?"
              required
            />
          </div>

          <div>
            <label htmlFor="topk" className="mb-1 block text-sm font-medium text-slate-700">
              Top-K Chunks
            </label>
            <input
              id="topk"
              type="number"
              min={1}
              max={10}
              value={topK}
              onChange={(event) => setTopK(Number(event.target.value) || 1)}
              className="w-24 rounded border border-slate-300 px-3 py-2 text-sm"
            />
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="inline-flex rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60"
          >
            {isSubmitting ? "Asking..." : "Ask"}
          </button>
        </form>
      </div>

      {error ? <p className="rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p> : null}

      {response ? (
        <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          {lowConfidence ? (
            <p className="mb-4 rounded border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
              Confidence is low ({response.confidence.toFixed(3)}). Ticket creation is enabled in Phase 5.
            </p>
          ) : null}

          <h3 className="text-lg font-semibold">Answer</h3>
          <p className="mt-3 whitespace-pre-wrap text-sm text-slate-800">{response.answer}</p>

          <div className="mt-4 flex flex-wrap gap-4 text-sm">
            <p>
              <span className="text-slate-600">Confidence:</span> {response.confidence.toFixed(3)}
            </p>
            <p>
              <span className="text-slate-600">Ticket created:</span> {response.ticketCreated ? "Yes" : "No"}
            </p>
          </div>

          <div className="mt-5">
            <h4 className="text-sm font-semibold text-slate-700">Sources</h4>
            {response.sources.length === 0 ? (
              <p className="mt-2 text-sm text-slate-600">No sources returned.</p>
            ) : (
              <ul className="mt-2 space-y-2 text-sm">
                {response.sources.map((source, index) => (
                  <li key={`${source.document}-${source.chunkId}-${index}`} className="rounded border border-slate-200 px-3 py-2">
                    {source.document} · {source.chunkId}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      ) : null}
    </section>
  );
}
