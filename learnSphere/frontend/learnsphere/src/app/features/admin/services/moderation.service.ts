import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

export interface FlaggedContentItem {
  id: number;
  contentType: 'DISCUSSION_POST' | 'MESSAGE';
  contentTypeLabel: string;
  contentId: number;
  reason: 'BULLYING' | 'SPAM' | 'SUSPICIOUS' | 'HIGH_RISK';
  severity: 'high' | 'medium';
  status: 'PENDING' | 'RESOLVED' | 'DISMISSED';
  authorId: number | null;
  authorName: string;
  authorInitials: string;
  courseTitle: string | null;
  snippet: string;
  createdAt: string;
}

/**
 * ModerationService drives the admin "Flagged Content" queue.
 * The backend runs a simple keyword-based auto-flagger (not real ML/NLP)
 * over discussion posts and messages, and this service resolves/dismisses
 * the resulting queue.
 */
@Injectable({
  providedIn: 'root'
})
export class ModerationService {
  constructor(private apiService: ApiService) {}

  getFlagged(category?: string): Observable<FlaggedContentItem[]> {
    const query = category && category !== 'all' ? `?category=${encodeURIComponent(category)}` : '';
    return this.apiService.get<FlaggedContentItem[]>(`/admin/flagged${query}`);
  }

  resolve(id: number): Observable<FlaggedContentItem> {
    return this.apiService.put<FlaggedContentItem>(`/admin/flagged/${id}/resolve`, {});
  }

  dismiss(id: number): Observable<FlaggedContentItem> {
    return this.apiService.put<FlaggedContentItem>(`/admin/flagged/${id}/dismiss`, {});
  }
}
