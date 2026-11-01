import { RECEIPT_COPY } from '@core/content/receipt.content';

/** A row of GET /api/payments/history (PaymentResponseDto). Amount is in rupees. */
export interface LedgerPayment {
  id: number;
  courseId: number;
  courseTitle?: string | null;
  department?: string | null;
  amount: number | string | null;
  currency?: string | null;
  status: string | null;
  gatewayOrderId?: string | null;
  gatewayPaymentId?: string | null;
  createdAt?: string | null;
  paidAt?: string | null;
}

export interface ReceiptOwner {
  fullName?: string | null;
  email?: string | null;
}

/** Everything a receipt shows. The modal and the PDF both render this one object. */
export interface Receipt {
  receiptNumber: string;
  orderId: string;
  paymentId: string;
  paidAtLabel: string;
  status: 'PAID';
  statusLabel: string;
  studentName: string;
  studentEmail: string;
  courseTitle: string;
  courseDepartment: string;
  amount: number;
  amountLabel: string;
  currency: string;
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const SERVER_DATE_TIME = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?/;

/**
 * Builds a receipt from a ledger row, or null when the row is not a captured payment.
 * Only `PAID` rows with a gateway order id, gateway payment id and paid time qualify (W6-13).
 * The receipt number is derived 1:1 from the ledger payment id; nothing is invented.
 */
export function buildReceipt(payment: LedgerPayment, owner: ReceiptOwner | null | undefined): Receipt | null {
  if (!payment || payment.status !== 'PAID') return null;
  const orderId = payment.gatewayOrderId?.trim();
  const paymentId = payment.gatewayPaymentId?.trim();
  const paidAtLabel = formatPaidAt(payment.paidAt);
  const amount = Number(payment.amount);
  if (!orderId || !paymentId || !paidAtLabel || !Number.isFinite(amount) || payment.id == null) return null;

  const currency = (payment.currency || 'INR').toUpperCase();
  return {
    receiptNumber: `RCPT-${payment.id}`,
    orderId,
    paymentId,
    paidAtLabel,
    status: 'PAID',
    statusLabel: RECEIPT_COPY.statusPaid,
    studentName: owner?.fullName?.trim() || '',
    studentEmail: owner?.email?.trim() || '',
    courseTitle: payment.courseTitle?.trim() || `Course #${payment.courseId}`,
    courseDepartment: payment.department?.trim() || '',
    amount,
    amountLabel: `${currency} ${amount.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
    currency
  };
}

/**
 * Formats a server `LocalDateTime` (no zone) as "01 Nov 2026, 14:05:09".
 * It is the server's wall-clock time, so it is formatted from its parts
 * rather than through `Date`, which would shift it into the viewer's zone.
 */
export function formatPaidAt(value: string | null | undefined): string | null {
  const m = value ? SERVER_DATE_TIME.exec(value) : null;
  if (!m) return null;
  const [, y, mo, d, h, mi, s] = m;
  const month = MONTHS[Number(mo) - 1];
  if (!month) return null;
  return `${d} ${month} ${y}, ${h}:${mi}:${s ?? '00'}`;
}

const GLYPHS: Array<[RegExp, string]> = [
  [/₹\s*/g, 'INR '],
  [/[•·]/g, '-'],
  [/[✓✔]\s*/g, ''],
  [/[–—]/g, '-'],
  [/[“”]/g, '"'],
  [/[‘’]/g, "'"],
  [/…/g, '...'],
  [/\s/g, ' ']
];

/**
 * Makes text drawable by jsPDF's built-in Helvetica (audit.md A1): known glyphs become
 * ASCII equivalents, accents are dropped, and anything else becomes "?".
 */
export function pdfSafe(value: string | null | undefined): string {
  let out = String(value ?? '');
  for (const [pattern, replacement] of GLYPHS) out = out.replace(pattern, replacement);
  return out
    .normalize('NFKD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^\x20-\x7E]+/g, '?');
}
