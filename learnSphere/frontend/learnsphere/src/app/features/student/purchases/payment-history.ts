import { LedgerPayment, Receipt, ReceiptOwner, buildReceipt, formatPaidAt } from '@core/services/receipt';
import { Order } from '../services/payments.service';

/**
 * W6-14 / AUD-02 (audit.md B2-B6, B12): one truthful payment history.
 *
 * Sources:
 *  - ledger  = GET /api/payments/history (main backend `payments`, amounts in rupees). The authority.
 *  - gateway = GET /api/payments/orders/me (payments service `orders`, amounts in paise). Supplementary:
 *    it is the only place a Razorpay failure or an unfinished checkout is recorded (B12).
 *
 * Rules: one display vocabulary; one row per gateway order (never per course), ledger first;
 * no success without a ledger payment; every amount in rupees; payment id and times kept.
 */
export type PaymentStatus = 'PAID' | 'PENDING' | 'FAILED';

export const STATUS_LABELS: Record<PaymentStatus, string> = {
  PAID: 'Paid',
  PENDING: 'Pending',
  FAILED: 'Failed'
};

export interface HistoryRow {
  key: string;
  source: 'ledger' | 'gateway';
  courseId: number;
  courseTitle?: string;
  courseDepartment?: string;
  courseThumbnail?: string;
  gatewayOrderId: string | null;
  gatewayPaymentId: string | null;
  amountRupees: number;
  currency: string;
  status: PaymentStatus;
  statusLabel: string;
  createdAt: string | null;
  paidAt: string | null;
  /** Capture time for a paid row, otherwise when the attempt was created. */
  displayAtLabel: string | null;
  receipt: Receipt | null;
}

export interface HistorySummary {
  all: number;
  paid: number;
  pending: number;
  failed: number;
  paidCourses: number;
  totalPaidRupees: number;
}

const PAID = new Set(['PAID', 'SUCCESS', 'COMPLETED', 'CAPTURED']);
const FAILED = new Set(['FAILED', 'CANCELLED', 'REJECTED']);

/** Maps the ledger (`PENDING|SUCCESS|FAILED`, served as `CREATED|PAID|FAILED`) and gateway vocabularies. */
export function normalizePaymentStatus(raw: unknown): PaymentStatus {
  const s = String(raw ?? '').trim().toUpperCase();
  if (PAID.has(s)) return 'PAID';
  if (FAILED.has(s)) return 'FAILED';
  return 'PENDING';
}

function fromLedger(p: LedgerPayment, owner: ReceiptOwner | null | undefined): HistoryRow {
  const status = normalizePaymentStatus(p.status);
  const paidAt = p.paidAt ?? null;
  const createdAt = p.createdAt ?? null;
  return {
    key: p.gatewayOrderId?.trim() || `ledger-${p.id}`,
    source: 'ledger',
    courseId: p.courseId,
    courseTitle: p.courseTitle ?? undefined,
    courseDepartment: p.department ?? undefined,
    gatewayOrderId: p.gatewayOrderId?.trim() || null,
    gatewayPaymentId: p.gatewayPaymentId?.trim() || null,
    amountRupees: Number(p.amount) || 0,
    currency: (p.currency || 'INR').toUpperCase(),
    status,
    statusLabel: STATUS_LABELS[status],
    createdAt,
    paidAt,
    displayAtLabel: formatPaidAt(status === 'PAID' ? paidAt ?? createdAt : createdAt),
    receipt: buildReceipt(p, owner)
  };
}

function fromGateway(o: Order): HistoryRow {
  // A gateway "PAID" without a ledger payment is not a success yet: confirmation is still pending.
  const raw = normalizePaymentStatus(o.status);
  const status: PaymentStatus = raw === 'PAID' ? 'PENDING' : raw;
  const createdAt = o.createdAt ?? null;
  return {
    key: o.razorpayOrderId?.trim() || `gateway-${o.id}`,
    source: 'gateway',
    courseId: o.courseId,
    gatewayOrderId: o.razorpayOrderId?.trim() || null,
    gatewayPaymentId: null,
    amountRupees: (Number(o.amount) || 0) / 100, // the payments service stores paise
    currency: (o.currency || 'INR').toUpperCase(),
    status,
    statusLabel: STATUS_LABELS[status],
    createdAt,
    paidAt: null,
    displayAtLabel: formatPaidAt(createdAt),
    receipt: null
  };
}

const sortTime = (r: HistoryRow) => (r.status === 'PAID' ? r.paidAt ?? r.createdAt : r.createdAt) ?? '';

export function mergeHistory(
  ledger: LedgerPayment[],
  gateway: Order[],
  owner: ReceiptOwner | null | undefined
): HistoryRow[] {
  const rows = new Map<string, HistoryRow>();
  for (const p of ledger ?? []) {
    if (!p || p.courseId == null) continue;
    const row = fromLedger(p, owner);
    if (!rows.has(row.key)) rows.set(row.key, row);
  }
  for (const o of gateway ?? []) {
    if (!o || o.courseId == null || !(Number(o.amount) > 0)) continue;
    const row = fromGateway(o);
    if (!rows.has(row.key)) rows.set(row.key, row); // ledger precedence
  }
  return Array.from(rows.values()).sort((a, b) => sortTime(b).localeCompare(sortTime(a)));
}

export function summarize(rows: HistoryRow[]): HistorySummary {
  const paid = rows.filter((r) => r.status === 'PAID');
  const totalPaise = paid.reduce((sum, r) => sum + Math.round(r.amountRupees * 100), 0);
  return {
    all: rows.length,
    paid: paid.length,
    pending: rows.filter((r) => r.status === 'PENDING').length,
    failed: rows.filter((r) => r.status === 'FAILED').length,
    paidCourses: new Set(paid.map((r) => r.courseId)).size,
    totalPaidRupees: totalPaise / 100
  };
}
