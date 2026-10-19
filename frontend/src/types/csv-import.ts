export interface CsvImportRow {
  line: number;
  date: string;
  description: string;
  amount: string;
  type: string;
  status: "READY" | "DUPLICATE" | "INVALID";
  errors: string[];
}

export interface CsvImportPreview {
  hash: string;
  accountName: string;
  categoryName: string;
  ready: number;
  duplicates: number;
  invalid: number;
  rows: CsvImportRow[];
}

export interface CsvImportResult {
  imported: number;
  skipped: number;
}
