import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '@core/auth/auth.service';
import { PaymentsService, Order } from '../services/payments.service';
import { StudentService } from '../services/student.service';
import { Course } from '@core/models/course.model';
import { PdfExportService } from '@core/services/pdf-export.service';
import { LedgerPayment, Receipt } from '@core/services/receipt';
import { RECEIPT_COPY } from '@core/content/receipt.content';
import { LucideAngularModule } from 'lucide-angular';
import { HistoryRow, HistorySummary, PaymentStatus, mergeHistory, summarize } from './payment-history';

/** One row of the purchases list (W6-14): see payment-history.ts for the rules. */
export type OrderWithCourse = HistoryRow;

export type PaymentFilterType = 'ALL' | PaymentStatus;

@Component({
  selector: 'app-purchases',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  templateUrl: './purchases.component.html',
  styleUrls: ['./purchases.component.scss']
})
export class PurchasesComponent implements OnInit {
  orders: OrderWithCourse[] = [];
  summary: HistorySummary = summarize([]);
  loading = true;
  selectedFilter: PaymentFilterType = 'ALL';

  // Receipt Modal State
  selectedReceipt: Receipt | null = null;
  currentUser: any = null;
  /** Read at render time (not captured at construction) so module load order cannot leave it undefined. */
  get receiptCopy(): typeof RECEIPT_COPY {
    return RECEIPT_COPY;
  }

  constructor(
    private paymentsService: PaymentsService,
    private studentService: StudentService,
    private authService: AuthService,
    private pdfExportService: PdfExportService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.currentUser;
    this.loadOrders();
  }

  get filteredOrders(): OrderWithCourse[] {
    return this.selectedFilter === 'ALL'
      ? this.orders
      : this.orders.filter(o => o.status === this.selectedFilter);
  }

  setFilter(filter: PaymentFilterType): void {
    this.selectedFilter = filter;
    this.cdr.markForCheck();
  }

  // --- Data: ledger history (authority) + payments-service orders (supplementary) ---

  loadOrders(): void {
    const user = this.authService.currentUser;
    if (!user) {
      this.loading = false;
      return;
    }

    this.loading = true;
    forkJoin({
      ledger: this.paymentsService.getPaymentHistory().pipe(catchError(() => of([] as LedgerPayment[]))),
      gateway: this.paymentsService.getMyOrders().pipe(catchError(() => of([] as Order[])))
    }).subscribe(({ ledger, gateway }) => {
      this.orders = mergeHistory(ledger as LedgerPayment[], gateway, user);
      this.summary = summarize(this.orders);
      this.loading = false;
      this.enrichWithCourseDetails();
      this.cdr.detectChanges();
    });
  }

  /** Fills missing course names: first from another row of the same course, then from the catalogue. */
  private enrichWithCourseDetails(): void {
    const known = new Map<number, OrderWithCourse>();
    for (const o of this.orders) {
      if (o.courseTitle && !known.has(o.courseId)) known.set(o.courseId, o);
    }
    for (const o of this.orders) {
      const sibling = known.get(o.courseId);
      if (!o.courseTitle && sibling) {
        o.courseTitle = sibling.courseTitle;
        o.courseDepartment = o.courseDepartment ?? sibling.courseDepartment;
      }
    }

    const courseIds = Array.from(new Set(this.orders.filter(o => !o.courseTitle).map(o => o.courseId)));
    for (const courseId of courseIds) {
      this.studentService.getPublicCourse(courseId).subscribe({
        next: (course: Course) => {
          this.orders.forEach(order => {
            if (order.courseId === courseId && !order.courseTitle) {
              order.courseTitle = course?.title || `Course #${courseId}`;
              order.courseDepartment = order.courseDepartment ?? course?.department;
              order.courseThumbnail = order.courseThumbnail ?? course?.thumbnail;
            }
          });
          this.cdr.markForCheck();
        },
        error: () => {
          this.orders.forEach(order => {
            if (order.courseId === courseId && !order.courseTitle) {
              order.courseTitle = `Course #${courseId}`;
            }
          });
          this.cdr.markForCheck();
        }
      });
    }
  }

  // --- Receipt Modal & PDF Export ---

  openReceipt(order: OrderWithCourse): void {
    if (!order.receipt) return;
    this.selectedReceipt = order.receipt;
    this.cdr.markForCheck();
  }

  closeReceipt(): void {
    this.selectedReceipt = null;
    this.cdr.markForCheck();
  }

  printReceipt(): void {
    window.print();
  }

  downloadPdfReceipt(order?: OrderWithCourse): void {
    const receipt = order ? order.receipt : this.selectedReceipt;
    if (!receipt) return;
    this.pdfExportService.generateStudentReceipt(receipt);
  }

  // --- Helpers ---

  /** Formats a rupee amount (every row and total is in rupees, W6-14). */
  formatAmount(rupees: number): string {
    const value = Number.isFinite(rupees) ? rupees : 0;
    return value.toLocaleString('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });
  }

  getStatusClass(status: PaymentStatus): string {
    switch (status) {
      case 'PAID': return 'pill--paid';
      case 'FAILED': return 'pill--failed';
      case 'PENDING': return 'pill--created';
      default: return 'pill--grey';
    }
  }
}
