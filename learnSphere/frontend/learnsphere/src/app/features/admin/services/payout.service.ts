import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

export interface PendingPayout {
  teacherId: number;
  teacherName: string;
  avatarUrl: string | null;
  pendingAmount: number;
  courseCount: number;
}

export interface PayoutRecord {
  id: number;
  period: string;
  amount: number;
  status: 'PENDING' | 'PAID';
  transferredAt: string | null;
  createdAt: string;
}

/** Admin-only payout actions: view teachers with a pending royalty balance, and pay them out. */
@Injectable({
  providedIn: 'root'
})
export class PayoutService {
  constructor(private apiService: ApiService) {}

  getPendingPayouts(): Observable<PendingPayout[]> {
    return this.apiService.get<PendingPayout[]>('/admin/payouts');
  }

  processAll(): Observable<{ processed: number }> {
    return this.apiService.post<{ processed: number }>('/admin/payouts/process-all', {});
  }

  payTeacher(teacherId: number): Observable<PayoutRecord> {
    return this.apiService.post<PayoutRecord>(`/admin/payouts/${teacherId}/pay`, {});
  }
}
