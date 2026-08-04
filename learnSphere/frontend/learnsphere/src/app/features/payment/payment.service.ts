import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { PaymentHistoryItem, PaymentOrder, PaymentStatus, PaymentVerifyResponse } from '@core/models/payment.model';

/**
 * PaymentService drives the (simulated) checkout flow: create a gateway order, then
 * immediately verify it (since there's no real gateway UI to redirect through in this
 * project), plus payment history/status lookups used by the payment guard.
 */
@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  constructor(private apiService: ApiService) {}

  checkout(courseId: number): Observable<PaymentOrder> {
    return this.apiService.post<PaymentOrder>('/payments/checkout', { courseId });
  }

  verify(orderId: string): Observable<PaymentVerifyResponse> {
    return this.apiService.post<PaymentVerifyResponse>('/payments/verify', { orderId });
  }

  getHistory(): Observable<PaymentHistoryItem[]> {
    return this.apiService.get<PaymentHistoryItem[]>('/payments/history');
  }

  getStatus(courseId: number): Observable<PaymentStatus> {
    return this.apiService.get<PaymentStatus>('/payments/status', { params: { courseId } });
  }
}
