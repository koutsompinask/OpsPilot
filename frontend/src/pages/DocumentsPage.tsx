import { FormEvent, useCallback, useEffect, useState } from "react";
import { getAuthClaims } from "../lib/auth";
import {
  deleteDocument,
  getDocument,
  listDocuments,
  uploadDocument,
  type DocumentResponse,
  type DocumentStatus,
} from "../lib/api";

function formatTimestamp(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function statusClasses(status: DocumentStatus): string {
  if (status === "READY") {
    return "bg-green-100 text-green-800";
  }
  if (status === "FAILED") {
    return "bg-red-100 text-red-800";
  }
  return "bg-amber-100 text-amber-800";
}

export function DocumentsPage() {
  const claims = getAuthClaims();
  const isAdmin = claims?.role === "TENANT_ADMIN";
  const [documents, setDocuments] = useState<DocumentResponse[]>([]);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [selectedDetails, setSelectedDetails] = useState<DocumentResponse | null>(null);
  const [activeDetailsId, setActiveDetailsId] = useState<string | null>(null);
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
      setSuccess(`Upload accepted for ${created.filename}. Status is ${created.status}.`);
      await loadDocuments();
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unable to upload document.";
      setError(message);
    } finally {
      setIsUploading(false);
    }
  }

  async function onDelete(documentId: string) {
    setError(null);
    setSuccess(null);
    setDeletingId(documentId);

    try {
      await deleteDocument(documentId);
      if (activeDetailsId === documentId) {
        setActiveDetailsId(null);
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

  async function onViewDetails(documentId: string) {
    setError(null);
    setIsLoadingDetails(true);
    setActiveDetailsId(documentId);

    try {
      const details = await getDocument(documentId);
      setSelectedDetails(details);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unable to load document details.";
      setError(message);
      setSelectedDetails(null);
      setActiveDetailsId(null);
    } finally {
      setIsLoadingDetails(false);
    }
  }

  return (
    <section className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-semibold">Documents</h2>
          <p className="text-sm text-slate-600">Upload and manage knowledge documents for Phase 3 ingestion (`.txt` and `.md`).</p>
        </div>
        <button
          type="button"
          onClick={() => void loadDocuments()}
          className="rounded border border-slate-300 px-3 py-2 text-sm font-medium hover:bg-slate-100"
        >
          Refresh
        </button>
      </div>

      {!isAdmin ? (
        <p className="rounded border border-sky-200 bg-sky-50 px-3 py-2 text-sm text-sky-800">
          Your role is read-only for documents. Tenant admins can upload and delete.
        </p>
      ) : null}

      {isAdmin ? (
        <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <h3 className="text-lg font-semibold">Upload Document</h3>
          <form className="mt-4 flex flex-col gap-3 md:flex-row md:items-end" onSubmit={onUpload}>
            <div className="flex-1">
              <label className="mb-1 block text-sm font-medium text-slate-700" htmlFor="document-file">
                File
              </label>
              <input
                id="document-file"
                type="file"
                accept=".txt,.md,text/plain,text/markdown"
                onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)}
                className="w-full rounded border border-slate-300 px-3 py-2 text-sm"
                required
              />
            </div>
            <button
              type="submit"
              disabled={isUploading}
              className="inline-flex rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:opacity-60"
            >
              {isUploading ? "Uploading..." : "Upload"}
            </button>
          </form>
        </div>
      ) : null}

      {error ? <p className="rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p> : null}
      {success ? <p className="rounded border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-800">{success}</p> : null}

      <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h3 className="text-lg font-semibold">Document List</h3>

        {isLoading ? (
          <p className="mt-4 text-sm text-slate-600">Loading documents...</p>
        ) : documents.length === 0 ? (
          <p className="mt-4 text-sm text-slate-600">No documents uploaded yet.</p>
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-sm">
              <thead>
                <tr className="text-left text-slate-600">
                  <th className="px-3 py-2 font-medium">Filename</th>
                  <th className="px-3 py-2 font-medium">Status</th>
                  <th className="px-3 py-2 font-medium">Chunks</th>
                  <th className="px-3 py-2 font-medium">Updated</th>
                  <th className="px-3 py-2 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {documents.map((document) => (
                  <tr key={document.id}>
                    <td className="px-3 py-2">{document.filename}</td>
                    <td className="px-3 py-2">
                      <span className={`inline-flex rounded-full px-2 py-1 text-xs font-medium ${statusClasses(document.status)}`}>
                        {document.status}
                      </span>
                    </td>
                    <td className="px-3 py-2">{document.chunkCount ?? "-"}</td>
                    <td className="px-3 py-2">{formatTimestamp(document.updatedAt)}</td>
                    <td className="px-3 py-2">
                      <div className="flex flex-wrap gap-2">
                        <button
                          type="button"
                          onClick={() => void onViewDetails(document.id)}
                          className="rounded border border-slate-300 px-2 py-1 text-xs font-medium hover:bg-slate-100"
                        >
                          View details
                        </button>
                        {isAdmin ? (
                          <button
                            type="button"
                            disabled={deletingId === document.id}
                            onClick={() => void onDelete(document.id)}
                            className="rounded border border-red-300 px-2 py-1 text-xs font-medium text-red-700 hover:bg-red-50 disabled:opacity-60"
                          >
                            {deletingId === document.id ? "Deleting..." : "Delete"}
                          </button>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h3 className="text-lg font-semibold">Selected Document Details</h3>
        {isLoadingDetails ? (
          <p className="mt-3 text-sm text-slate-600">Loading details...</p>
        ) : !selectedDetails ? (
          <p className="mt-3 text-sm text-slate-600">Select a document row and click "View details".</p>
        ) : (
          <dl className="mt-4 grid gap-3 text-sm md:grid-cols-2">
            <div>
              <dt className="text-slate-600">ID</dt>
              <dd className="break-all text-xs text-slate-900">{selectedDetails.id}</dd>
            </div>
            <div>
              <dt className="text-slate-600">Filename</dt>
              <dd>{selectedDetails.filename}</dd>
            </div>
            <div>
              <dt className="text-slate-600">Content Type</dt>
              <dd>{selectedDetails.contentType}</dd>
            </div>
            <div>
              <dt className="text-slate-600">Status</dt>
              <dd>{selectedDetails.status}</dd>
            </div>
            <div>
              <dt className="text-slate-600">Chunk Count</dt>
              <dd>{selectedDetails.chunkCount ?? "-"}</dd>
            </div>
            <div>
              <dt className="text-slate-600">Created</dt>
              <dd>{formatTimestamp(selectedDetails.createdAt)}</dd>
            </div>
            <div>
              <dt className="text-slate-600">Updated</dt>
              <dd>{formatTimestamp(selectedDetails.updatedAt)}</dd>
            </div>
            <div>
              <dt className="text-slate-600">Error</dt>
              <dd>{selectedDetails.errorMessage ?? "-"}</dd>
            </div>
          </dl>
        )}
      </div>
    </section>
  );
}
