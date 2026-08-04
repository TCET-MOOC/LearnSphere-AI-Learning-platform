import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RevenueService, RevenueSummary } from '../services/revenue.service';
import { NotificationService } from '@core/services/notification.service';

@Component({
  selector: 'app-admin-revenue',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './revenue.component.html',
  styleUrls: ['./revenue.component.scss']
})
export class RevenueComponent implements OnInit {
  loading = false;
  summary: RevenueSummary | null = null;

  constructor(
    private revenueService: RevenueService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.revenueService.getRevenue().subscribe({
      next: (summary) => {
        this.summary = summary;
        this.loading = false;
      },
      error: () => {
        this.notificationService.error('Failed to load revenue data.');
        this.loading = false;
      }
    });
  }

  maxMonthlyAmount(): number {
    if (!this.summary?.byMonth?.length) return 1;
    return Math.max(...this.summary.byMonth.map((m) => m.amount), 1);
  }
}
