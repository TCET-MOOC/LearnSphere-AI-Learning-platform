import { Injectable } from '@angular/core';
import { jsPDF } from 'jspdf';
import { RECEIPT_COPY } from '@core/content/receipt.content';
import { STATEMENT_COPY } from '@core/content/statement.content';
import { Receipt, pdfSafe } from './receipt';

export type { Receipt } from './receipt';

export interface StatementData {
  teacherName: string;
  teacherEmail: string;
  statementPeriod: string;
  generatedDate: string;
  thisMonthTotal: string;
  totalEarned: string;
  pendingPayout: string;
  courses: Array<{
    title: string;
    enrolled: number;
    externalPaid: number;
    amount: string;
    status: string;
  }>;
  payouts?: Array<{
    month: string;
    date: string;
    amount: string;
    status: string;
  }>;
}

const BLACK = 0;
const MID_GREY = 90;
const RULE_GREY = 160;
const MM_PER_PT = 0.3528;
const LINE_SPACING = 1.35;
const MARGIN = 18;

interface Column {
  label: string;
  width: number;
  align: 'left' | 'right';
}

/**
 * Monochrome, ASCII-only page writer shared by the receipt and the statement.
 * Keeps a running cursor, wraps every value to its column, right-aligns by measured
 * width (never by a fixed coordinate) and starts a new page before running off the end.
 * It draws only text and rules: no filled boxes, so no shape can inherit a text colour.
 */
class PdfWriter {
  readonly pageWidth: number;
  readonly pageHeight: number;
  readonly left = MARGIN;
  readonly right: number;
  readonly contentWidth: number;
  y = MARGIN + 4;

  constructor(private readonly doc: jsPDF) {
    this.pageWidth = doc.internal.pageSize.getWidth();
    this.pageHeight = doc.internal.pageSize.getHeight();
    this.right = this.pageWidth - MARGIN;
    this.contentWidth = this.right - this.left;
  }

  lineHeight(size: number): number {
    return size * MM_PER_PT * LINE_SPACING;
  }

  style(weight: 'normal' | 'bold', size: number, grey = BLACK): void {
    this.doc.setFont('helvetica', weight);
    this.doc.setFontSize(size);
    this.doc.setTextColor(grey, grey, grey);
  }

  wrap(value: string, width: number): string[] {
    return this.doc.splitTextToSize(pdfSafe(value), width);
  }

  /** Starts a new page when `height` would not fit; returns true when it did. */
  ensureSpace(height: number): boolean {
    if (this.y + height > this.pageHeight - MARGIN) {
      this.doc.addPage();
      this.y = MARGIN + 4;
      return true;
    }
    return false;
  }

  text(value: string, x: number, top = this.y): void {
    this.doc.text(pdfSafe(value), x, top);
  }

  /** Draws `lines` right-aligned so each ends at `edge`. */
  textRight(lines: string[] | string, edge: number, top = this.y, size = 10): void {
    const list = Array.isArray(lines) ? lines : [pdfSafe(lines)];
    list.forEach((line, i) => this.doc.text(line, edge - this.doc.getTextWidth(line), top + i * this.lineHeight(size)));
  }

  lines(lines: string[], x: number, top: number, size: number): void {
    lines.forEach((line, i) => this.doc.text(line, x, top + i * this.lineHeight(size)));
  }

  rule(at = this.y): void {
    this.doc.setDrawColor(RULE_GREY, RULE_GREY, RULE_GREY);
    this.doc.setLineWidth(0.2);
    this.doc.line(this.left, at, this.right, at);
  }

  header(title: string, issuer: string): void {
    this.style('bold', 18);
    this.text('LEARNSPHERE', this.left);
    this.style('bold', 14);
    this.textRight(title, this.right);
    this.y += this.lineHeight(18);
    this.style('normal', 9, MID_GREY);
    this.text(issuer, this.left);
    this.y += 4;
    this.rule();
    this.y += 8;
  }

  /** Label column on the left, wrapped values beside it. */
  field(label: string, values: string[], labelWidth = 45): void {
    const valueX = this.left + labelWidth;
    const valueLines = values.filter(v => !!v).flatMap(v => this.wrap(v, this.right - valueX));
    if (valueLines.length === 0) return;
    const height = valueLines.length * this.lineHeight(10);
    this.ensureSpace(height + 2);
    this.style('bold', 9, MID_GREY);
    this.text(label, this.left);
    this.style('normal', 10);
    this.lines(valueLines, valueX, this.y, 10);
    this.y += height + 2;
  }

  heading(value: string): void {
    this.ensureSpace(14);
    this.style('bold', 11);
    this.text(value, this.left);
    this.y += 6;
  }

  /** A table whose cells wrap inside their column and whose rows grow to fit; the header repeats on a new page. */
  table(columns: Column[], rows: string[][], size = 9): void {
    // Each interior column edge gets padding, so neighbouring cells never touch.
    const pad = 3;
    const last = columns.length - 1;
    const cells = columns.map((c, i, all) => {
      const start = this.left + all.slice(0, i).reduce((sum, col) => sum + col.width, 0);
      const from = start + (i === 0 ? 0 : pad);
      const to = start + c.width - (i === last ? 0 : pad);
      return { ...c, from, to };
    });
    const cellLines = (value: string, c: (typeof cells)[number]) => this.wrap(value, c.to - c.from);
    const draw = (lines: string[], c: (typeof cells)[number], textSize: number) =>
      c.align === 'right' ? this.textRight(lines, c.to, this.y, textSize) : this.lines(lines, c.from, this.y, textSize);

    const drawHeader = () => {
      this.rule();
      this.y += 5;
      this.style('bold', 8.5, MID_GREY);
      const labels = cells.map(c => cellLines(c.label, c));
      cells.forEach((c, i) => draw(labels[i], c, 8.5));
      this.y += (Math.max(...labels.map(l => l.length)) - 1) * this.lineHeight(8.5) + 3;
      this.rule();
      this.y += 5;
    };

    this.ensureSpace(24);
    drawHeader();
    for (const row of rows) {
      this.style('normal', size); // measure in the font the row is drawn in
      const lines = cells.map((c, i) => cellLines(row[i] ?? '', c));
      const height = Math.max(...lines.map(l => l.length)) * this.lineHeight(size);
      if (this.ensureSpace(height + 3)) {
        drawHeader();
        this.style('normal', size);
      }
      cells.forEach((c, i) => draw(lines[i], c, size));
      this.y += height + 3;
    }
    this.rule(this.y - 2);
    this.y += 4;
  }

  paragraphs(texts: readonly string[], size = 8.5, grey = MID_GREY): void {
    this.style('normal', size, grey);
    for (const value of texts) {
      const lines = this.wrap(value, this.contentWidth);
      this.ensureSpace(lines.length * this.lineHeight(size));
      this.lines(lines, this.left, this.y, size);
      this.y += lines.length * this.lineHeight(size) + 1.5;
    }
  }
}

@Injectable({
  providedIn: 'root'
})
export class PdfExportService {

  /** Seam for tests: returns the document the generators draw on. */
  protected createDoc(): jsPDF {
    return new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
  }

  /**
   * Student payment receipt (W6-13 / AUD-01, audit.md A1-A5).
   * Monochrome, ASCII-only Helvetica; every value wraps inside its column and
   * rows grow to fit, so long titles or e-mails never overlap or leave the page.
   */
  generateStudentReceipt(receipt: Receipt): void {
    const doc = this.createDoc();
    const w = new PdfWriter(doc);
    const amountColWidth = 45;

    w.header(RECEIPT_COPY.documentTitle, RECEIPT_COPY.issuer);
    w.field(RECEIPT_COPY.receiptNumber, [receipt.receiptNumber]);
    w.field(RECEIPT_COPY.paidAt, [receipt.paidAtLabel]);
    w.field(RECEIPT_COPY.status, [receipt.statusLabel]);
    w.field(RECEIPT_COPY.billedTo, [receipt.studentName, receipt.studentEmail]);
    w.field(RECEIPT_COPY.gateway, [RECEIPT_COPY.gatewayName]);
    w.field(RECEIPT_COPY.orderId, [receipt.orderId]);
    w.field(RECEIPT_COPY.paymentId, [receipt.paymentId]);
    w.y += 4;

    // Line item
    w.ensureSpace(12);
    w.rule();
    w.y += 5;
    w.style('bold', 9, MID_GREY);
    w.text(RECEIPT_COPY.item, w.left);
    w.textRight(RECEIPT_COPY.amount, w.right);
    w.y += 3;
    w.rule();
    w.y += 6;

    const itemWidth = w.contentWidth - amountColWidth - 4;
    const titleLines = w.wrap(receipt.courseTitle, itemWidth);
    const deptLines = receipt.courseDepartment ? w.wrap(receipt.courseDepartment, itemWidth) : [];
    const rowHeight = titleLines.length * w.lineHeight(10) + deptLines.length * w.lineHeight(9);
    w.ensureSpace(rowHeight + 4);
    w.style('bold', 10);
    w.lines(titleLines, w.left, w.y, 10);
    w.textRight(receipt.amountLabel, w.right);
    w.style('normal', 9, MID_GREY);
    w.lines(deptLines, w.left, w.y + titleLines.length * w.lineHeight(10), 9);
    w.y += rowHeight + 2;
    w.rule();
    w.y += 7;

    // Total
    w.ensureSpace(10);
    w.style('bold', 11);
    w.text(RECEIPT_COPY.total, w.left);
    w.textRight(receipt.amountLabel, w.right);
    w.y += 12;

    w.paragraphs([RECEIPT_COPY.notice, RECEIPT_COPY.footer]);

    doc.save(`LearnSphere_Receipt_${pdfSafe(receipt.receiptNumber)}.pdf`);
  }

  /**
   * Teacher royalty statement: same monochrome, ASCII-only layout as the receipt.
   * Columns are laid out by width (no overlap), course titles wrap instead of being
   * cut, long statements continue on new pages, and the policy text follows A-22.
   */
  generateTeacherStatement(data: StatementData): void {
    const doc = this.createDoc();
    const w = new PdfWriter(doc);

    w.header(STATEMENT_COPY.documentTitle, STATEMENT_COPY.issuer);
    w.field(STATEMENT_COPY.instructor, [data.teacherName, data.teacherEmail]);
    w.field(STATEMENT_COPY.period, [data.statementPeriod || 'All time']);
    w.field(STATEMENT_COPY.generated, [data.generatedDate]);
    w.y += 4;

    // Summary: three figures side by side
    const summary: Array<[string, string]> = [
      [STATEMENT_COPY.thisMonth, data.thisMonthTotal],
      [STATEMENT_COPY.totalEarned, data.totalEarned],
      [STATEMENT_COPY.unpaidBalance, data.pendingPayout]
    ];
    const colWidth = w.contentWidth / summary.length;
    w.ensureSpace(20);
    w.rule();
    w.y += 6;
    summary.forEach(([label, value], i) => {
      const x = w.left + i * colWidth;
      w.style('bold', 8, MID_GREY);
      w.lines(w.wrap(label, colWidth - 4), x, w.y, 8);
      w.style('bold', 14);
      w.lines(w.wrap(value, colWidth - 4), x, w.y + 7, 14);
    });
    w.y += 16;
    w.rule();
    w.y += 10;

    w.heading(STATEMENT_COPY.coursesHeading);
    w.table(
      [
        { label: STATEMENT_COPY.colCourse, width: 54, align: 'left' },
        { label: STATEMENT_COPY.colEnrolled, width: 20, align: 'right' },
        { label: STATEMENT_COPY.colPaid, width: 30, align: 'right' },
        { label: STATEMENT_COPY.colStatus, width: 40, align: 'left' },
        { label: STATEMENT_COPY.colRoyalty, width: w.contentWidth - 144, align: 'right' }
      ],
      data.courses.map(c => [c.title, String(c.enrolled), String(c.externalPaid), c.status, c.amount])
    );

    if (data.payouts?.length) {
      w.heading(STATEMENT_COPY.payoutsHeading);
      w.table(
        [
          { label: STATEMENT_COPY.colMonth, width: 44, align: 'left' },
          { label: STATEMENT_COPY.colPaidOn, width: 50, align: 'left' },
          { label: STATEMENT_COPY.colStatus, width: 40, align: 'left' },
          { label: STATEMENT_COPY.colAmount, width: w.contentWidth - 134, align: 'right' }
        ],
        data.payouts.map(p => [p.month, p.date, p.status, p.amount])
      );
    }

    w.heading(STATEMENT_COPY.policyHeading);
    w.paragraphs([...STATEMENT_COPY.policy, STATEMENT_COPY.footer]);

    doc.save(`LearnSphere_Royalty_Statement_${new Date().toISOString().slice(0, 10)}.pdf`);
  }
}
