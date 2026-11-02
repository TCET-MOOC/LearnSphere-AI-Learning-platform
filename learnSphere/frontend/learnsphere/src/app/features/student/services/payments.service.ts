import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { APP_ENDPOINTS } from '@core/config/app-endpoints';

export interface PaymentOrderRequest {
  userId?: number;
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

  private readonly endpoints = inject(APP_ENDPOINTS);
  /** Payments service (orders/verify); payment history lives on the main API. */
  private baseUrl = this.endpoints.payments;

  constructor(private http: HttpClient) { }

  createOrder(request: PaymentOrderRequest): Observable<PaymentOrderResponse> {
    return this.http.post<PaymentOrderResponse>(`${this.baseUrl}/orders`, request);
  }

  verifyPayment(payload: { razorpay_order_id: string; razorpay_payment_id: string; razorpay_signature?: string }): Observable<any> {
    return this.http.post(`${this.baseUrl}/orders/verify`, payload);
  }

  getMyOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.baseUrl}/orders/me`);
  }

  getUserOrders(userId?: number): Observable<Order[]> {
    return this.getMyOrders();
  }

  getPaymentHistory(): Observable<any[]> {
    return this.http.get<any[]>(`${this.endpoints.api}/payments/history`);
  }
}
