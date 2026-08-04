import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

export interface LiveSession {
  id: number;
  courseId: number;
  courseTitle: string;
  title: string;
  scheduledAt: string;
  status: 'SCHEDULED' | 'LIVE' | 'ENDED';
  joinUrl: string | null;
}

export interface CreateLiveSessionPayload {
  courseId: number;
  title: string;
  scheduledAt: string;
}

/** Teacher's own live-session scheduling/management. */
@Injectable({
  providedIn: 'root'
})
export class LiveSessionService {
  constructor(private apiService: ApiService) {}

  getSessions(): Observable<LiveSession[]> {
    return this.apiService.get<LiveSession[]>('/teacher/live-sessions');
  }

  createSession(payload: CreateLiveSessionPayload): Observable<LiveSession> {
    return this.apiService.post<LiveSession>('/teacher/live-sessions', payload);
  }

  startSession(id: number): Observable<LiveSession> {
    return this.apiService.put<LiveSession>(`/teacher/live-sessions/${id}/start`, {});
  }

  endSession(id: number): Observable<LiveSession> {
    return this.apiService.put<LiveSession>(`/teacher/live-sessions/${id}/end`, {});
  }
}
