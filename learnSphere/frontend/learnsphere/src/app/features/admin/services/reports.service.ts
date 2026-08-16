import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

export interface ReportsSummary {
  totalUsers: number;
  usersByRole: Record<string, number>;
  totalCourses: number;
  coursesByStatus: Record<string, number>;
  totalRevenue: number;
  totalCertificatesIssued: number;
  flaggedContentPending: number;
}

@Injectable({
  providedIn: 'root'
})
export class ReportsService {
  constructor(private apiService: ApiService) {}

  getSummary(): Observable<ReportsSummary> {
    return this.apiService.get<ReportsSummary>('/admin/reports/summary');
  }
}
