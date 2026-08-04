import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

export interface CourseRevenueRow {
  courseId: number;
  courseTitle: string;
  teacherName: string;
  amount: number;
  purchases: number;
}

export interface MonthlyRevenueRow {
  month: string;
  amount: number;
}

export interface RevenueSummary {
  totalRevenue: number;
  platformEarnings: number;
  teacherRoyalties: number;
  platformCutPercent: number;
  teacherRoyaltyPercent: number;
  byCourse: CourseRevenueRow[];
  byMonth: MonthlyRevenueRow[];
}

@Injectable({
  providedIn: 'root'
})
export class RevenueService {
  constructor(private apiService: ApiService) {}

  getRevenue(): Observable<RevenueSummary> {
    return this.apiService.get<RevenueSummary>('/admin/revenue');
  }
}
