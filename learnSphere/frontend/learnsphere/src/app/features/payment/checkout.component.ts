import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '@core/services/api.service';
import { NotificationService } from '@core/services/notification.service';
import { PaymentService } from './payment.service';
import { Course } from '@core/models/course.model';

/**
 * Simulated checkout page for a paid course. There is no real payment gateway configured
 * for this project — "Pay now" creates a (simulated) gateway order and then immediately
 * verifies it, since there's no hosted checkout UI to redirect the user through.
 */
@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="checkout-page">
      <div class="checkout-card" *ngIf="!loadingCourse && course">
        <h1 class="checkout-title">Checkout</h1>

        <div class="course-summary">
          <img *ngIf="course.thumbnail" [src]="course.thumbnail" alt="" class="course-thumb" />
          <div class="course-info">
            <div class="course-name">{{ course.title }}</div>
            <div class="course-teacher" *ngIf="course.teacherName">by {{ course.teacherName }}</div>
          </div>
        </div>

        <div class="price-row">
          <span>Course price</span>
          <span class="price-value">₹{{ course.price }}</span>
        </div>
        <div class="price-row total-row">
          <span>Total payable</span>
          <span class="price-value">₹{{ course.price }}</span>
        </div>

        <p class="sim-note">
          This is a simulated payment flow for demo purposes — no real payment gateway is connected.
          Clicking "Pay now" will instantly create and confirm a mock transaction.
        </p>

        <button class="pay-btn" [disabled]="paying" (click)="payNow()">
          {{ paying ? 'Processing…' : 'Pay now — ₹' + course.price }}
        </button>

        <a routerLink="/student/courses" class="cancel-link">Cancel and go back</a>
      </div>

      <div class="checkout-card" *ngIf="loadingCourse">
        <p>Loading course details…</p>
      </div>

      <div class="checkout-card" *ngIf="!loadingCourse && !course">
        <p>We couldn't find that course.</p>
        <a routerLink="/student/courses" class="cancel-link">Back to courses</a>
      </div>
    </div>
  `,
  styles: [`
    .checkout-page {
      display: flex;
      justify-content: center;
      padding: 40px 16px;
    }
    .checkout-card {
      width: 100%;
      max-width: 460px;
      background: var(--color-background-primary, #fff);
      border: 1px solid var(--color-border-secondary, #e2e8f0);
      border-radius: 12px;
      padding: 28px;
    }
    .checkout-title {
      font-size: 20px;
      font-weight: 700;
      margin: 0 0 20px;
      color: var(--color-text-primary, #1e293b);
    }
    .course-summary {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 20px;
    }
    .course-thumb {
      width: 64px;
      height: 64px;
      border-radius: 8px;
      object-fit: cover;
      background: #f4f5f7;
    }
    .course-name {
      font-weight: 600;
      font-size: 15px;
      color: var(--color-text-primary, #1e293b);
    }
    .course-teacher {
      font-size: 13px;
      color: var(--color-text-secondary, #64748b);
    }
    .price-row {
      display: flex;
      justify-content: space-between;
      padding: 10px 0;
      font-size: 14px;
      color: var(--color-text-secondary, #64748b);
      border-bottom: 1px solid var(--color-border-tertiary, #edf2f7);
    }
    .total-row {
      font-weight: 700;
      color: var(--color-text-primary, #1e293b);
      border-bottom: none;
    }
    .price-value {
      font-weight: 600;
    }
    .sim-note {
      font-size: 12px;
      color: var(--color-text-secondary, #64748b);
      background: #f4f5f7;
      border-radius: 8px;
      padding: 10px 12px;
      margin: 16px 0;
    }
    .pay-btn {
      width: 100%;
      padding: 12px;
      border: none;
      border-radius: 8px;
      background: #534AB7;
      color: #fff;
      font-weight: 600;
      font-size: 14px;
      cursor: pointer;
    }
    .pay-btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    .cancel-link {
      display: block;
      text-align: center;
      margin-top: 14px;
      font-size: 13px;
      color: var(--color-text-secondary, #64748b);
      text-decoration: none;
    }
  `]
})
export class CheckoutComponent implements OnInit {
  course: Course | null = null;
  loadingCourse = true;
  paying = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ApiService,
    private paymentService: PaymentService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    const courseId = Number(this.route.snapshot.paramMap.get('courseId'));
    if (!courseId) {
      this.loadingCourse = false;
      return;
    }
    this.apiService.get<Course>(`/courses/${courseId}`).subscribe({
      next: (course) => {
        this.course = course;
        this.loadingCourse = false;
      },
      error: () => {
        this.loadingCourse = false;
      }
    });
  }

  payNow(): void {
    if (!this.course) {
      return;
    }
    this.paying = true;
    this.paymentService.checkout(this.course.id).subscribe({
      next: (order) => {
        this.paymentService.verify(order.orderId).subscribe({
          next: () => {
            this.paying = false;
            this.notificationService.success('Payment successful — you are now enrolled!');
            this.router.navigate(['/payment/success'], { queryParams: { courseId: this.course!.id } });
          },
          error: (err) => {
            this.paying = false;
            this.notificationService.error(err?.error?.message || 'Payment verification failed.');
          }
        });
      },
      error: (err) => {
        this.paying = false;
        this.notificationService.error(err?.error?.message || 'Could not start checkout.');
      }
    });
  }
}
