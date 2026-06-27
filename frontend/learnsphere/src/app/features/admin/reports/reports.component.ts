import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

type ReportTab = 'engagement' | 'growth' | 'quality' | 'revenue';
type Tone = 'green' | 'amber' | 'red' | 'purple' | 'blue';

interface ReportMetric {
  label: string;
  value: string;
  sub: string;
  color: string;
}

interface DepartmentMetric {
  department: string;
  value: number;
  color: string;
}

interface QualityRow {
  course: string;
  teacher: string;
  completion: string;
  rating: string;
  flags: string;
  status: string;
  tone: Tone;
}

interface ScheduledReport {
  name: string;
  cadence: string;
  recipients: string;
  status: 'Active' | 'Paused';
  tone: Tone;
}

@Component({
  selector: 'app-admin-reports',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.scss']
})
export class ReportsComponent {
  activeTab: ReportTab = 'engagement';
  selectedPeriod = 'Jun 2026';
  selectedCollege = 'All colleges';
  exportNotice = '';

  readonly tabs: Array<{ label: string; value: ReportTab }> = [
    { label: 'Engagement', value: 'engagement' },
    { label: 'Growth', value: 'growth' },
    { label: 'Course quality', value: 'quality' },
    { label: 'Revenue', value: 'revenue' }
  ];

  readonly periods = ['Jun 2026', 'May 2026', 'Apr 2026', 'Q2 2026'];
  readonly colleges = ['All colleges', 'Mumbai Institute of Technology', 'Pune Engineering College', 'External learners'];

  readonly metricsByTab: Record<ReportTab, ReportMetric[]> = {
    engagement: [
      { label: 'Avg. session time', value: '34 min', sub: '+6 min vs May', color: '#0F6E56' },
      { label: 'Videos watched', value: '12,480', sub: 'This month', color: '#534AB7' },
      { label: 'Quizzes attempted', value: '4,210', sub: '78% pass rate', color: '#0C447C' },
      { label: 'Active learners', value: '2,386', sub: '+14% vs May', color: '#1D9E75' }
    ],
    growth: [
      { label: 'New students', value: '+386', sub: '+22% vs May', color: '#0F6E56' },
      { label: 'New teachers', value: '+14', sub: '+12% vs May', color: '#534AB7' },
      { label: 'External paid users', value: '+128', sub: '+34% vs May', color: '#0C447C' },
      { label: 'Certificates issued', value: '218', sub: '+18% vs May', color: '#1D9E75' }
    ],
    quality: [
      { label: 'Avg. completion', value: '78%', sub: '+5% vs May', color: '#0F6E56' },
      { label: 'Avg. course rating', value: '4.6', sub: 'Across 62 courses', color: '#534AB7' },
      { label: 'Flag rate', value: '1.8%', sub: '14 open items', color: '#A32D2D' },
      { label: 'Review backlog', value: '7', sub: 'Pending approvals', color: '#BA7517' }
    ],
    revenue: [
      { label: 'Gross revenue', value: 'INR 4.2L', sub: '+18% this month', color: '#0F6E56' },
      { label: 'Platform share', value: 'INR 84K', sub: '20% retained', color: '#534AB7' },
      { label: 'Teacher payouts', value: 'INR 3.36L', sub: 'Due 30 Jun', color: '#BA7517' },
      { label: 'Paid conversions', value: '8.4%', sub: '+1.2% vs May', color: '#0C447C' }
    ]
  };

  readonly departments: DepartmentMetric[] = [
    { department: 'IT / CS', value: 84, color: '#534AB7' },
    { department: 'Mechanical', value: 71, color: '#1D9E75' },
    { department: 'Electronics', value: 63, color: '#0C447C' },
    { department: 'Civil', value: 58, color: '#BA7517' },
    { department: 'Biotech', value: 44, color: '#993556' }
  ];

  readonly qualityRows: QualityRow[] = [
    { course: 'Engineering Mathematics III', teacher: 'Prof. A. Sharma', completion: '82%', rating: '4.9', flags: '1', status: 'Healthy', tone: 'green' },
    { course: 'Data Structures and Algorithms', teacher: 'Dr. Priya Nair', completion: '76%', rating: '4.7', flags: '3', status: 'Monitor', tone: 'amber' },
    { course: 'Fluid Mechanics', teacher: 'Prof. R. Patil', completion: '61%', rating: '4.2', flags: '0', status: 'Needs boost', tone: 'blue' },
    { course: 'Statistics for Engineers', teacher: 'Prof. S. Mehta', completion: 'Draft', rating: '-', flags: '0', status: 'Pending review', tone: 'purple' }
  ];

  scheduledReports: ScheduledReport[] = [
    { name: 'Monthly platform report', cadence: '1st of every month', recipients: 'Admin inbox', status: 'Active', tone: 'green' },
    { name: 'Weekly flagged content digest', cadence: 'Every Monday', recipients: 'Moderation team', status: 'Active', tone: 'green' },
    { name: 'Payout summary', cadence: 'End of month', recipients: 'Admin + finance', status: 'Active', tone: 'green' }
  ];

  get metrics(): ReportMetric[] {
    return this.metricsByTab[this.activeTab];
  }

  setTab(tab: ReportTab): void {
    this.activeTab = tab;
    this.exportNotice = '';
  }

  exportReport(): void {
    const tabLabel = this.tabs.find((tab) => tab.value === this.activeTab)?.label ?? 'Report';
    this.exportNotice = `${tabLabel} report queued for ${this.selectedPeriod} (${this.selectedCollege}).`;
  }

  toggleSchedule(report: ScheduledReport): void {
    this.scheduledReports = this.scheduledReports.map((item) => {
      if (item.name !== report.name) {
        return item;
      }

      const isActive = item.status === 'Active';
      return {
        ...item,
        status: isActive ? 'Paused' : 'Active',
        tone: isActive ? 'amber' : 'green'
      };
    });
  }

  trackByLabel(_: number, item: { label?: string; name?: string; department?: string; course?: string }): string {
    return item.label ?? item.name ?? item.department ?? item.course ?? '';
  }
}
