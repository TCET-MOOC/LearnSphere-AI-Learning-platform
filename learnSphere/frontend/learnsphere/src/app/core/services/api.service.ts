import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * ApiService is a central wrapper around Angular's HttpClient.
 * It prepends the environment's base apiUrl and handles common options.
 */
@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = environment.apiUrl;

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
   * Perform DELETE request.
   */
  delete<T>(endpoint: string, options?: any): Observable<T> {
    return this.http.delete<T>(this.getFullUrl(endpoint), options) as Observable<T>;
  }
}
