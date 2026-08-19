import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';
import { PaymentsService, Order } from '../services/payments.service';
import { StudentService } from '../services/student.service';
import { Course } from '@core/models/course.model';
import { PdfExportService } from '@core/services/pdf-export.service';
import { LucideAngularModule } from 'lucide-angular';

export interface OrderWithCourse extends Order {
  courseTitle?: string;
  courseDepartment?: string;
  courseThumbnail?: string;
}

export type PaymentFilterType = 'ALL' | 'PAID' | 'PENDING' | 'FAILED';

@Component({
  selector: 'app-purchases',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  templateUrl: './purchases.component.html',
  styleUrls: ['./purchases.component.scss']
})
export class PurchasesComponent implements OnInit {
  orders: OrderWithCourse[] = [];
  loading = true;
  selectedFilter: PaymentFilterType = 'ALL';

  // Receipt Modal State
  selectedReceipt: OrderWithCourse | null = null;
  currentUser: any = null;

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

  // --- Pure Computed Getters (100% Deterministic Filtering) ---

  get filteredOrders(): OrderWithCourse[] {
    switch (this.selectedFilter) {
      case 'PAID':
        return this.orders.filter(o => o.status === 'PAID');
      case 'PENDING':
        return this.orders.filter(o => o.status === 'CREATED');
      case 'FAILED':
        return this.orders.filter(o => o.status === 'FAILED');
      default:
        return this.orders;
    }
  }

  get allCount(): number {
    return this.orders.length;
  }

  get paidCount(): number {
    return this.orders.filter(o => o.status === 'PAID').length;
  }

  get pendingCount(): number {
    return this.orders.filter(o => o.status === 'CREATED').length;
  }

  get failedCount(): number {
    return this.orders.filter(o => o.status === 'FAILED').length;
  }

  get successfulPurchasesCount(): number {
    return this.paidCount;
  }

  get activeOrdersCount(): number {
    return this.pendingCount;
  }

  get totalSpent(): number {
    const totalPaise = this.orders
      .filter(o => o.status === 'PAID')
      .reduce((sum, o) => sum + (o.amount || 0), 0);
    return totalPaise / 100;
  }

  setFilter(filter: PaymentFilterType): void {
    this.selectedFilter = filter;
    this.cdr.markForCheck();
  }

  // --- Data Fetching & Course Consolidation (1 Card Per Course) ---

  loadOrders(): void {
    const user = this.authService.currentUser;
    if (!user) {
      this.loading = false;
      return;
    }

    this.loading = true;
    const userId = user.id;

    // 1. Fetch authoritative verified payment history from Main Backend
    this.paymentsService.getPaymentHistory().subscribe({
      next: (history) => {
        const verifiedOrders: OrderWithCourse[] = (history || [])
          .filter((p: any) => p && p.courseId)
          .map((p: any) => ({
            id: p.id,
            razorpayOrderId: p.gatewayOrderId || `RZP-ORD-${p.id}`,
            userId: userId,
            courseId: p.courseId,
            courseTitle: p.courseTitle,
            courseDepartment: p.department,
            courseThumbnail: p.thumbnail,
            amount: Math.round(Number(p.amount || 0) * 100), // convert to paise
            currency: p.currency || 'INR',
            status: 'PAID',
            createdAt: p.paidAt || p.createdAt
          }));

        // 2. Fetch pending checkout drafts from payments microservice
        this.paymentsService.getUserOrders(userId).subscribe({
          next: (msOrders) => {
            const rawDrafts: OrderWithCourse[] = (msOrders || [])
              .filter(o => o && o.courseId && o.amount > 0)
              .map(o => ({
                ...o,
                status: this.normalizeStatus(o.status)
              }));

            this.consolidateAndEnrich([...verifiedOrders, ...rawDrafts]);
          },
          error: () => {
            this.consolidateAndEnrich(verifiedOrders);
          }
        });
      },
      error: () => {
        // Fallback to microservice if main backend is restarting
        this.paymentsService.getUserOrders(userId).subscribe({
          next: (msOrders) => {
            const rawDrafts: OrderWithCourse[] = (msOrders || [])
              .filter(o => o && o.courseId && o.amount > 0)
              .map(o => ({
                ...o,
                status: this.normalizeStatus(o.status)
              }));
            this.consolidateAndEnrich(rawDrafts);
          },
          error: () => {
            this.orders = [];
            this.loading = false;
            this.cdr.markForCheck();
          }
        });
      }
    });
  }

  private normalizeStatus(rawStatus: any): 'PAID' | 'FAILED' | 'CREATED' {
    const s = String(rawStatus || '').toUpperCase().trim();
    if (s === 'SUCCESS' || s === 'PAID' || s === 'COMPLETED') {
      return 'PAID';
    }
    if (s === 'FAILED' || s === 'CANCELLED' || s === 'REJECTED') {
      return 'FAILED';
    }
    return 'CREATED';
  }

  /**
   * Consolidates all orders by Course ID so that each course has exactly ONE card:
   * 1. If course is PAID, keep the PAID order and discard all pending/failed drafts for that course.
   * 2. If course is not paid, keep only the latest single pending attempt.
   */
  private consolidateAndEnrich(allRaw: OrderWithCourse[]): void {
    const courseMap = new Map<number, OrderWithCourse>();

    // First pass: register all PAID orders
    for (const ord of allRaw) {
      if (ord.status === 'PAID') {
        courseMap.set(ord.courseId, ord);
      }
    }

    // Second pass: for courses not yet paid, keep only one latest pending/failed draft
    for (const ord of allRaw) {
      if (!courseMap.has(ord.courseId)) {
        courseMap.set(ord.courseId, ord);
      }
    }

    this.orders = Array.from(courseMap.values());
    this.enrichWithCourseDetails();
    this.loading = false;
    this.cdr.detectChanges();
  }

  private enrichWithCourseDetails(): void {
    const missing = this.orders.filter(o => !o.courseTitle && o.courseId);
    const courseIds = Array.from(new Set(missing.map(o => o.courseId)));

    if (courseIds.length === 0) return;

    for (const courseId of courseIds) {
      this.studentService.getPublicCourse(courseId).subscribe({
        next: (course: Course) => {
          this.orders.forEach(order => {
            if (order.courseId === courseId) {
              order.courseTitle = course.title;
              order.courseDepartment = course.department;
              order.courseThumbnail = course.thumbnail;
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
    this.selectedReceipt = order;
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
    const o = order || this.selectedReceipt;
    if (!o) return;

    this.pdfExportService.generateStudentReceipt({
      receiptNumber: `INV-${o.id ? (o.id + 1000) : '2026'}`,
      orderId: o.razorpayOrderId,
      studentName: this.currentUser?.fullName || 'Student',
      studentEmail: this.currentUser?.email || 'student@learnsphere.io',
      courseTitle: o.courseTitle || 'Course Access (Lifetime)',
      courseDepartment: o.courseDepartment || 'Online Learning Program',
      amount: (o.amount || 0) / 100,
      currency: o.currency || 'INR',
      paymentDate: o.createdAt ? new Date(o.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : 'Completed',
      status: 'PAID'
    });
  }

  // --- Helpers ---

  formatAmount(paiseAmount: number): string {
    if (paiseAmount == null || isNaN(paiseAmount)) {
      return '₹0.00';
    }
    return (paiseAmount / 100).toLocaleString('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PAID': return 'pill--paid';
      case 'FAILED': return 'pill--failed';
      case 'CREATED': return 'pill--created';
      default: return 'pill--grey';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'PAID': return 'COMPLETED';
      case 'FAILED': return 'FAILED';
      case 'CREATED': return 'PENDING CONFIRMATION';
      default: return status;
    }
  }
}
