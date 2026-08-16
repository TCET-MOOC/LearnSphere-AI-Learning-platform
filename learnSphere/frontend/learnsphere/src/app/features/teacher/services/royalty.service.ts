import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

export interface RoyaltySummary {
  thisMonthTotal: number;
  totalEarned: number;
  pendingPayout: number;
  externalEnrollments: number;
}

export interface RoyaltyBreakdownRow {
  courseId: number;
  courseTitle: string;
  enrolledCount: number;
  externalPaidCount: number;
  amount: number;
  status: string;
  isDraft: boolean;
}

export interface RoyaltySourceSplit {
  source: string;
  label: string;
  amount: number;
  percentage: number;
}

export interface TeacherRoyaltiesResponse {
  summary: RoyaltySummary;
  byCourse: RoyaltyBreakdownRow[];
  sourceBreakdown: RoyaltySourceSplit[];
}

export interface PayoutRow {
  id: number;
  period: string;
  amount: number;
  status: 'PENDING' | 'PAID';
  transferredAt: string | null;
  createdAt: string;
}

/** Teacher's own royalty earnings + payout history. */
@Injectable({
  providedIn: 'root'
})
export class RoyaltyService {
  constructor(private apiService: ApiService) {}

  getRoyalties(): Observable<TeacherRoyaltiesResponse> {
    return this.apiService.get<TeacherRoyaltiesResponse>('/teacher/royalties');
  }

  getPayouts(): Observable<PayoutRow[]> {
    return this.apiService.get<PayoutRow[]>('/teacher/payouts');
  }
}
