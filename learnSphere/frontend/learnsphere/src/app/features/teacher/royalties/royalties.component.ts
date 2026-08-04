import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RoyaltyService, RoyaltyBreakdownRow, RoyaltySourceSplit, PayoutRow } from '../services/royalty.service';
import { NotificationService } from '@core/services/notification.service';

interface EarningsRow {
  title: string;
  enrolled: number;
  externalPaid: number;
  amount: string;
  status: string;
  isDraft: boolean;
}

interface PayoutHistoryRow {
  month: string;
  date: string;
  amount: string;
  status: string;
  statusClass: string;
}

interface BreakdownRow {
  label: string;
  percentage: number;
  color: string;
}

const SOURCE_COLORS: Record<string, string> = {
  EXTERNAL_SALES: '#534AB7',
  COLLEGE_SHARE: '#0f9d58',
  REMEDIAL_CERTS: '#b97700'
};

const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/**
 * Real earnings/payouts data pulled from RoyaltyService — replaces the previous static
 * earningsByCourse/payoutHistory/royaltyBreakdown demo arrays.
 */
@Component({
  selector: 'app-royalties',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './royalties.component.html',
  styleUrls: ['./royalties.component.scss']
})
export class RoyaltiesComponent implements OnInit {
  loading = true;

  thisMonthTotal = '₹0';
  totalEarned = '₹0';
  pendingPayoutAmount = '₹0';
  externalEnrollments = 0;

  earningsByCourse: EarningsRow[] = [];
  payoutHistory: PayoutHistoryRow[] = [];
  royaltyBreakdown: BreakdownRow[] = [];

  constructor(
    private royaltyService: RoyaltyService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.royaltyService.getRoyalties().subscribe({
      next: (res) => {
        this.thisMonthTotal = this.formatCurrency(res.summary.thisMonthTotal);
        this.totalEarned = this.formatCurrency(res.summary.totalEarned);
        this.pendingPayoutAmount = this.formatCurrency(res.summary.pendingPayout);
        this.externalEnrollments = res.summary.externalEnrollments;

        this.earningsByCourse = res.byCourse.map((c: RoyaltyBreakdownRow) => ({
          title: c.courseTitle,
          enrolled: c.enrolledCount,
          externalPaid: c.externalPaidCount,
          amount: c.isDraft ? '—' : this.formatCurrency(c.amount),
          status: c.status,
          isDraft: c.isDraft
        }));

        this.royaltyBreakdown = res.sourceBreakdown.map((s: RoyaltySourceSplit) => ({
          label: s.label,
          percentage: s.percentage,
          color: SOURCE_COLORS[s.source] ?? '#888'
        }));

        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.notificationService.error('Failed to load royalty earnings.');
      }
    });

    this.royaltyService.getPayouts().subscribe({
      next: (payouts) => {
        this.payoutHistory = payouts.map((p: PayoutRow) => this.toPayoutRow(p));
      },
      error: () => {
        this.notificationService.error('Failed to load payout history.');
      }
    });
  }

  private toPayoutRow(p: PayoutRow): PayoutHistoryRow {
    const label = this.formatPeriod(p.period);
    const isPaid = p.status === 'PAID';
    return {
      month: `${label} payout`,
      date: isPaid && p.transferredAt
        ? `Transferred ${this.formatDate(p.transferredAt)}`
        : `Scheduled ${this.formatDate(p.createdAt)}`,
      amount: this.formatCurrency(p.amount),
      status: isPaid ? 'paid' : 'pending',
      statusClass: isPaid ? 'status-green' : 'status-amber'
    };
  }

  private formatPeriod(period: string): string {
    const [year, month] = period.split('-').map(Number);
    if (!year || !month) {
      return period;
    }
    return `${MONTH_NAMES[month - 1]} ${year}`;
  }

  private formatDate(iso: string): string {
    const d = new Date(iso);
    if (isNaN(d.getTime())) {
      return iso;
    }
    return `${d.getDate()} ${MONTH_NAMES[d.getMonth()]}`;
  }

  private formatCurrency(amount: number): string {
    if (amount == null) {
      return '₹0';
    }
    if (amount >= 100000) {
      return `₹${(amount / 100000).toFixed(1)}L`;
    }
    return `₹${Math.round(amount).toLocaleString('en-IN')}`;
  }
}
