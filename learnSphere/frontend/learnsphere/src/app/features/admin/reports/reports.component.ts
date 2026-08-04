import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReportsService, ReportsSummary } from '../services/reports.service';
import { NotificationService } from '@core/services/notification.service';

@Component({
  selector: 'app-admin-reports',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.scss']
})
export class ReportsComponent implements OnInit {
  loading = false;
  summary: ReportsSummary | null = null;
  exportedAt: Date | null = null;

  constructor(
    private reportsService: ReportsService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.reportsService.getSummary().subscribe({
      next: (summary) => {
        this.summary = summary;
        this.loading = false;
      },
      error: () => {
        this.notificationService.error('Failed to load report summary.');
        this.loading = false;
      }
    });
  }

  roleEntries(): [string, number][] {
    return this.summary ? Object.entries(this.summary.usersByRole) : [];
  }

  statusEntries(): [string, number][] {
    return this.summary ? Object.entries(this.summary.coursesByStatus) : [];
  }

  exportCsv(): void {
    if (!this.summary) {
      return;
    }
    const rows: string[] = ['Metric,Value'];
    rows.push(`Total users,${this.summary.totalUsers}`);
    this.roleEntries().forEach(([role, count]) => rows.push(`Users - ${role},${count}`));
    rows.push(`Total courses,${this.summary.totalCourses}`);
    this.statusEntries().forEach(([status, count]) => rows.push(`Courses - ${status},${count}`));
    rows.push(`Total revenue,${this.summary.totalRevenue}`);
    rows.push(`Certificates issued,${this.summary.totalCertificatesIssued}`);
    rows.push(`Flagged content pending,${this.summary.flaggedContentPending}`);

    const blob = new Blob([rows.join('\n')], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `learnsphere-platform-report-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);

    this.exportedAt = new Date();
    this.notificationService.success('Report exported.');
  }
}
