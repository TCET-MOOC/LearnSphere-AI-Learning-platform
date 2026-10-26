import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

/** RFC 9457 problem+json body returned by the backend for every error. */
export interface ProblemResponse {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  traceId?: string;
  errors?: Array<{ field: string; message: string }>;
  /** Legacy members kept while any pre-P0-09 endpoint is still unmigrated. */
  message?: string;
  fieldErrors?: Record<string, string>;
}

/**
 * ErrorHandlerService extracts a user-facing message from a failed HTTP response.
 *
 * <p>P0-09 switched the backend to {@code application/problem+json}. Field
 * validation messages win, then the problem {@code detail}; the legacy
 * {@code message}/{@code fieldErrors} shape remains as a fallback.</p>
 */
@Injectable({
  providedIn: 'root'
})
export class ErrorHandlerService {
  extractMessage(error: HttpErrorResponse, fallback = 'Something went wrong. Please try again.'): string {
    if (error.error) {
      if (typeof error.error === 'string') {
        return error.error;
      }

      const problem = error.error as ProblemResponse;

      if (problem.errors?.length) {
        const first = problem.errors.find((entry) => entry?.message);
        if (first) {
          return first.message;
        }
      }
      if (problem.detail) {
        return problem.detail;
      }
      if (problem.fieldErrors) {
        const firstField = Object.values(problem.fieldErrors)[0];
        if (firstField) {
          return String(firstField);
        }
      }
      if (problem.message) {
        return problem.message;
      }
    }
    if (error.status === 0) {
      return 'Cannot reach the server. Please check your connection and try again.';
    }
    return fallback;
  }
}
