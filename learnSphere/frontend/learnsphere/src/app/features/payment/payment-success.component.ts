import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

/** Simple confirmation page shown after a successful (simulated) course payment. */
@Component({
  selector: 'app-payment-success',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="success-page">
      <div class="success-card">
        <div class="success-icon">✅</div>
        <h1 class="success-title">Payment successful</h1>
        <p class="success-sub">
          You're all set — you now have full access to this course. Happy learning!
        </p>
        <a *ngIf="courseId" [routerLink]="['/student/courses', courseId]" class="primary-link">
          Go to course
        </a>
        <a routerLink="/student/courses" class="secondary-link">Back to my courses</a>
      </div>
    </div>
  `,
  styles: [`
    .success-page {
      display: flex;
      justify-content: center;
      padding: 60px 16px;
    }
    .success-card {
      width: 100%;
      max-width: 420px;
      text-align: center;
      background: var(--color-background-primary, #fff);
      border: 1px solid var(--color-border-secondary, #e2e8f0);
      border-radius: 12px;
      padding: 36px 28px;
    }
    .success-icon {
      font-size: 40px;
      margin-bottom: 12px;
    }
    .success-title {
      font-size: 20px;
      font-weight: 700;
      margin: 0 0 8px;
      color: var(--color-text-primary, #1e293b);
    }
    .success-sub {
      font-size: 14px;
      color: var(--color-text-secondary, #64748b);
      margin: 0 0 24px;
    }
    .primary-link {
      display: block;
      padding: 12px;
      border-radius: 8px;
      background: #534AB7;
      color: #fff;
      font-weight: 600;
      font-size: 14px;
      text-decoration: none;
      margin-bottom: 12px;
    }
    .secondary-link {
      display: block;
      font-size: 13px;
      color: var(--color-text-secondary, #64748b);
      text-decoration: none;
    }
  `]
})
export class PaymentSuccessComponent implements OnInit {
  courseId: string | null = null;

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.courseId = this.route.snapshot.queryParamMap.get('courseId');
  }
}
