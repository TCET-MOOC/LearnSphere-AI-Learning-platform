/**
 * Response from POST /api/payments/checkout — a simulated payment-gateway "order".
 * There is no real gateway (Razorpay/Stripe) wired up in this project, so checkout
 * immediately followed by verify simulates the full pay flow without a hosted redirect.
 */
export interface PaymentOrder {
  orderId: string;
  amount: number;
  currency: string;
  courseId: number;
}

/** Response from POST /api/payments/verify and the shape of each entry in the history list. */
export interface PaymentVerifyResponse {
  id: number;
  courseId: number;
  courseTitle: string;
  amount: number;
  currency: string;
  status: 'PENDING' | 'SUCCESS' | 'FAILED';
  gatewayOrderId: string;
  gatewayPaymentId: string | null;
  createdAt: string;
  paidAt: string | null;
}

/** A single row in GET /api/payments/history. Same shape as PaymentVerifyResponse. */
export type PaymentHistoryItem = PaymentVerifyResponse;

/** Response from GET /api/payments/status?courseId= */
export interface PaymentStatus {
  paid: boolean;
}
