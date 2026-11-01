import { mergeHistory, normalizePaymentStatus, summarize, HistoryRow } from './payment-history';
import { LedgerPayment } from '@core/services/receipt';
import { Order } from '../services/payments.service';

/** W6-14 / AUD-02 (audit.md B2–B6, B12): one truthful payment history. */
describe('payment history (W6-14)', () => {
  const owner = { fullName: 'Asha Rao', email: 'asha@example.test' };

  const ledger = (over: Partial<LedgerPayment>): LedgerPayment => ({
    id: 1, courseId: 7, courseTitle: 'Distributed Systems', department: 'Computer Science',
    amount: 2500, currency: 'INR', status: 'PAID',
    gatewayOrderId: 'order_A', gatewayPaymentId: 'pay_A',
    createdAt: '2026-11-01T14:00:00', paidAt: '2026-11-01T14:05:09',
    ...over
  });
  const gateway = (over: Partial<Order>): Order => ({
    id: 900, razorpayOrderId: 'order_X', userId: 2, courseId: 7,
    amount: 250000, currency: 'INR', status: 'CREATED', createdAt: '2026-11-01T13:00:00',
    ...over
  });
  const byKey = (rows: HistoryRow[], key: string) => rows.find((r) => r.gatewayOrderId === key)!;

  describe('normalizePaymentStatus (B4)', () => {
    it('maps both vocabularies onto PAID / PENDING / FAILED', () => {
      expect(['SUCCESS', 'PAID', 'paid', 'CAPTURED'].map(normalizePaymentStatus)).toEqual(['PAID', 'PAID', 'PAID', 'PAID']);
      expect(['PENDING', 'CREATED', '', null, 'weird'].map(normalizePaymentStatus))
        .toEqual(['PENDING', 'PENDING', 'PENDING', 'PENDING', 'PENDING']);
      expect(['FAILED', 'CANCELLED', 'REJECTED'].map(normalizePaymentStatus)).toEqual(['FAILED', 'FAILED', 'FAILED']);
    });
  });

  it('keeps the ledger status instead of forcing PAID (B2)', () => {
    const rows = mergeHistory([ledger({ status: 'CREATED', gatewayPaymentId: null, paidAt: null })], [], owner);
    expect(rows[0].status).toBe('PENDING');
    expect(rows[0].statusLabel).toBe('Pending');
    expect(rows[0].receipt).toBeNull();
  });

  it('keeps a failed attempt and the later success for the same course, newest first (B3)', () => {
    const rows = mergeHistory(
      [
        ledger({ id: 1, status: 'FAILED', gatewayOrderId: 'order_F', gatewayPaymentId: 'pay_F', paidAt: null, createdAt: '2026-11-01T10:00:00' }),
        ledger({ id: 2, status: 'PAID', gatewayOrderId: 'order_S', gatewayPaymentId: 'pay_S' })
      ],
      [],
      owner
    );
    expect(rows.map((r) => [r.gatewayOrderId, r.status])).toEqual([['order_S', 'PAID'], ['order_F', 'FAILED']]);
  });

  it('dedupes by gateway order, not by course, and the ledger wins (B3, B12)', () => {
    const rows = mergeHistory(
      [ledger({ gatewayOrderId: 'order_A' })],
      [
        gateway({ razorpayOrderId: 'order_A', status: 'CREATED' }),
        gateway({ id: 901, razorpayOrderId: 'order_B', status: 'FAILED', createdAt: '2026-11-01T09:00:00' })
      ],
      owner
    );
    expect(rows).toHaveLength(2);
    expect(byKey(rows, 'order_A')).toMatchObject({ source: 'ledger', status: 'PAID', gatewayPaymentId: 'pay_A' });
    expect(byKey(rows, 'order_B')).toMatchObject({ source: 'gateway', status: 'FAILED', statusLabel: 'Failed' });
  });

  it('never shows a success without a ledger payment', () => {
    const rows = mergeHistory([], [gateway({ razorpayOrderId: 'order_P', status: 'PAID' })], owner);
    expect(rows[0].status).toBe('PENDING');
    expect(rows[0].receipt).toBeNull();
  });

  it('uses rupees everywhere: ledger rupees stay, gateway paise are divided once (B5)', () => {
    const rows = mergeHistory(
      [ledger({ amount: '2500.00' })],
      [gateway({ razorpayOrderId: 'order_B', amount: 99900, status: 'FAILED' })],
      owner
    );
    expect(byKey(rows, 'order_A').amountRupees).toBe(2500);
    expect(byKey(rows, 'order_B').amountRupees).toBe(999);
  });

  it('preserves the payment id and capture time, and shows the capture time for paid rows (B6)', () => {
    const [paid] = mergeHistory([ledger({})], [], owner);
    expect(paid.gatewayPaymentId).toBe('pay_A');
    expect(paid.paidAt).toBe('2026-11-01T14:05:09');
    expect(paid.createdAt).toBe('2026-11-01T14:00:00');
    expect(paid.displayAtLabel).toBe('01 Nov 2026, 14:05:09');

    const [pending] = mergeHistory([], [gateway({ createdAt: '2026-11-01T13:00:00' })], owner);
    expect(pending.displayAtLabel).toBe('01 Nov 2026, 13:00:00');
  });

  it('offers a receipt only for a paid ledger row', () => {
    const rows = mergeHistory(
      [ledger({}), ledger({ id: 2, status: 'FAILED', gatewayOrderId: 'order_F', paidAt: null })],
      [],
      owner
    );
    expect(byKey(rows, 'order_A').receipt?.receiptNumber).toBe('RCPT-1');
    expect(byKey(rows, 'order_F').receipt).toBeNull();
  });

  it('counts rows by status, paid courses by distinct course, and totals only paid rupees', () => {
    const rows = mergeHistory(
      [
        ledger({ id: 1, gatewayOrderId: 'o1', courseId: 7, amount: 2500 }),
        ledger({ id: 2, gatewayOrderId: 'o2', courseId: 8, amount: 999.5 }),
        ledger({ id: 3, gatewayOrderId: 'o3', courseId: 9, amount: 400, status: 'FAILED', paidAt: null })
      ],
      [gateway({ razorpayOrderId: 'o4', courseId: 9, amount: 40000, status: 'CREATED' })],
      owner
    );
    expect(summarize(rows)).toEqual({ all: 4, paid: 2, pending: 1, failed: 1, paidCourses: 2, totalPaidRupees: 3499.5 });
  });

  it('ignores rows without a course and gateway drafts without a positive amount', () => {
    const rows = mergeHistory(
      [ledger({ courseId: null as unknown as number })],
      [gateway({ amount: 0 })],
      owner
    );
    expect(rows).toEqual([]);
  });
});
