import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { ReportsService, ReportsSummary } from '../services/reports.service';
import { NotificationService } from '@core/services/notification.service';

@Component({
  selector: 'app-admin-reports',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.scss']
})
export class ReportsComponent implements OnInit {
  loading = false;
  summary: ReportsSummary | null = null;
  exportedAt: Date | null = null;

  constructor(
    private reportsService: ReportsService,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.reportsService.getSummary().subscribe({
      next: (summary) => {
        this.summary = summary;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.notificationService.error('Failed to load report summary.');
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  roleEntries(): [string, number][] {
    return this.summary ? Object.entries(this.summary.usersByRole) : [];
  }

  statusEntries(): [string, number][] {
    return this.summary ? Object.entries(this.summary.coursesByStatus) : [];
  }

  getRolePercentage(count: number): number {
    if (!this.summary || this.summary.totalUsers <= 0) return 0;
    return Math.round((count / this.summary.totalUsers) * 100);
  }

  getStatusPercentage(count: number): number {
    if (!this.summary || this.summary.totalCourses <= 0) return 0;
    return Math.round((count / this.summary.totalCourses) * 100);
  }

  getStatusClass(status: string): string {
    const s = status.toUpperCase();
    if (s.includes('LIVE')) return 'pill--green';
    if (s.includes('PENDING')) return 'pill--amber';
    if (s.includes('ARCHIVED')) return 'pill--grey';
    return 'pill--purple';
  }

  getRoleClass(role: string): string {
    const r = role.toUpperCase();
    if (r.includes('ADMIN')) return 'pill--purple';
    if (r.includes('TEACHER')) return 'pill--amber';
    return 'pill--green';
  }

  exportCsv(): void {
    if (!this.summary) {
      return;
    }
    const rows: string[] = ['Metric,Value'];
    rows.push(`Total Users,${this.summary.totalUsers}`);
    this.roleEntries().forEach(([role, count]) => rows.push(`Users - ${role},${count}`));
    rows.push(`Total Courses,${this.summary.totalCourses}`);
    this.statusEntries().forEach(([status, count]) => rows.push(`Courses - ${status},${count}`));
    rows.push(`Total Revenue (INR),${this.summary.totalRevenue}`);
    rows.push(`Certificates Issued,${this.summary.totalCertificatesIssued}`);
    rows.push(`Flagged Content Pending,${this.summary.flaggedContentPending}`);

    const blob = new Blob([rows.join('\n')], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `learnsphere-executive-report-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);

    this.exportedAt = new Date();
    this.notificationService.success('Executive CSV Ledger exported successfully.');
    this.cdr.markForCheck();
  }
}
