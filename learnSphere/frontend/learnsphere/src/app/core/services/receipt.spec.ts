import { buildReceipt, formatPaidAt, pdfSafe, LedgerPayment } from './receipt';

/** W6-13 / AUD-01 (audit.md A1, A3, A4): receipt identity, time, status and ASCII safety. */
describe('buildReceipt (W6-13)', () => {
  const owner = { fullName: 'Asha Rao', email: 'asha@example.test' };
  const paid: LedgerPayment = {
    id: 42,
    courseId: 7,
    courseTitle: 'Distributed Systems',
    department: 'Computer Science',
    amount: 2500,
    currency: 'INR',
    status: 'PAID',
    gatewayOrderId: 'order_A',
    gatewayPaymentId: 'pay_A',
    createdAt: '2026-11-01T14:00:00',
    paidAt: '2026-11-01T14:05:09'
  };

  it('uses the real ledger, order and payment identifiers (no fabricated INV- number)', () => {
    const r = buildReceipt(paid, owner)!;
    expect(r.receiptNumber).toBe('RCPT-42');
    expect(r.orderId).toBe('order_A');
    expect(r.paymentId).toBe('pay_A');
    expect(r.receiptNumber).not.toMatch(/INV-/);
  });

  it('carries the paid date and time from paidAt, not createdAt', () => {
    expect(buildReceipt(paid, owner)!.paidAtLabel).toBe('01 Nov 2026, 14:05:09');
  });

  it('formats the amount in rupees with ASCII only', () => {
    const r = buildReceipt({ ...paid, amount: '123456.5' }, owner)!;
    expect(r.amount).toBe(123456.5);
    expect(r.amountLabel).toBe('INR 1,23,456.50');
  });

  it('only issues a receipt for a PAID row with a gateway payment id, order id and paid time', () => {
    expect(buildReceipt({ ...paid, status: 'CREATED' }, owner)).toBeNull();
    expect(buildReceipt({ ...paid, status: 'FAILED' }, owner)).toBeNull();
    expect(buildReceipt({ ...paid, gatewayPaymentId: null }, owner)).toBeNull();
    expect(buildReceipt({ ...paid, gatewayOrderId: '' }, owner)).toBeNull();
    expect(buildReceipt({ ...paid, paidAt: null }, owner)).toBeNull();
    expect(buildReceipt({ ...paid, paidAt: 'not-a-date' }, owner)).toBeNull();
  });

  it('bills the signed-in owner and never invents an e-mail address', () => {
    expect(buildReceipt(paid, owner)!.studentEmail).toBe('asha@example.test');
    const anonymous = buildReceipt(paid, { fullName: 'Asha Rao' })!;
    expect(anonymous.studentEmail).toBe('');
  });
});

describe('formatPaidAt (W6-13)', () => {
  it('formats the server wall-clock time without shifting time zones', () => {
    expect(formatPaidAt('2026-01-05T09:03:00')).toBe('05 Jan 2026, 09:03:00');
    expect(formatPaidAt('2026-12-31T23:59:59.123456')).toBe('31 Dec 2026, 23:59:59');
    expect(formatPaidAt('2026-12-31T23:59')).toBe('31 Dec 2026, 23:59:00');
  });

  it('returns null for missing or malformed values', () => {
    expect(formatPaidAt(undefined)).toBeNull();
    expect(formatPaidAt('yesterday')).toBeNull();
  });
});

describe('pdfSafe (W6-13 / audit A1)', () => {
  it('replaces glyphs Helvetica cannot draw', () => {
    expect(pdfSafe('₹2,500 • paid ✓ — “ok” … ‘x’')).toBe('INR 2,500 - paid - "ok" ... \'x\'');
  });

  it('transliterates accents and leaves only printable ASCII', () => {
    const out = pdfSafe('José Müller\tदेव');
    expect(out.startsWith('Jose Muller ')).toBe(true);
    expect(out).toMatch(/^[\x20-\x7E]*$/);
  });

  it('tolerates null and undefined', () => {
    expect(pdfSafe(null)).toBe('');
    expect(pdfSafe(undefined)).toBe('');
  });
});
