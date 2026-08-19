import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from 'src/environments/environment';

export interface PaymentOrderRequest {
  userId: number;
  courseId: number;
}

export interface PaymentOrderResponse {
  razorpay_order_id: string;
  amount: number;
  currency: string;
  key_id: string;
  is_demo_mode?: boolean;
}

export interface Order {
  id: number;
  razorpayOrderId: string;
  userId: number;
  courseId: number;
  amount: number;
  currency: string;
  status: 'CREATED' | 'PAID' | 'FAILED';
  createdAt?: string;
  updatedAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PaymentsService {

  // Default to 8081 for local development
  private baseUrl = 'http://localhost:8081/api/payments';

  constructor(private http: HttpClient) { }

  createOrder(request: PaymentOrderRequest): Observable<PaymentOrderResponse> {
    return this.http.post<PaymentOrderResponse>(`${this.baseUrl}/orders`, request);
  }

  simulateSuccess(razorpayOrderId: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/orders/simulate-success`, { razorpay_order_id: razorpayOrderId });
  }

  verifyPayment(payload: { razorpay_order_id: string; razorpay_payment_id: string; razorpay_signature?: string }): Observable<any> {
    return this.http.post(`${this.baseUrl}/orders/verify`, payload);
  }

  getUserOrders(userId: number): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.baseUrl}/orders/${userId}`);
  }

  getPaymentHistory(): Observable<any[]> {
    return this.http.get<any[]>(`${environment.apiUrl}/payments/history`);
  }
}
