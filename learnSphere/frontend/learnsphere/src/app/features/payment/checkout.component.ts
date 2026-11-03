import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CHECKOUT_COPY } from '@core/content/checkout.content';

/**
 * Legacy simulated checkout redirector.
 * The simulated checkout rail has been retired (A-06, AUD-06, W6-01).
 * All purchases are routed to the course page for real Razorpay modal checkout.
 */
@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="checkout-page">
      <div class="checkout-card">
        <h1 class="checkout-title">{{ copy.title }}</h1>
        <p class="sim-note">{{ copy.note }}</p>
        <a [routerLink]="courseId ? ['/student/courses', courseId] : '/student/courses'" class="pay-btn">
          {{ copy.toCourse }}
        </a>
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
      text-align: center;
    }
    .checkout-title {
      font-size: 20px;
      font-weight: 700;
      margin: 0 0 16px;
      color: var(--color-text-primary, #1e293b);
    }
    .sim-note {
      font-size: 14px;
      color: var(--color-text-secondary, #64748b);
      background: #f8fafc;
      border: 1px solid var(--color-border-tertiary, #e2e8f0);
      border-radius: 8px;
      padding: 14px;
      margin: 16px 0 24px;
      line-height: 1.5;
    }
    .pay-btn {
      display: inline-block;
      width: 100%;
      padding: 12px;
      border: none;
      border-radius: 8px;
      background: #534AB7;
      color: #fff;
      font-weight: 600;
      font-size: 14px;
      text-decoration: none;
      cursor: pointer;
      box-sizing: border-box;
    }
  `]
})
export class CheckoutComponent implements OnInit {
  protected readonly copy = CHECKOUT_COPY;
  courseId: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('courseId');
    if (idParam) {
      this.courseId = Number(idParam);
      // Automatically redirect to the course details page where Razorpay checkout is hosted
      this.router.navigate(['/student/courses', this.courseId]);
    } else {
      this.router.navigate(['/student/courses']);
    }
  }
}
