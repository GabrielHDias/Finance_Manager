import type { ApiErrorResponse } from "@/types/api-error";

export class ApiError extends Error {
  status: number;
  fields: Record<string, string>;

  constructor(response: ApiErrorResponse) {
    super(response.message);

    this.name = "ApiError";
    this.status = response.status;
    this.fields = response.fields;
  }
}