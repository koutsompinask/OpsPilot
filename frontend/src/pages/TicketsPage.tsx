import { useCallback, useDeferredValue, useEffect, useMemo, useState } from "react";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { EmptyState } from "../components/ui/EmptyState";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { PageHeader } from "../components/ui/PageHeader";
import { Panel } from "../components/ui/Panel";
import { cn } from "../lib/cn";
import { getAuthClaims } from "../lib/auth";
import { listTickets, updateTicketStatus, type TicketOrigin, type TicketResponse, type TicketStatus } from "../lib/api";

type TicketFilter = "ALL" | TicketStatus;

const filterOptions: Array<{ value: TicketFilter; label: string }> = [
  { value: "ALL", label: "All" },
  { value: "OPEN", label: "Open" },
  { value: "IN_PROGRESS", label: "In progress" },
  { value: "RESOLVED", label: "Resolved" },
];

const statusActionOrder: TicketStatus[] = ["OPEN", "IN_PROGRESS", "RESOLVED"];

function formatTimestamp(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function formatRelativeAge(value: string): string {
  const createdAt = new Date(value).getTime();
  if (Number.isNaN(createdAt)) {
    return value;
  }

  const diffMs = Date.now() - createdAt;
  const diffMinutes = Math.max(1, Math.floor(diffMs / 60000));
  if (diffMinutes < 60) {
    return `${diffMinutes}m open`;
  }

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) {
    return `${diffHours}h open`;
  }

  const diffDays = Math.floor(diffHours / 24);
  return `${diffDays}d open`;
}

function statusBadgeVariant(status: TicketStatus): "success" | "warning" | "info" {
  if (status === "RESOLVED") {
    return "success";
  }
  if (status === "IN_PROGRESS") {
    return "info";
  }
  return "warning";
}

function originBadgeVariant(origin: TicketOrigin): "neutral" | "info" {
  return origin === "CHAT_LOW_CONFIDENCE" ? "info" : "neutral";
}

function confidenceVariant(confidence: number | null): "success" | "warning" | "error" | "neutral" {
  if (confidence === null) {
    return "neutral";
  }
  if (confidence >= 0.75) {
    return "success";
  }
  if (confidence >= 0.55) {
    return "warning";
  }
  return "error";
}

function confidenceLabel(confidence: number | null): string {
  if (confidence === null) {
    return "No confidence score";
  }
  return `Confidence ${confidence.toFixed(3)}`;
}

function statusTone(status: TicketStatus): string {
  if (status === "RESOLVED") {
    return "from-emerald-500/18 via-emerald-500/10 to-transparent border-emerald-400/40";
  }
  if (status === "IN_PROGRESS") {
    return "from-sky-500/18 via-sky-500/10 to-transparent border-sky-400/40";
  }
  return "from-amber-500/18 via-amber-500/10 to-transparent border-amber-400/40";
}

function statusCopy(status: TicketStatus): string {
  if (status === "RESOLVED") {
    return "Closed out and ready for audit trail review.";
  }
  if (status === "IN_PROGRESS") {
    return "Actively being reviewed by staff.";
  }
  return "Awaiting staff triage or assignment.";
}

function originLabel(origin: TicketOrigin): string {
  return origin === "CHAT_LOW_CONFIDENCE" ? "AI fallback" : "Manual";
}

export function TicketsPage() {
  const claims = getAuthClaims();
  const isAdmin = claims?.role === "TENANT_ADMIN";

  const [tickets, setTickets] = useState<TicketResponse[]>([]);
  const [selectedTicketId, setSelectedTicketId] = useState<string | null>(null);
  const [filter, setFilter] = useState<TicketFilter>("ALL");
  const [search, setSearch] = useState("");
  const deferredSearch = useDeferredValue(search);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [statusError, setStatusError] = useState<string | null>(null);
  const [statusSuccess, setStatusSuccess] = useState<string | null>(null);
  const [updatingStatus, setUpdatingStatus] = useState<TicketStatus | null>(null);

  const loadTickets = useCallback(async (mode: "initial" | "refresh" = "initial") => {
    setError(null);
    if (mode === "initial") {
      setIsLoading(true);
    } else {
      setIsRefreshing(true);
    }

    try {
      const data = await listTickets();
      setTickets(data);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unable to load tickets.";
      setError(message);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, []);

  useEffect(() => {
    void loadTickets();
  }, [loadTickets]);

  const ticketSummary = useMemo(() => {
    const openTickets = tickets.filter((ticket) => ticket.status === "OPEN");
    const inProgressTickets = tickets.filter((ticket) => ticket.status === "IN_PROGRESS");
    const resolvedTickets = tickets.filter((ticket) => ticket.status === "RESOLVED");
    const oldestOpenTicket = [...openTickets].sort((left, right) => {
      return new Date(left.createdAt).getTime() - new Date(right.createdAt).getTime();
    })[0] ?? null;

    return {
      total: tickets.length,
      open: openTickets.length,
      inProgress: inProgressTickets.length,
      resolved: resolvedTickets.length,
      oldestOpenTicket,
    };
  }, [tickets]);

  const filteredTickets = useMemo(() => {
    const normalizedSearch = deferredSearch.trim().toLowerCase();

    return tickets.filter((ticket) => {
      const matchesFilter = filter === "ALL" ? true : ticket.status === filter;
      if (!matchesFilter) {
        return false;
      }

      if (!normalizedSearch) {
        return true;
      }

      const haystack = [ticket.question, ticket.createdByEmail, ticket.notes ?? "", ticket.answer ?? ""]
        .join(" ")
        .toLowerCase();
      return haystack.includes(normalizedSearch);
    });
  }, [deferredSearch, filter, tickets]);

  useEffect(() => {
    if (filteredTickets.length === 0) {
      setSelectedTicketId(null);
      return;
    }

    const stillVisible = filteredTickets.some((ticket) => ticket.id === selectedTicketId);
    if (!stillVisible) {
      setSelectedTicketId(filteredTickets[0].id);
    }
  }, [filteredTickets, selectedTicketId]);

  const selectedTicket = useMemo(() => {
    if (!selectedTicketId) {
      return null;
    }
    return filteredTickets.find((ticket) => ticket.id === selectedTicketId) ?? null;
  }, [filteredTickets, selectedTicketId]);

  async function handleStatusChange(nextStatus: TicketStatus) {
    if (!selectedTicket || selectedTicket.status === nextStatus || !isAdmin) {
      return;
    }

    setStatusError(null);
    setStatusSuccess(null);
    setUpdatingStatus(nextStatus);

    const previousTickets = tickets;
    const optimisticTickets = tickets.map((ticket) =>
      ticket.id === selectedTicket.id
        ? {
            ...ticket,
            status: nextStatus,
            updatedAt: new Date().toISOString(),
          }
        : ticket,
    );

    setTickets(optimisticTickets);

    try {
      const updated = await updateTicketStatus(selectedTicket.id, nextStatus);
      setTickets((current) => current.map((ticket) => (ticket.id === updated.id ? updated : ticket)));
      setStatusSuccess(`Ticket marked ${nextStatus.replace("_", " ").toLowerCase()}.`);
    } catch (err) {
      setTickets(previousTickets);
      const message = err instanceof Error ? err.message : "Unable to update ticket status.";
      setStatusError(message);
    } finally {
      setUpdatingStatus(null);
    }
  }

  return (
    <section className="tickets-page">
      <PageHeader
        title="Support radar"
        breadcrumb="Insights"
        description="Track low-confidence AI escalations, review operator context, and close the loop on tenant support work."
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant={isAdmin ? "info" : "neutral"}>{isAdmin ? "Admin controls" : "Read only"}</Badge>
            <Button variant="ghost" onClick={() => void loadTickets("refresh")} disabled={isRefreshing}>
              {isRefreshing ? "Refreshing..." : "Refresh"}
            </Button>
          </div>
        }
      />

      {error ? <ErrorState message={error} onRetry={() => void loadTickets()} /> : null}

      <Panel className="tickets-radar overflow-hidden p-0">
        <div className="tickets-radar__glow" aria-hidden />
        <div className="relative grid gap-6 p-6 lg:grid-cols-[1.35fr_0.95fr] lg:p-8">
          <div className="space-y-5">
            <div className="space-y-3">
              <Badge variant="warning" className="tickets-eyebrow">
                Support desk visibility
              </Badge>
              <div className="max-w-3xl space-y-3">
                <h2 className="text-3xl font-semibold tracking-[-0.04em] text-foreground md:text-5xl">
                  See every low-confidence answer before it turns into silent support debt.
                </h2>
                <p className="max-w-2xl text-sm leading-6 text-slate-300 md:text-base">
                  This workspace turns backend ticket creation into an operator-facing queue with triage status,
                  answer context, and ownership cues that are visible the moment chat confidence drops.
                </p>
              </div>
            </div>

            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              <div className="tickets-stat tickets-stat--sky">
                <p className="tickets-stat__label">Total tickets</p>
                <p className="tickets-stat__value">{ticketSummary.total}</p>
                <p className="tickets-stat__copy">All tenant support events in view</p>
              </div>
              <div className="tickets-stat tickets-stat--amber">
                <p className="tickets-stat__label">Open</p>
                <p className="tickets-stat__value">{ticketSummary.open}</p>
                <p className="tickets-stat__copy">Waiting for first staff touch</p>
              </div>
              <div className="tickets-stat tickets-stat--sky-soft">
                <p className="tickets-stat__label">In progress</p>
                <p className="tickets-stat__value">{ticketSummary.inProgress}</p>
                <p className="tickets-stat__copy">Actively under review</p>
              </div>
              <div className="tickets-stat tickets-stat--emerald">
                <p className="tickets-stat__label">Resolved</p>
                <p className="tickets-stat__value">{ticketSummary.resolved}</p>
                <p className="tickets-stat__copy">Closed with visible audit trail</p>
              </div>
            </div>
          </div>

          <div className="tickets-briefing">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-amber-300">Current pulse</p>
              <h3 className="mt-3 text-xl font-semibold text-foreground">Triage briefing</h3>
            </div>
            <div className="space-y-4">
              <div className="rounded-2xl border border-white/10 bg-slate-950/35 p-4">
                <p className="text-xs uppercase tracking-[0.16em] text-slate-400">Oldest open ticket</p>
                {ticketSummary.oldestOpenTicket ? (
                  <>
                    <p className="mt-3 text-sm font-medium leading-6 text-foreground">
                      {ticketSummary.oldestOpenTicket.question}
                    </p>
                    <div className="mt-3 flex flex-wrap gap-2">
                      <Badge variant="warning">{formatRelativeAge(ticketSummary.oldestOpenTicket.createdAt)}</Badge>
                      <Badge variant={originBadgeVariant(ticketSummary.oldestOpenTicket.origin)}>
                        {originLabel(ticketSummary.oldestOpenTicket.origin)}
                      </Badge>
                    </div>
                  </>
                ) : (
                  <p className="mt-3 text-sm text-slate-300">No open tickets. The queue is clear.</p>
                )}
              </div>

              <div className="grid gap-3 sm:grid-cols-2">
                <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                  <p className="text-xs uppercase tracking-[0.16em] text-slate-400">Role mode</p>
                  <p className="mt-2 text-sm font-medium text-foreground">{isAdmin ? "Admin workflow" : "Member review"}</p>
                  <p className="mt-1 text-xs leading-5 text-slate-300">
                    {isAdmin
                      ? "Status controls are live for this session."
                      : "You can inspect queue details, but only admins can change status."}
                  </p>
                </div>
                <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                  <p className="text-xs uppercase tracking-[0.16em] text-slate-400">Coverage signal</p>
                  <p className="mt-2 text-sm font-medium text-foreground">
                    {ticketSummary.open > 0 ? "Low-confidence answers need attention" : "Knowledge coverage is stable"}
                  </p>
                  <p className="mt-1 text-xs leading-5 text-slate-300">
                    Review answer context and citations before staff follows up externally.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Panel>

      <div className="mt-6 grid gap-6 xl:grid-cols-[0.96fr_1.04fr]">
        <Panel className="p-0">
          <div className="border-b border-border px-5 py-5">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-muted">Queue</p>
                <h3 className="mt-2 text-2xl font-semibold tracking-tight text-foreground">Ticket stream</h3>
                <p className="mt-2 max-w-xl text-sm text-muted">
                  Newest tickets stay first. Filter by workflow stage or search by question, operator email, notes, or answer text.
                </p>
              </div>
              <div className="w-full max-w-xs">
                <label htmlFor="ticket-search" className="app-label">
                  Search queue
                </label>
                <input
                  id="ticket-search"
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  className="app-input"
                  placeholder="Search question, email, notes, answer..."
                />
              </div>
            </div>

            <div className="mt-4 flex flex-wrap gap-2">
              {filterOptions.map((option) => (
                <button
                  key={option.value}
                  type="button"
                  onClick={() => setFilter(option.value)}
                  className={cn(
                    "rounded-full border px-3 py-1.5 text-xs font-semibold uppercase tracking-[0.16em] transition-all duration-150",
                    filter === option.value
                      ? "border-amber-400 bg-amber-500 text-slate-950"
                      : "border-border bg-surface text-muted hover:border-sky-400 hover:text-foreground",
                  )}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>

          <div className="max-h-[52rem] overflow-y-auto px-3 py-3">
            {isLoading ? <LoadingState label="Loading tickets..." /> : null}

            {!isLoading && tickets.length === 0 ? (
              <EmptyState
                title="No tickets yet"
                description="Low-confidence chat questions will appear here once the assistant escalates them."
              />
            ) : null}

            {!isLoading && tickets.length > 0 && filteredTickets.length === 0 ? (
              <EmptyState
                title="No matches found"
                description="Try a different search term or switch to another status filter."
              />
            ) : null}

            {!isLoading && filteredTickets.length > 0 ? (
              <div className="space-y-3">
                {filteredTickets.map((ticket, index) => {
                  const isSelected = ticket.id === selectedTicketId;
                  return (
                    <button
                      key={ticket.id}
                      type="button"
                      onClick={() => setSelectedTicketId(ticket.id)}
                      className={cn(
                        "tickets-row w-full rounded-[1.35rem] border bg-surface p-4 text-left shadow-soft transition-all duration-200",
                        `motion-safe:animate-sidebar-in`,
                        isSelected
                          ? "border-amber-400 bg-slate-900/90 shadow-[0_24px_60px_rgba(15,23,42,0.35)]"
                          : "border-border hover:border-sky-400/70 hover:bg-surface-elevated",
                      )}
                      style={{ animationDelay: `${index * 45}ms` }}
                    >
                      <div className="flex flex-wrap items-start justify-between gap-3">
                        <div className="space-y-3">
                          <div className="flex flex-wrap gap-2">
                            <Badge variant={statusBadgeVariant(ticket.status)}>{ticket.status.replace("_", " ")}</Badge>
                            <Badge variant={originBadgeVariant(ticket.origin)}>{originLabel(ticket.origin)}</Badge>
                            {ticket.confidence !== null ? (
                              <Badge variant={confidenceVariant(ticket.confidence)}>{confidenceLabel(ticket.confidence)}</Badge>
                            ) : null}
                          </div>
                          <div>
                            <p className="text-sm font-semibold leading-6 text-foreground">{ticket.question}</p>
                            <p className="mt-2 text-xs text-muted">
                              {ticket.createdByEmail} · {formatTimestamp(ticket.createdAt)}
                            </p>
                          </div>
                        </div>
                        <div className="min-w-fit text-right">
                          <p className="font-mono text-[11px] uppercase tracking-[0.16em] text-slate-400">
                            {formatRelativeAge(ticket.createdAt)}
                          </p>
                          <p className="mt-2 text-xs text-muted">{ticket.sourceCount} source refs</p>
                        </div>
                      </div>

                      <p className="mt-4 line-clamp-2 text-sm leading-6 text-slate-300">
                        {ticket.answer ?? "No answer body was stored with this ticket."}
                      </p>
                    </button>
                  );
                })}
              </div>
            ) : null}
          </div>
        </Panel>

        <Panel className="overflow-hidden p-0">
          <div className="border-b border-border px-5 py-5">
            <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-muted">Detail</p>
            <h3 className="mt-2 text-2xl font-semibold tracking-tight text-foreground">Case context</h3>
            <p className="mt-2 text-sm text-muted">
              Review the original question, generated answer, and escalation context before moving the ticket.
            </p>
          </div>

          <div className="p-5">
            {!selectedTicket ? (
              <EmptyState
                title={tickets.length === 0 ? "No active case selected" : "Select a ticket"}
                description={
                  tickets.length === 0
                    ? "Once a ticket exists, the full answer context and status controls will appear here."
                    : "Choose a ticket from the queue to inspect answer context and workflow state."
                }
              />
            ) : (
              <div className="space-y-5">
                <div className={cn("rounded-[1.5rem] border bg-gradient-to-br p-5", statusTone(selectedTicket.status))}>
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="space-y-3">
                      <div className="flex flex-wrap gap-2">
                        <Badge variant={statusBadgeVariant(selectedTicket.status)}>
                          {selectedTicket.status.replace("_", " ")}
                        </Badge>
                        <Badge variant={originBadgeVariant(selectedTicket.origin)}>{originLabel(selectedTicket.origin)}</Badge>
                        <Badge variant={confidenceVariant(selectedTicket.confidence)}>
                          {confidenceLabel(selectedTicket.confidence)}
                        </Badge>
                      </div>
                      <h4 className="max-w-3xl text-2xl font-semibold leading-tight text-foreground">
                        {selectedTicket.question}
                      </h4>
                    </div>
                    <div className="rounded-2xl border border-white/10 bg-slate-950/35 px-4 py-3">
                      <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">Status note</p>
                      <p className="mt-2 text-sm text-foreground">{statusCopy(selectedTicket.status)}</p>
                    </div>
                  </div>
                </div>

                <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                  <div className="rounded-2xl border border-border bg-surface p-4">
                    <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted">Created by</p>
                    <p className="mt-2 text-sm font-medium text-foreground">{selectedTicket.createdByEmail}</p>
                  </div>
                  <div className="rounded-2xl border border-border bg-surface p-4">
                    <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted">Created</p>
                    <p className="mt-2 text-sm font-medium text-foreground">{formatTimestamp(selectedTicket.createdAt)}</p>
                  </div>
                  <div className="rounded-2xl border border-border bg-surface p-4">
                    <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted">Updated</p>
                    <p className="mt-2 text-sm font-medium text-foreground">{formatTimestamp(selectedTicket.updatedAt)}</p>
                  </div>
                  <div className="rounded-2xl border border-border bg-surface p-4">
                    <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-muted">Source count</p>
                    <p className="mt-2 text-sm font-medium text-foreground">{selectedTicket.sourceCount}</p>
                  </div>
                </div>

                <div className="space-y-3 rounded-2xl border border-border bg-surface p-5">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-muted">Answer context</p>
                      <h4 className="mt-2 text-lg font-semibold text-foreground">Generated answer</h4>
                    </div>
                    {selectedTicket.confidence !== null ? (
                      <div className="min-w-[10rem]">
                        <div className="h-2 overflow-hidden rounded-full bg-slate-800">
                          <div
                            className={cn(
                              "h-full rounded-full",
                              selectedTicket.confidence >= 0.75
                                ? "bg-gradient-to-r from-emerald-400 to-emerald-300"
                                : selectedTicket.confidence >= 0.55
                                  ? "bg-gradient-to-r from-amber-400 to-amber-300"
                                  : "bg-gradient-to-r from-rose-400 to-amber-300",
                            )}
                            style={{ width: `${Math.max(6, Math.min(100, selectedTicket.confidence * 100))}%` }}
                          />
                        </div>
                      </div>
                    ) : null}
                  </div>

                  <div className="rounded-2xl border border-white/10 bg-slate-950/35 p-4">
                    <p className="whitespace-pre-wrap text-sm leading-7 text-slate-200">
                      {selectedTicket.answer ?? "No answer body was stored with this ticket."}
                    </p>
                  </div>
                </div>

                {selectedTicket.notes ? (
                  <div className="rounded-2xl border border-border bg-surface p-5">
                    <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-muted">Notes</p>
                    <p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-slate-300">{selectedTicket.notes}</p>
                  </div>
                ) : null}

                <div className="rounded-2xl border border-border bg-surface p-5">
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                      <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-muted">Workflow</p>
                      <h4 className="mt-2 text-lg font-semibold text-foreground">Status controls</h4>
                      <p className="mt-2 max-w-2xl text-sm text-muted">
                        {isAdmin
                          ? "Update the ticket lifecycle directly from this workspace. Changes sync with the backend immediately."
                          : "This tenant role can review status history and answer context, but only admins can change lifecycle state."}
                      </p>
                    </div>
                    {isAdmin ? (
                      <div className="flex flex-wrap gap-2">
                        {statusActionOrder.map((status) => (
                          <Button
                            key={status}
                            type="button"
                            variant={selectedTicket.status === status ? "primary" : "ghost"}
                            disabled={updatingStatus !== null}
                            onClick={() => void handleStatusChange(status)}
                          >
                            {updatingStatus === status ? "Updating..." : status.replace("_", " ")}
                          </Button>
                        ))}
                      </div>
                    ) : null}
                  </div>

                  {statusError ? (
                    <p className="mt-4 rounded-xl border border-rose-400 bg-rose-500 px-3 py-2 text-sm text-white">
                      {statusError}
                    </p>
                  ) : null}
                  {statusSuccess ? (
                    <p className="mt-4 rounded-xl border border-emerald-400 bg-emerald-500 px-3 py-2 text-sm text-white">
                      {statusSuccess}
                    </p>
                  ) : null}
                </div>
              </div>
            )}
          </div>
        </Panel>
      </div>
    </section>
  );
}
