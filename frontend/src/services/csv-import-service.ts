import { apiRequest } from "@/lib/api";
import type { CsvImportPreview, CsvImportResult } from "@/types/csv-import";

function upload(file: File, accountId: string, categoryId: string, previewHash?: string) {
  const body = new FormData();
  body.set("file", file);
  body.set("accountId", accountId);
  body.set("categoryId", categoryId);
  if (previewHash) body.set("previewHash", previewHash);
  return { method: "POST", body };
}

export function previewCsv(file: File, accountId: string, categoryId: string) {
  return apiRequest<CsvImportPreview>("/transactions/import/preview", upload(file, accountId, categoryId));
}

export function confirmCsv(file: File, accountId: string, categoryId: string, hash: string) {
  return apiRequest<CsvImportResult>("/transactions/import/confirm", upload(file, accountId, categoryId, hash));
}
