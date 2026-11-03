import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LucideAngularModule, CheckCircle2 } from 'lucide-angular';
import { PAYMENT_SUCCESS_COPY } from '@core/content/payment-success.content';

/** Simple confirmation page shown after a successful (simulated) course payment. */
@Component({
  selector: 'app-payment-success',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule],
  template: `
    <div class="success-page">
      <div class="success-card">
        <div class="success-icon" style="display:flex; justify-content:center;">
          <lucide-icon name="check-circle-2" style="width:40px; height:40px; color:#1D9E75;"></lucide-icon>
        </div>
        <h1 class="success-title">{{ copy.title }}</h1>
        <p class="success-sub">{{ copy.body }}</p>
        <a *ngIf="courseId" [routerLink]="['/student/courses', courseId]" class="primary-link">
          {{ copy.toCourse }}
        </a>
        <a routerLink="/student/courses" class="secondary-link">{{ copy.toMyCourses }}</a>
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
  protected readonly copy = PAYMENT_SUCCESS_COPY;
  courseId: string | null = null;

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.courseId = this.route.snapshot.queryParamMap.get('courseId');
  }
}
