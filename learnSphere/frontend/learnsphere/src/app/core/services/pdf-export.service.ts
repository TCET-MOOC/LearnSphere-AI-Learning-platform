import { Injectable } from '@angular/core';
import { jsPDF } from 'jspdf';

export interface ReceiptData {
  receiptNumber: string;
  orderId: string;
  paymentId?: string;
  studentName: string;
  studentEmail: string;
  courseTitle: string;
  courseDepartment?: string;
  amount: number; // in Rupees
  currency: string;
  paymentDate: string;
  status: string;
}

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

@Injectable({
  providedIn: 'root'
})
export class PdfExportService {

  constructor() {}

  /**
   * Generates a sleek, executive-quality PDF Payment Receipt for Students
   */
  generateStudentReceipt(data: ReceiptData): void {
    const doc = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4'
    });

    const pageWidth = doc.internal.pageSize.getWidth();
    const margin = 18;

    // Header Background Accent Bar
    doc.setFillColor(99, 102, 241); // Indigo-600 #6366f1
    doc.rect(0, 0, pageWidth, 6, 'F');

    // Corporate Header
    doc.setFillColor(248, 250, 252);
    doc.rect(margin, 14, pageWidth - (margin * 2), 34, 'F');
    doc.setDrawColor(226, 232, 240);
    doc.roundedRect(margin, 14, pageWidth - (margin * 2), 34, 3, 3, 'S');

    // LearnSphere Brand Logo Title
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(22);
    doc.setTextColor(79, 70, 229); // #4f46e5
    doc.text('LEARNSPHERE', margin + 6, 26);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(100, 116, 139);
    doc.text('AI-Powered Next-Gen Online Learning Platform', margin + 6, 32);
    doc.text('support@learnsphere.io  |  https://learnsphere.io', margin + 6, 38);

    // Right-aligned Document Label
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(14);
    doc.setTextColor(15, 23, 42);
    doc.text('TAX INVOICE / RECEIPT', pageWidth - margin - 6, 26, { align: 'right' });

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(100, 116, 139);
    doc.text(`Receipt #: ${data.receiptNumber}`, pageWidth - margin - 6, 33, { align: 'right' });
    doc.text(`Date: ${data.paymentDate}`, pageWidth - margin - 6, 39, { align: 'right' });

    // Status Banner (Green Verified Pill)
    doc.setFillColor(236, 253, 245);
    doc.setDrawColor(167, 243, 208);
    doc.roundedRect(margin, 54, pageWidth - (margin * 2), 12, 2, 2, 'FD');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(10);
    doc.setTextColor(6, 95, 70);
    doc.text('✓  PAYMENT VERIFIED & COURSE ENROLLMENT CONFIRMED', margin + 6, 62);

    // Student & Gateway Meta Grid
    const colWidth = (pageWidth - (margin * 2) - 8) / 2;
    const boxY = 72;
    const boxH = 34;

    // Billed To Box
    doc.setFillColor(248, 250, 252);
    doc.setDrawColor(226, 232, 240);
    doc.roundedRect(margin, boxY, colWidth, boxH, 2, 2, 'FD');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8.5);
    doc.setTextColor(100, 116, 139);
    doc.text('BILLED TO (STUDENT)', margin + 5, boxY + 7);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(11);
    doc.setTextColor(15, 23, 42);
    doc.text(data.studentName || 'Student', margin + 5, boxY + 15);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(71, 85, 105);
    doc.text(data.studentEmail || 'student@learnsphere.io', margin + 5, boxY + 22);
    doc.text('Role: Verified Student Account', margin + 5, boxY + 28);

    // Payment Reference Box
    const col2X = margin + colWidth + 8;
    doc.roundedRect(col2X, boxY, colWidth, boxH, 2, 2, 'FD');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8.5);
    doc.setTextColor(100, 116, 139);
    doc.text('TRANSACTION REFERENCE', col2X + 5, boxY + 7);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(15, 23, 42);
    doc.text(`Gateway: Razorpay Checkout`, col2X + 5, boxY + 15);
    doc.text(`Order ID: ${data.orderId}`, col2X + 5, boxY + 22);
    if (data.paymentId) {
      doc.text(`Payment ID: ${data.paymentId}`, col2X + 5, boxY + 28);
    } else {
      doc.text(`Status: Completed & Captured`, col2X + 5, boxY + 28);
    }

    // Line Items Table
    const tableY = 114;
    doc.setFillColor(79, 70, 229);
    doc.rect(margin, tableY, pageWidth - (margin * 2), 9, 'F');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(9);
    doc.setTextColor(255, 255, 255);
    doc.text('Item / Course Description', margin + 5, tableY + 6);
    doc.text('Department', margin + 105, tableY + 6);
    doc.text('Amount (INR)', pageWidth - margin - 5, tableY + 6, { align: 'right' });

    // Table Content Row
    const rowY = tableY + 9;
    const rowH = 18;
    doc.setFillColor(255, 255, 255);
    doc.setDrawColor(226, 232, 240);
    doc.rect(margin, rowY, pageWidth - (margin * 2), rowH, 'FD');

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(10);
    doc.setTextColor(15, 23, 42);
    doc.text(data.courseTitle || 'Course Access', margin + 5, rowY + 7);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8.5);
    doc.setTextColor(100, 116, 139);
    doc.text('Full course curriculum, interactive lectures & certificate', margin + 5, rowY + 13);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(71, 85, 105);
    doc.text(data.courseDepartment || 'Computer Science', margin + 105, rowY + 9);

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(11);
    doc.setTextColor(15, 23, 42);
    doc.text(`₹${data.amount.toLocaleString('en-IN')}`, pageWidth - margin - 5, rowY + 10, { align: 'right' });

    // Total Calculation Box
    const totalBoxY = rowY + rowH + 6;
    const totalBoxW = 75;
    const totalBoxX = pageWidth - margin - totalBoxW;

    doc.setFillColor(248, 250, 252);
    doc.setDrawColor(226, 232, 240);
    doc.roundedRect(totalBoxX, totalBoxY, totalBoxW, 28, 2, 2, 'FD');

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(100, 116, 139);
    doc.text('Subtotal:', totalBoxX + 5, totalBoxY + 7);
    doc.text(`₹${data.amount.toLocaleString('en-IN')}`, pageWidth - margin - 5, totalBoxY + 7, { align: 'right' });

    doc.text('Taxes (GST Included):', totalBoxX + 5, totalBoxY + 13);
    doc.text('₹0.00', pageWidth - margin - 5, totalBoxY + 13, { align: 'right' });

    doc.setDrawColor(203, 213, 225);
    doc.line(totalBoxX + 5, totalBoxY + 17, pageWidth - margin - 5, totalBoxY + 17);

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(11);
    doc.setTextColor(79, 70, 229);
    doc.text('Total Paid:', totalBoxX + 5, totalBoxY + 24);
    doc.text(`₹${data.amount.toLocaleString('en-IN')} INR`, pageWidth - margin - 5, totalBoxY + 24, { align: 'right' });

    // Terms & Security Signature
    const footerY = 240;
    doc.setDrawColor(226, 232, 240);
    doc.line(margin, footerY, pageWidth - margin, footerY);

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8.5);
    doc.setTextColor(15, 23, 42);
    doc.text('Important Information:', margin, footerY + 7);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8);
    doc.setTextColor(100, 116, 139);
    doc.text('• This receipt confirms lifetime access to the purchased course on LearnSphere.', margin, footerY + 12);
    doc.text('• For questions or tax inquiries, contact billing@learnsphere.io with your Receipt Number.', margin, footerY + 17);

    doc.setFont('helvetica', 'italic');
    doc.setFontSize(7.5);
    doc.setTextColor(148, 163, 184);
    doc.text('This is an electronically generated receipt issued by LearnSphere AI Learning Platform. No signature is required.', pageWidth / 2, footerY + 30, { align: 'center' });

    doc.save(`LearnSphere_Receipt_${data.receiptNumber}.pdf`);
  }

  /**
   * Generates a comprehensive, branded PDF Royalty & Earnings Statement for Teachers
   */
  generateTeacherStatement(data: StatementData): void {
    const doc = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4'
    });

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
    doc.text('LEARNSPHERE', margin + 6, 26);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(100, 116, 139);
    doc.text('Instructor Monetization & Payout Division', margin + 6, 32);
    doc.text('finance@learnsphere.io  |  https://learnsphere.io', margin + 6, 38);

    // Document Title (Right)
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(13);
    doc.setTextColor(15, 23, 42);
    doc.text('ROYALTIES & EARNINGS STATEMENT', pageWidth - margin - 6, 26, { align: 'right' });

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    doc.setTextColor(100, 116, 139);
    doc.text(`Period: ${data.statementPeriod || 'All-Time'}`, pageWidth - margin - 6, 33, { align: 'right' });
    doc.text(`Generated: ${data.generatedDate}`, pageWidth - margin - 6, 39, { align: 'right' });

    // Teacher Summary Card
    doc.setFillColor(248, 250, 252);
    doc.setDrawColor(226, 232, 240);
    doc.roundedRect(margin, 54, pageWidth - (margin * 2), 16, 2, 2, 'FD');

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(9.5);
    doc.setTextColor(15, 23, 42);
    doc.text(`Instructor: ${data.teacherName}`, margin + 5, 62);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(100, 116, 139);
    doc.text(`Email: ${data.teacherEmail}`, margin + 5, 67);

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
    doc.text('THIS MONTH EARNINGS', margin + 4, statY + 6);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(14);
    doc.setTextColor(15, 23, 42);
    doc.text(data.thisMonthTotal, margin + 4, statY + 16);

    // Stat 2: Total Earned
    const stat2X = margin + statW + 6;
    doc.roundedRect(stat2X, statY, statW, statH, 2, 2, 'FD');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8);
    doc.setTextColor(100, 116, 139);
    doc.text('TOTAL EARNED (LIFETIME)', stat2X + 4, statY + 6);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(14);
    doc.setTextColor(79, 70, 229);
    doc.text(data.totalEarned, stat2X + 4, statY + 16);

    // Stat 3: Next Payout Balance
    const stat3X = stat2X + statW + 6;
    doc.roundedRect(stat3X, statY, statW, statH, 2, 2, 'FD');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8);
    doc.setTextColor(100, 116, 139);
    doc.text('PENDING PAYOUT BALANCE', stat3X + 4, statY + 6);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(14);
    doc.setTextColor(5, 150, 105);
    doc.text(data.pendingPayout, stat3X + 4, statY + 16);

    // Course Monetization Breakdown Table
    let tableY = 104;
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(11);
    doc.setTextColor(15, 23, 42);
    doc.text('Course Revenue & Royalty Breakdown', margin, tableY);

    tableY += 4;
    doc.setFillColor(79, 70, 229);
    doc.rect(margin, tableY, pageWidth - (margin * 2), 8, 'F');
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8.5);
    doc.setTextColor(255, 255, 255);
    doc.text('Course Title', margin + 4, tableY + 5.5);
    doc.text('Enrolled', margin + 90, tableY + 5.5);
    doc.text('Paid Students', margin + 115, tableY + 5.5);
    doc.text('Status', margin + 145, tableY + 5.5);
    doc.text('Royalty Cut', pageWidth - margin - 4, tableY + 5.5, { align: 'right' });

    let currentY = tableY + 8;
    for (const c of data.courses) {
      doc.setFillColor(255, 255, 255);
      doc.setDrawColor(241, 245, 249);
      doc.rect(margin, currentY, pageWidth - (margin * 2), 10, 'FD');

      doc.setFont('helvetica', 'normal');
      doc.setFontSize(8.5);
      doc.setTextColor(15, 23, 42);
      doc.text(c.title.length > 40 ? c.title.substring(0, 38) + '...' : c.title, margin + 4, currentY + 6.5);

      doc.setTextColor(71, 85, 105);
      doc.text(String(c.enrolled), margin + 90, currentY + 6.5);
      doc.text(String(c.externalPaid), margin + 115, currentY + 6.5);
      doc.text(c.status, margin + 145, currentY + 6.5);

      doc.setFont('helvetica', 'bold');
      doc.setTextColor(79, 70, 229);
      doc.text(c.amount, pageWidth - margin - 4, currentY + 6.5, { align: 'right' });

      currentY += 10;
    }

    // Payout Schedule & Policies Footer
    const footerY = 245;
    doc.setDrawColor(226, 232, 240);
    doc.line(margin, footerY, pageWidth - margin, footerY);

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(8.5);
    doc.setTextColor(15, 23, 42);
    doc.text('Payout Policy & Notes:', margin, footerY + 6);

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8);
    doc.setTextColor(100, 116, 139);
    doc.text('• Instructors receive a standard 70% net royalty share on all direct paid course sales.', margin, footerY + 11);
    doc.text('• Monthly payouts are processed automatically on the 1st of every calendar month via bank transfer.', margin, footerY + 16);

    doc.setFont('helvetica', 'italic');
    doc.setFontSize(7.5);
    doc.setTextColor(148, 163, 184);
    doc.text('Official statement generated by LearnSphere AI Platform • Confidential', pageWidth / 2, footerY + 28, { align: 'center' });

    doc.save(`LearnSphere_Royalties_Statement_${new Date().toISOString().slice(0, 10)}.pdf`);
  }
}
