import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { APP_ENDPOINTS } from '@core/config/app-endpoints';

/**
 * ApiService is a central wrapper around Angular's HttpClient.
 * It prepends the injected API base (APP_ENDPOINTS, W7-03) and handles common options.
 */
@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = inject(APP_ENDPOINTS).api;

  constructor(private http: HttpClient) {}

  /**
   * Helper to format the full URL.
   */
  private getFullUrl(endpoint: string): string {
    // Standardize slashes
    const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
    return `${this.baseUrl}${cleanEndpoint}`;
  }

  /**
   * Perform GET request.
   */
  get<T>(endpoint: string, options?: any): Observable<T> {
    return this.http.get<T>(this.getFullUrl(endpoint), options) as Observable<T>;
  }

  /**
   * Perform POST request.
   */
  post<T>(endpoint: string, body: any, options?: any): Observable<T> {
    return this.http.post<T>(this.getFullUrl(endpoint), body, options) as Observable<T>;
  }

  /**
   * Perform PUT request.
   */
  put<T>(endpoint: string, body: any, options?: any): Observable<T> {
    return this.http.put<T>(this.getFullUrl(endpoint), body, options) as Observable<T>;
  }

  /**
   * Perform PATCH request.
   */
  patch<T>(endpoint: string, body: any, options?: any): Observable<T> {
    return this.http.patch<T>(this.getFullUrl(endpoint), body, options) as Observable<T>;
  }

  /**
   * Perform DELETE request.
   */
  delete<T>(endpoint: string, options?: any): Observable<T> {
    return this.http.delete<T>(this.getFullUrl(endpoint), options) as Observable<T>;
  }
}

