import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';

/**
 * ErrorHandlerService extracts a user-facing message from a failed HTTP response.
 * The backend's GlobalExceptionHandler returns { message: string, fieldErrors?: {...} }.
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
      if (error.error.fieldErrors) {
        const firstField = Object.values(error.error.fieldErrors)[0];
        if (firstField) {
          return String(firstField);
        }
      }
      if (error.error.message) {
        return error.error.message;
      }
    }
    if (error.status === 0) {
      return 'Cannot reach the server. Please check your connection and try again.';
    }
    return fallback;
  }
}
