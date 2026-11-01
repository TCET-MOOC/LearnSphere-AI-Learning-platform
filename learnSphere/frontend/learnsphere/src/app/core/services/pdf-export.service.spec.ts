import { PdfExportService, StatementData } from './pdf-export.service';
import { Receipt } from './receipt';

/**
 * Records every call the PDF generator makes so the spec can assert on
 * what is drawn (text, colour, position) without rendering a real PDF.
 */
function recordingDoc() {
  const texts: { s: string; x: number; y: number; page: number }[] = [];
  const colours: number[][] = [];
  const saved: string[] = [];
  /** Order of colour changes and filled shapes, to check every fill sets its own colour. */
  const ops: string[] = [];
  let page = 1;
  const charWidth = 2; // mm per character in this fake
  const shape = (style?: string) => {
    if (style && style.includes('F')) ops.push('fill-shape');
  };
  const doc: any = {
    internal: { pageSize: { getWidth: () => 210, getHeight: () => 297 } },
    setFont: () => {},
    setFontSize: () => {},
    setLineWidth: () => {},
    setTextColor: (...a: number[]) => { colours.push(a); ops.push('text-colour'); },
    setFillColor: (...a: number[]) => { colours.push(a); ops.push('fill-colour'); },
    setDrawColor: (...a: number[]) => colours.push(a),
    rect: (_x: number, _y: number, _w: number, _h: number, style?: string) => shape(style),
    roundedRect: (_x: number, _y: number, _w: number, _h: number, _rx: number, _ry: number, style?: string) => shape(style),
    line: () => {},
    addPage: vi.fn(() => { page += 1; }),
    getTextWidth: (s: string) => s.length * charWidth,
    splitTextToSize: (s: string, w: number) => {
      const per = Math.max(1, Math.floor(w / charWidth));
      const out: string[] = [];
      for (let i = 0; i < s.length; i += per) out.push(s.slice(i, i + per));
      return out.length ? out : [''];
    },
    text: (s: string | string[], x: number, y: number) =>
      (Array.isArray(s) ? s : [s]).forEach((t) => texts.push({ s: t, x, y, page })),
    save: (name: string) => saved.push(name)
  };
  return { doc, texts, colours, saved, ops };
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

describe('PdfExportService.generateTeacherStatement (W6-13 / audit A1; statement rebuild)', () => {
  const statement = (overrides: Partial<StatementData> = {}): StatementData => ({
    teacherName: 'Dr. Mehta',
    teacherEmail: 't@example.test',
    statementPeriod: 'November 2026',
    generatedDate: '01 Nov 2026',
    thisMonthTotal: '₹1,750',
    totalEarned: '₹10,500',
    pendingPayout: '₹750',
    courses: [{ title: 'Algorithms', enrolled: 3, externalPaid: 2, amount: '₹3,500', status: 'LIVE' }],
    ...overrides
  });

  function render(data: StatementData) {
    const service = new PdfExportService();
    const rec = recordingDoc();
    vi.spyOn(service as any, 'createDoc').mockReturnValue(rec.doc);
    service.generateTeacherStatement(data);
    return rec;
  }

  it('draws only printable ASCII even when amounts arrive with a rupee sign', () => {
    const rec = render(statement());
    for (const t of rec.texts) {
      expect(t.s, `non-ASCII text: ${t.s}`).toMatch(/^[\x20-\x7E]*$/);
    }
    expect(rec.texts.map((t) => t.s)).toContain('INR 1,750');
  });

  it('sets a fill colour for every filled box, so no box inherits the last text colour', () => {
    const { ops } = render(statement());
    ops.forEach((op, i) => {
      if (op !== 'fill-shape') return;
      const lastColour = ops.slice(0, i).reverse().find((o) => o !== 'fill-shape');
      expect(lastColour, `filled shape #${i} inherits a text colour`).toBe('fill-colour');
    });
  });

  it('is monochrome like the receipt', () => {
    const { colours } = render(statement());
    for (const c of colours) {
      expect(new Set(c).size, `colour ${c.join(',')} is not grey`).toBe(1);
    }
  });

  it('never overlaps text on the same line, and keeps it inside the page', () => {
    const { texts } = render(statement({
      courses: [{ title: 'Algorithms', enrolled: 1234, externalPaid: 999, amount: '₹12,34,567', status: 'PENDING_REVIEW' }]
    }));
    const lines = new Map<string, typeof texts>();
    for (const t of texts) {
      const key = `${t.page}:${t.y.toFixed(2)}`;
      lines.set(key, [...(lines.get(key) ?? []), t]);
    }
    for (const row of lines.values()) {
      const sorted = [...row].sort((a, b) => a.x - b.x);
      for (let i = 0; i < sorted.length; i++) {
        const end = sorted[i].x + sorted[i].s.length * 2;
        expect(end, `off page: ${sorted[i].s}`).toBeLessThanOrEqual(210);
        if (i + 1 < sorted.length) {
          // At least 2 mm of white space between neighbours, so columns never read as one word.
          expect(end + 2, `"${sorted[i].s}" runs into "${sorted[i + 1].s}"`).toBeLessThanOrEqual(sorted[i + 1].x);
        }
      }
    }
  });

  it('wraps a long course title instead of cutting it off', () => {
    const title = 'Advanced Topics in Large-Scale Distributed Systems: Consensus, Replication and Fault Tolerance';
    const { texts } = render(statement({ courses: [{ title, enrolled: 3, externalPaid: 2, amount: '₹3,500', status: 'LIVE' }] }));
    const all = texts.map((t) => t.s).join('');
    expect(all.replace(/\s/g, '')).toContain(title.replace(/\s/g, ''));
    expect(texts.some((t) => t.s.endsWith('...'))).toBe(false);
  });

  it('continues on a new page when there are many courses', () => {
    const courses = Array.from({ length: 40 }, (_, i) => ({ title: `Course ${i + 1}`, enrolled: i, externalPaid: i, amount: '₹100', status: 'LIVE' }));
    const rec = render(statement({ courses }));
    expect(rec.doc.addPage).toHaveBeenCalled();
    expect(rec.texts.map((t) => t.s)).toContain('Course 40');
    for (const t of rec.texts) expect(t.y, `below the page: ${t.s}`).toBeLessThanOrEqual(297 - 18);
  });

  it('includes payout history when given, and states the real payout policy (A-22)', () => {
    const { texts } = render(statement({ payouts: [{ month: '2026-10', date: '01 Nov 2026', amount: '₹750', status: 'PAID' }] }));
    const all = texts.map((t) => t.s).join('\n');
    const joined = texts.map((t) => t.s).join(''); // the fake wraps mid-word
    expect(all).toContain('2026-10');
    expect(joined).toMatch(/INR 500/);
    expect(all).not.toMatch(/automatically|bank transfer/i);
  });
});
