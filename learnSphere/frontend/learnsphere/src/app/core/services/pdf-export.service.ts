import { Injectable } from '@angular/core';
import { jsPDF } from 'jspdf';
import { RECEIPT_COPY } from '@core/content/receipt.content';
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
    const pageWidth = doc.internal.pageSize.getWidth();
    const pageHeight = doc.internal.pageSize.getHeight();
    const margin = 18;
    const right = pageWidth - margin;
    const contentWidth = right - margin;
    const labelWidth = 45;
    const valueX = margin + labelWidth;
    const valueWidth = right - valueX;
    const amountColWidth = 45;
    let y = margin + 4;

    const lineHeight = (size: number) => size * MM_PER_PT * LINE_SPACING;
    const style = (weight: 'normal' | 'bold', size: number, grey = BLACK) => {
      doc.setFont('helvetica', weight);
      doc.setFontSize(size);
      doc.setTextColor(grey, grey, grey);
    };
    const wrap = (value: string, width: number): string[] => doc.splitTextToSize(pdfSafe(value), width);
    const ensureSpace = (height: number) => {
      if (y + height > pageHeight - margin) {
        doc.addPage();
        y = margin + 4;
      }
    };
    const drawLines = (lines: string[], x: number, top: number, size: number) =>
      lines.forEach((line, i) => doc.text(line, x, top + i * lineHeight(size)));
    const drawRight = (value: string, top: number) => {
      const safe = pdfSafe(value);
      doc.text(safe, right - doc.getTextWidth(safe), top);
    };
    const rule = (at: number) => {
      doc.setDrawColor(RULE_GREY, RULE_GREY, RULE_GREY);
      doc.setLineWidth(0.2);
      doc.line(margin, at, right, at);
    };
    const field = (label: string, values: string[]) => {
      const valueLines = values.filter(v => !!v).flatMap(v => wrap(v, valueWidth));
      if (valueLines.length === 0) return;
      const height = valueLines.length * lineHeight(10);
      ensureSpace(height + 2);
      style('bold', 9, MID_GREY);
      doc.text(pdfSafe(label), margin, y);
      style('normal', 10);
      drawLines(valueLines, valueX, y, 10);
      y += height + 2;
    };

    // Header
    style('bold', 18);
    doc.text('LEARNSPHERE', margin, y);
    style('bold', 14);
    drawRight(RECEIPT_COPY.documentTitle, y);
    y += lineHeight(18);
    style('normal', 9, MID_GREY);
    doc.text(pdfSafe(RECEIPT_COPY.issuer), margin, y);
    y += 4;
    rule(y);
    y += 8;

    // Receipt identity and parties
    field(RECEIPT_COPY.receiptNumber, [receipt.receiptNumber]);
    field(RECEIPT_COPY.paidAt, [receipt.paidAtLabel]);
    field(RECEIPT_COPY.status, [receipt.statusLabel]);
    field(RECEIPT_COPY.billedTo, [receipt.studentName, receipt.studentEmail]);
    field(RECEIPT_COPY.gateway, [RECEIPT_COPY.gatewayName]);
    field(RECEIPT_COPY.orderId, [receipt.orderId]);
    field(RECEIPT_COPY.paymentId, [receipt.paymentId]);
    y += 4;

    // Line item
    ensureSpace(12);
    rule(y);
    y += 5;
    style('bold', 9, MID_GREY);
    doc.text(pdfSafe(RECEIPT_COPY.item), margin, y);
    drawRight(RECEIPT_COPY.amount, y);
    y += 3;
    rule(y);
    y += 6;

    const itemWidth = contentWidth - amountColWidth - 4;
    const titleLines = wrap(receipt.courseTitle, itemWidth);
    const deptLines = receipt.courseDepartment ? wrap(receipt.courseDepartment, itemWidth) : [];
    const rowHeight = titleLines.length * lineHeight(10) + deptLines.length * lineHeight(9);
    ensureSpace(rowHeight + 4);
    style('bold', 10);
    drawLines(titleLines, margin, y, 10);
    drawRight(receipt.amountLabel, y);
    style('normal', 9, MID_GREY);
    drawLines(deptLines, margin, y + titleLines.length * lineHeight(10), 9);
    y += rowHeight + 2;
    rule(y);
    y += 7;

    // Total
    ensureSpace(10);
    style('bold', 11);
    doc.text(pdfSafe(RECEIPT_COPY.total), margin, y);
    drawRight(receipt.amountLabel, y);
    y += 12;

    // Notice and footer
    style('normal', 8.5, MID_GREY);
    for (const text of [RECEIPT_COPY.notice, RECEIPT_COPY.footer]) {
      const lines = wrap(text, contentWidth);
      ensureSpace(lines.length * lineHeight(8.5));
      drawLines(lines, margin, y, 8.5);
      y += lines.length * lineHeight(8.5) + 1.5;
    }

    doc.save(`LearnSphere_Receipt_${pdfSafe(receipt.receiptNumber)}.pdf`);
  }

  /**
   * Generates a comprehensive, branded PDF Royalty & Earnings Statement for Teachers.
   * All text is passed through pdfSafe: Helvetica has no rupee sign or bullet (audit.md A1).
   */
  generateTeacherStatement(data: StatementData): void {
    const doc = this.createDoc();
    const text = (value: string, x: number, y: number, options?: { align?: 'left' | 'center' | 'right' }) =>
      doc.text(pdfSafe(value), x, y, options);

    const pageWidth = doc.internal.pageSize.getWidth();
    const margin = 18;

    // Top Accent Stripe
    doc.setFillColor(79, 70, 229); // #4f46e5
    doc.rect(0, 0, pageWidth, 6, 'F');

    // Corporate Header Box
    doc.setFillColor(248, 250, 252);
    doc.setDrawColor(226, 232, 240);
    doc.roundedRect(margin, 14, pageWidth - (margin * 2), 34, 3, 3, 'FD');

    // Brand Name
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(22);
    doc.setTextColor(79, 70, 229);
    text('LEARNSPHERE', margin + 6, 26);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(100, 116, 139);
    text('Instructor Monetization & Payout Division', margin + 6, 32);
    text('finance@learnsphere.io  |  https://learnsphere.io', margin + 6, 38);

    // Document Title (Right)
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(13);
    doc.setTextColor(15, 23, 42);
    text('ROYALTIES & EARNINGS STATEMENT', pageWidth - margin - 6, 26, { align: 'right' });

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(100, 116, 139);
    text(`Period: ${data.statementPeriod || 'All-Time'}`, pageWidth - margin - 6, 33, { align: 'right' });
    text(`Generated: ${data.generatedDate}`, pageWidth - margin - 6, 39, { align: 'right' });

    // Teacher Summary Card
    doc.setFillColor(248, 250, 252);
    doc.setDrawColor(226, 232, 240);
    doc.roundedRect(margin, 54, pageWidth - (margin * 2), 16, 2, 2, 'FD');

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(9.5);
    doc.setTextColor(15, 23, 42);
    text(`Instructor: ${data.teacherName}`, margin + 5, 62);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(100, 116, 139);
    text(`Email: ${data.teacherEmail}`, margin + 5, 67);

    // Metric Summary Highlights Grid
    const statW = (pageWidth - (margin * 2) - 12) / 3;
    const statY = 75;
    const statH = 22;

    // Stat 1: This Month
    doc.setFillColor(241, 245, 249);
    doc.setDrawColor(203, 213, 225);
    doc.roundedRect(margin, statY, statW, statH, 2, 2, 'FD');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8);
    doc.setTextColor(100, 116, 139);
    text('THIS MONTH EARNINGS', margin + 4, statY + 6);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(14);
    doc.setTextColor(15, 23, 42);
    text(data.thisMonthTotal, margin + 4, statY + 16);

    // Stat 2: Total Earned
    const stat2X = margin + statW + 6;
    doc.roundedRect(stat2X, statY, statW, statH, 2, 2, 'FD');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8);
    doc.setTextColor(100, 116, 139);
    text('TOTAL EARNED (LIFETIME)', stat2X + 4, statY + 6);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(14);
    doc.setTextColor(79, 70, 229);
    text(data.totalEarned, stat2X + 4, statY + 16);

    // Stat 3: Next Payout Balance
    const stat3X = stat2X + statW + 6;
    doc.roundedRect(stat3X, statY, statW, statH, 2, 2, 'FD');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8);
    doc.setTextColor(100, 116, 139);
    text('PENDING PAYOUT BALANCE', stat3X + 4, statY + 6);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(14);
    doc.setTextColor(5, 150, 105);
    text(data.pendingPayout, stat3X + 4, statY + 16);

    // Course Monetization Breakdown Table
    let tableY = 104;
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(11);
    doc.setTextColor(15, 23, 42);
    text('Course Revenue & Royalty Breakdown', margin, tableY);

    tableY += 4;
    doc.setFillColor(79, 70, 229);
    doc.rect(margin, tableY, pageWidth - (margin * 2), 8, 'F');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8.5);
    doc.setTextColor(255, 255, 255);
    text('Course Title', margin + 4, tableY + 5.5);
    text('Enrolled', margin + 90, tableY + 5.5);
    text('Paid Students', margin + 115, tableY + 5.5);
    text('Status', margin + 145, tableY + 5.5);
    text('Royalty Cut', pageWidth - margin - 4, tableY + 5.5, { align: 'right' });

    let currentY = tableY + 8;
    for (const c of data.courses) {
      doc.setFillColor(255, 255, 255);
      doc.setDrawColor(241, 245, 249);
      doc.rect(margin, currentY, pageWidth - (margin * 2), 10, 'FD');

      doc.setFont('helvetica', 'normal');
      doc.setFontSize(8.5);
      doc.setTextColor(15, 23, 42);
      text(c.title.length > 40 ? c.title.substring(0, 38) + '...' : c.title, margin + 4, currentY + 6.5);

      doc.setTextColor(71, 85, 105);
      text(String(c.enrolled), margin + 90, currentY + 6.5);
      text(String(c.externalPaid), margin + 115, currentY + 6.5);
      text(c.status, margin + 145, currentY + 6.5);

      doc.setFont('helvetica', 'bold');
      doc.setTextColor(79, 70, 229);
      text(c.amount, pageWidth - margin - 4, currentY + 6.5, { align: 'right' });

      currentY += 10;
    }

    // Payout Schedule & Policies Footer
    const footerY = 245;
    doc.setDrawColor(226, 232, 240);
    doc.line(margin, footerY, pageWidth - margin, footerY);

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8.5);
    doc.setTextColor(15, 23, 42);
    text('Payout Policy & Notes:', margin, footerY + 6);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8);
    doc.setTextColor(100, 116, 139);
    text('- Instructors receive a standard 70% net royalty share on all direct paid course sales.', margin, footerY + 11);
    text('- Monthly payouts are processed automatically on the 1st of every calendar month via bank transfer.', margin, footerY + 16);

    doc.setFont('helvetica', 'italic');
    doc.setFontSize(7.5);
    doc.setTextColor(148, 163, 184);
    text('Official statement generated by LearnSphere AI Platform - Confidential', pageWidth / 2, footerY + 28, { align: 'center' });

    doc.save(`LearnSphere_Royalties_Statement_${new Date().toISOString().slice(0, 10)}.pdf`);
  }
}
