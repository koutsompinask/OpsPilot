import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { Badge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { EmptyState } from "../components/ui/EmptyState";
import { ErrorState } from "../components/ui/ErrorState";
import { LoadingState } from "../components/ui/LoadingState";
import { PageHeader } from "../components/ui/PageHeader";
import { Panel } from "../components/ui/Panel";
import { Sheet } from "../components/ui/Sheet";
import { deleteDocument, getDocument, listDocuments, uploadDocument, type DocumentResponse, type DocumentStatus } from "../lib/api";
import { getAuthClaims } from "../lib/auth";

function formatTimestamp(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function toStatusVariant(status: DocumentStatus): "success" | "warning" | "error" {
  if (status === "READY") {
    return "success";
  }
  if (status === "FAILED") {
    return "error";
  }
  return "warning";
}

export function DocumentsPage() {
  const claims = getAuthClaims();
  const isAdmin = claims?.role === "TENANT_ADMIN";
  const [documents, setDocuments] = useState<DocumentResponse[]>([]);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [selectedDetails, setSelectedDetails] = useState<DocumentResponse | null>(null);
  const [detailsOpen, setDetailsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isUploading, setIsUploading] = useState(false);
  const [isLoadingDetails, setIsLoadingDetails] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const loadDocuments = useCallback(async () => {
    setError(null);
    setIsLoading(true);

    try {
      const data = await listDocuments();
      setDocuments(data);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unable to load documents.";
      setError(message);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadDocuments();
  }, [loadDocuments]);

  const statusSummary = useMemo(() => {
    return {
      total: documents.length,
      ready: documents.filter((doc) => doc.status === "READY").length,
      processing: documents.filter((doc) => doc.status === "PROCESSING").length,
      failed: documents.filter((doc) => doc.status === "FAILED").length,
    };
  }, [documents]);

  async function onUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedFile) {
      setError("Select a .txt or .md file first.");
      return;
    }

    setError(null);
    setSuccess(null);
    setIsUploading(true);

    try {
      const created = await uploadDocument(selectedFile);
      setSelectedFile(null);
      setSuccess(`Upload accepted for ${created.filename}. Status: ${created.status}.`);
      await loadDocuments();
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unable to upload document.";
      setError(message);
    } finally {
      setIsUploading(false);
    }
  }

  async function onDelete(documentId: string) {
    const confirmed = window.confirm("Delete this document permanently?");
    if (!confirmed) {
      return;
    }

    setError(null);
    setSuccess(null);
    setDeletingId(documentId);

    try {
      await deleteDocument(documentId);
      if (selectedDetails?.id === documentId) {
        setDetailsOpen(false);
        setSelectedDetails(null);
      }
      setSuccess("Document deleted.");
      await loadDocuments();
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unable to delete document.";
      setError(message);
    } finally {
      setDeletingId(null);
    }
  }

  async function openDetails(documentId: string) {
    setError(null);
    setIsLoadingDetails(true);
    setDetailsOpen(true);

    try {
      const details = await getDocument(documentId);
      setSelectedDetails(details);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unable to load document details.";
      setError(message);
    } finally {
      setIsLoadingDetails(false);
    }
  }

  return (
    <section>
      <PageHeader
        title="Documents"
        breadcrumb="Manage"
        description="Upload tenant knowledge files and monitor ingestion status for AI retrieval."
        actions={
          <>
            <Button variant="ghost" onClick={() => void loadDocuments()}>
              Refresh
            </Button>
            {isAdmin ? <Button onClick={() => document.getElementById("document-file")?.click()}>Upload document</Button> : null}
          </>
        }
      />

      {error ? <ErrorState message={error} onRetry={() => void loadDocuments()} /> : null}
      {success ? <p className="mb-4 rounded-lg border border-emerald-400 bg-emerald-500 px-3 py-2 text-sm text-white">{success}</p> : null}

      <div className="grid gap-4 md:grid-cols-4">
        <Panel className="border-sky-400 bg-sky-500 p-4">
          <p className="text-xs uppercase tracking-[0.16em] text-muted">Total</p>
          <p className="mt-2 text-2xl font-semibold text-foreground">{statusSummary.total}</p>
        </Panel>
        <Panel className="border-emerald-400 bg-emerald-500 p-4">
          <p className="text-xs uppercase tracking-[0.16em] text-muted">Indexed</p>
          <p className="mt-2 text-2xl font-semibold text-foreground">{statusSummary.ready}</p>
        </Panel>
        <Panel className="border-amber-400 bg-amber-500 p-4">
          <p className="text-xs uppercase tracking-[0.16em] text-muted">Processing</p>
          <p className="mt-2 text-2xl font-semibold text-foreground">{statusSummary.processing}</p>
        </Panel>
        <Panel className="border-rose-400 bg-rose-500 p-4">
          <p className="text-xs uppercase tracking-[0.16em] text-muted">Failed</p>
          <p className="mt-2 text-2xl font-semibold text-foreground">{statusSummary.failed}</p>
        </Panel>
      </div>

      {isAdmin ? (
        <Panel className="mt-5">
          <h3 className="text-lg font-semibold text-foreground">Upload document</h3>
          <p className="mt-1 text-sm text-muted">Supported formats: `.txt`, `.md`.</p>
          <form className="mt-4" onSubmit={onUpload}>
            <label
              htmlFor="document-file"
              className="flex cursor-pointer flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-surface px-4 py-8 text-center transition-colors hover:border-amber-400"
            >
              <span className="text-sm font-medium text-foreground">Drop file here or click to browse</span>
              <span className="mt-1 text-xs text-muted">{selectedFile ? selectedFile.name : "No file selected"}</span>
            </label>
            <input
              id="document-file"
              type="file"
              accept=".txt,.md,text/plain,text/markdown"
              onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)}
              className="sr-only"
            />
            <div className="mt-4 flex justify-end">
              <Button type="submit" disabled={isUploading || !selectedFile}>
                {isUploading ? "Uploading..." : "Upload"}
              </Button>
            </div>
          </form>
        </Panel>
      ) : (
        <Panel className="mt-5">
          <p className="text-sm text-muted">Your role is read-only for documents. Tenant admins can upload and delete files.</p>
        </Panel>
      )}

      <Panel className="mt-5">
        <h3 className="text-lg font-semibold text-foreground">Document list</h3>

        {isLoading ? <LoadingState label="Loading documents..." /> : null}

        {!isLoading && documents.length === 0 ? (
          <EmptyState title="No documents uploaded" description="Upload your first document to enable grounded AI answers." />
        ) : null}

        {!isLoading && documents.length > 0 ? (
          <div className="mt-4 overflow-x-auto rounded-xl border border-border">
            <table className="min-w-full divide-y divide-border text-sm">
              <thead className="bg-surface-elevated text-left text-muted">
                <tr>
                  <th className="px-3 py-2.5 font-medium">Filename</th>
                  <th className="px-3 py-2.5 font-medium">Status</th>
                  <th className="px-3 py-2.5 font-medium">Chunks</th>
                  <th className="px-3 py-2.5 font-medium">Updated</th>
                  <th className="px-3 py-2.5 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/80 bg-surface">
                {documents.map((document) => (
                  <tr key={document.id}>
                    <td className="px-3 py-3 font-medium text-foreground">{document.filename}</td>
                    <td className="px-3 py-3">
                      <Badge variant={toStatusVariant(document.status)}>{document.status}</Badge>
                    </td>
                    <td className="px-3 py-3 text-muted">{document.chunkCount ?? "-"}</td>
                    <td className="px-3 py-3 text-muted">{formatTimestamp(document.updatedAt)}</td>
                    <td className="px-3 py-3">
                      <div className="flex flex-wrap gap-2">
                        <Button variant="ghost" size="sm" onClick={() => void openDetails(document.id)}>
                          View
                        </Button>
                        {isAdmin ? (
                          <Button
                            variant="destructive"
                            size="sm"
                            disabled={deletingId === document.id}
                            onClick={() => void onDelete(document.id)}
                          >
                            {deletingId === document.id ? "Deleting..." : "Delete"}
                          </Button>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </Panel>

      <Sheet
        open={detailsOpen}
        onClose={() => setDetailsOpen(false)}
        title="Document details"
        description="Metadata and ingestion status"
      >
        {isLoadingDetails ? <LoadingState label="Loading details..." /> : null}

        {!isLoadingDetails && !selectedDetails ? (
          <p className="text-sm text-muted">Select a document to view metadata.</p>
        ) : null}

        {!isLoadingDetails && selectedDetails ? (
          <dl className="grid gap-3 text-sm md:grid-cols-2">
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted">ID</dt>
              <dd className="mt-1 break-all font-mono text-xs text-foreground">{selectedDetails.id}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted">Filename</dt>
              <dd className="mt-1 text-foreground">{selectedDetails.filename}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted">Content type</dt>
              <dd className="mt-1 text-foreground">{selectedDetails.contentType}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted">Status</dt>
              <dd className="mt-1">
                <Badge variant={toStatusVariant(selectedDetails.status)}>{selectedDetails.status}</Badge>
              </dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted">Chunk count</dt>
              <dd className="mt-1 text-foreground">{selectedDetails.chunkCount ?? "-"}</dd>
            </div>
            <div>
              <dt className="text-xs uppercase tracking-wide text-muted">Updated</dt>
              <dd className="mt-1 text-foreground">{formatTimestamp(selectedDetails.updatedAt)}</dd>
            </div>
            <div className="md:col-span-2">
              <dt className="text-xs uppercase tracking-wide text-muted">Processing error</dt>
              <dd className="mt-1 text-foreground">{selectedDetails.errorMessage ?? "No processing errors"}</dd>
            </div>
          </dl>
        ) : null}
      </Sheet>
    </section>
  );
}
