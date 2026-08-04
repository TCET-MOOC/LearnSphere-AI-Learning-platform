import { Injectable } from '@angular/core';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';

export interface UploadEvent {
  type: 'progress' | 'done';
  percent: number;
  url?: string;
}

/**
 * UploadService streams a file upload (video or resource) to the backend and
 * emits progress percentage events, finishing with the stored file's public
 * URL. Uses HttpClient directly (not ApiService) so the browser is free to
 * set its own multipart/form-data Content-Type + boundary header — manually
 * forcing application/json (as some wrappers do) would break the upload.
 */
@Injectable({
  providedIn: 'root'
})
export class UploadService {
  constructor(private http: HttpClient) {}

  uploadVideo(file: File): Observable<UploadEvent> {
    return this.upload('/teacher/upload/video', file);
  }

  uploadResource(file: File): Observable<UploadEvent> {
    return this.upload('/teacher/upload/resource', file);
  }

  private upload(path: string, file: File): Observable<UploadEvent> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http
      .post<{ url: string }>(`${environment.apiUrl}${path}`, formData, {
        reportProgress: true,
        observe: 'events'
      })
      .pipe(
        filter(event =>
          event.type === HttpEventType.UploadProgress || event.type === HttpEventType.Response
        ),
        map(event => {
          if (event.type === HttpEventType.UploadProgress) {
            const percent = event.total ? Math.round((100 * event.loaded) / event.total) : 0;
            return { type: 'progress', percent } as UploadEvent;
          }
          // HttpEventType.Response
          const body = (event as any).body as { url: string };
          return { type: 'done', percent: 100, url: body?.url } as UploadEvent;
        })
      );
  }
}
