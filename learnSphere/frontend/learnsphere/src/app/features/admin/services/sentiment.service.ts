import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

export interface SentimentSummary {
  totalAnalyzed: number;
  positivePercent: number;
  neutralPercent: number;
  negativePercent: number;
  courseTypeSentiment: { type: string; positive: number; color: string }[];
  negativeKeywords: { word: string; count: number; severity: string }[];
  recommendations: { title: string; description: string; action: string; actionClass: string }[];
  teacherScores: { initials: string; name: string; comments: number; score: string; status: string }[];
}

/**
 * SentimentService — the backend computes this with a simple keyword-based
 * heuristic (see Service/support/KeywordLists.java), not real NLP/ML.
 */
@Injectable({
  providedIn: 'root'
})
export class SentimentService {
  constructor(private apiService: ApiService) {}

  getSentiment(): Observable<SentimentSummary> {
    return this.apiService.get<SentimentSummary>('/admin/sentiment');
  }
}
