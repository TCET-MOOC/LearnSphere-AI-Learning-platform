import { PdfExportService, StatementData } from './pdf-export.service';
import { Receipt } from './receipt';

/**
 * Records every call the PDF generator makes so the spec can assert on
 * what is drawn (text, colour, position) without rendering a real PDF.
 */
function recordingDoc() {
  const texts: { s: string; x: number; y: number }[] = [];
  const colours: number[][] = [];
  const saved: string[] = [];
  const charWidth = 2; // mm per character in this fake
  const doc: any = {
    internal: { pageSize: { getWidth: () => 210, getHeight: () => 297 } },
    setFont: () => {},
    setFontSize: () => {},
    setLineWidth: () => {},
    setTextColor: (...a: number[]) => colours.push(a),
    setFillColor: (...a: number[]) => colours.push(a),
    setDrawColor: (...a: number[]) => colours.push(a),
    rect: () => {},
    roundedRect: () => {},
    line: () => {},
    addPage: vi.fn(),
    getTextWidth: (s: string) => s.length * charWidth,
    splitTextToSize: (s: string, w: number) => {
      const per = Math.max(1, Math.floor(w / charWidth));
      const out: string[] = [];
      for (let i = 0; i < s.length; i += per) out.push(s.slice(i, i + per));
      return out.length ? out : [''];
    },
    text: (s: string | string[], x: number, y: number) =>
      (Array.isArray(s) ? s : [s]).forEach((t) => texts.push({ s: t, x, y })),
    save: (name: string) => saved.push(name)
  };
  return { doc, texts, colours, saved };
}

const receipt = (overrides: Partial<Receipt> = {}): Receipt => ({
  receiptNumber: 'RCPT-42',
  orderId: 'order_A',
  paymentId: 'pay_A',
  paidAtLabel: '01 Nov 2026, 14:05:09',
  status: 'PAID',
  statusLabel: 'Paid',
  studentName: 'Asha Rao',
  studentEmail: 'asha@example.test',
  courseTitle: 'Distributed Systems',
  courseDepartment: 'Computer Science',
  amount: 2500,
  amountLabel: 'INR 2,500.00',
  currency: 'INR',
  ...overrides
});

describe('PdfExportService.generateStudentReceipt (W6-13 / AUD-01)', () => {
  let service: PdfExportService;

  beforeEach(() => {
    service = new PdfExportService();
  });

  function render(r: Receipt) {
    const rec = recordingDoc();
    vi.spyOn(service as any, 'createDoc').mockReturnValue(rec.doc);
    service.generateStudentReceipt(r);
    return rec;
  }

  it('prints the real receipt, order and payment identifiers, paid date and time, status and amount', () => {
    const { texts } = render(receipt());
    const all = texts.map((t) => t.s).join('\n');
    expect(all).toContain('PAYMENT RECEIPT');
    expect(all).toContain('RCPT-42');
    expect(all).toContain('order_A');
    expect(all).toContain('pay_A');
    expect(all).toContain('01 Nov 2026, 14:05:09');
    expect(all).toContain('Paid');
    expect(all).toContain('INR 2,500.00');
  });

  it('draws only printable ASCII (no rupee sign, bullets or check marks)', () => {
    const { texts } = render(receipt({ courseTitle: 'Café ₹ Design • Basics ✓' }));
    for (const t of texts) {
      expect(t.s, `non-ASCII text: ${t.s}`).toMatch(/^[\x20-\x7E]*$/);
    }
  });

  it('is monochrome: every colour is a grey (r = g = b)', () => {
    const { colours } = render(receipt());
    expect(colours.length).toBeGreaterThan(0);
    for (const c of colours) {
      expect(new Set(c).size, `colour ${c.join(',')} is not grey`).toBe(1);
    }
  });

  it('makes no tax, GST or invoice claim', () => {
    const { texts } = render(receipt());
    const all = texts.map((t) => t.s).join('\n');
    expect(all).not.toMatch(/GST|TAX INVOICE|Taxes|INV-/i);
  });

  it('wraps a long course title and grows the row so the total is drawn below it', () => {
    const short = render(receipt());
    const long = render(
      receipt({ courseTitle: 'A very long course title that keeps going '.repeat(4).trim() })
    );
    const totalY = (r: ReturnType<typeof recordingDoc>) =>
      r.texts.find((t) => t.s.startsWith('Total paid'))!.y;

    expect(long.texts.filter((t) => t.s.startsWith('A very long') || t.s.includes('keeps')).length)
      .toBeGreaterThan(1);
    expect(totalY(long)).toBeGreaterThan(totalY(short));
  });

  it('keeps every line inside the page margins', () => {
    const { texts } = render(
      receipt({ studentEmail: 'a.really.long.address.for.testing.wrapping@an-unusually-long-domain.example.test' })
    );
    for (const t of texts) {
      expect(t.x).toBeGreaterThanOrEqual(0);
      expect(t.x + t.s.length * 2, `overflow: ${t.s}`).toBeLessThanOrEqual(210);
    }
  });

  it('saves the file under the real receipt number', () => {
    const { saved } = render(receipt());
    expect(saved).toEqual(['LearnSphere_Receipt_RCPT-42.pdf']);
  });
});

describe('PdfExportService.generateTeacherStatement (W6-13 / audit A1)', () => {
  it('draws only printable ASCII even when amounts arrive with a rupee sign', () => {
    const service = new PdfExportService();
    const rec = recordingDoc();
    vi.spyOn(service as any, 'createDoc').mockReturnValue(rec.doc);
    const data: StatementData = {
      teacherName: 'Dr. Mehta',
      teacherEmail: 't@example.test',
      statementPeriod: 'Nov 2026',
      generatedDate: '01 Nov 2026',
      thisMonthTotal: '₹1,750',
      totalEarned: '₹10,500',
      pendingPayout: '₹750',
      courses: [{ title: 'Algorithms', enrolled: 3, externalPaid: 2, amount: '₹3,500', status: 'LIVE' }]
    };
    service.generateTeacherStatement(data);
    for (const t of rec.texts) {
      expect(t.s, `non-ASCII text: ${t.s}`).toMatch(/^[\x20-\x7E]*$/);
    }
    expect(rec.texts.map((t) => t.s)).toContain('INR 1,750');
  });
});
