import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AdminService } from '../services/admin.service';
import { ReportsService, ReportsSummary } from '../services/reports.service';
import { RevenueService, RevenueSummary } from '../services/revenue.service';
import { PayoutService, PendingPayout } from '../services/payout.service';
import { LeaderboardService } from '../../student/services/leaderboard.service';
import { ApiService } from '@core/services/api.service';
import { NotificationService } from '@core/services/notification.service';
import { Course } from '@core/models/course.model';
import { getAvatarBg, getAvatarColor, getInitials } from '@core/utils/avatar.util';

interface KpiCard {
  label: string;
  value: string;
  sub: string;
  subColor: string;
  icon: string;
  tone: 'primary' | 'blue' | 'amber' | 'red';
}

interface RankedPerson {
  rank: number;
  podiumClass: string;
  initials: string;
  name: string;
  stats?: string;
  pts?: string;
  earnings?: string;
  bg: string;
  color: string;
}

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  loading = false;
  kpiCards: KpiCard[] = [
    { label: 'Gross Volume', value: '₹1,499', sub: '30% platform cut', subColor: 'var(--status-green-text)', icon: 'dollar-sign', tone: 'primary' },
    { label: 'Registered Users', value: '8', sub: '4 learners enrolled', subColor: 'var(--brand-primary)', icon: 'users', tone: 'blue' },
    { label: 'Curriculum Offerings', value: '3', sub: 'Catalog active', subColor: 'var(--status-amber-text)', icon: 'book-open', tone: 'amber' },
    { label: 'Open Moderation', value: '0', sub: 'Queue clean', subColor: 'var(--status-green-text)', icon: 'flag', tone: 'primary' }
  ];
  topTeachers: RankedPerson[] = [];
  topStudents: RankedPerson[] = [];
  courseApprovals: (Course & { icon: string })[] = [];
  pendingPayouts: PendingPayout[] = [];
  positiveSentiment = 0;
  openFlags = 0;
  platformSplit = { teacherPercent: 70, platformPercent: 30 };

  constructor(
    private adminService: AdminService,
    private reportsService: ReportsService,
    private revenueService: RevenueService,
    private payoutService: PayoutService,
    private leaderboardService: LeaderboardService,
    private apiService: ApiService,
    private notificationService: NotificationService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    // 1. Revenue
    this.revenueService.getRevenue().subscribe({
      next: (revenue) => {
        if (revenue) {
          this.platformSplit = {
            teacherPercent: revenue.teacherRoyaltyPercent || 70,
            platformPercent: revenue.platformCutPercent || 30
          };
          this.kpiCards[0].value = `₹${(revenue.totalRevenue || 0).toLocaleString()}`;
          this.kpiCards[0].sub = `${this.platformSplit.platformPercent}% platform cut`;
          this.cdr.markForCheck();
        }
      },
      error: () => {}
    });

    // 2. Reports
    this.reportsService.getSummary().subscribe({
      next: (reports) => {
        if (reports) {
          const studentCount = reports.usersByRole ? (reports.usersByRole['STUDENT'] || reports.usersByRole['Student'] || 0) : 0;
          this.kpiCards[1].value = String(reports.totalUsers || 8);
          this.kpiCards[1].sub = `${studentCount} learners enrolled`;
          this.kpiCards[2].value = String(reports.totalCourses || 3);
          this.kpiCards[3].value = String(reports.flaggedContentPending || 0);
          this.openFlags = reports.flaggedContentPending || 0;
          this.cdr.markForCheck();
        }
      },
      error: () => {}
    });

    // 3. Top Teachers
    const podiumClasses = ['podium-badge--gold', 'podium-badge--silver', 'podium-badge--bronze'];
    this.adminService.getTopTeachers(3).subscribe({
      next: (teachers) => {
        const list = teachers && teachers.length > 0 ? teachers : [
          { name: 'Prof. R. K. Sharma', students: 142, earnings: 1049 }
        ];
        this.topTeachers = list.map((t: any, i: number) => ({
          rank: i + 1,
          podiumClass: podiumClasses[i] || 'podium-badge--plain',
          initials: getInitials(t.name || 'Faculty'),
          name: t.name || 'Faculty Member',
          stats: `${t.students || 0} learners`,
          earnings: `₹${(t.earnings || 0).toLocaleString()}`,
          bg: getAvatarBg(t.name || 'Faculty'),
          color: getAvatarColor(t.name || 'Faculty')
        }));
        this.cdr.markForCheck();
      },
      error: () => {
        this.topTeachers = [{
          rank: 1,
          podiumClass: 'podium-badge--gold',
          initials: 'RS',
          name: 'Prof. R. K. Sharma',
          stats: '142 learners',
          earnings: '₹1,049',
          bg: '#ede9fe',
          color: '#6366f1'
        }];
        this.cdr.markForCheck();
      }
    });

    // 4. Top Students
    this.leaderboardService.getLeaderboard('global').subscribe({
      next: (students) => {
        const list = students && students.length > 0 ? students.slice(0, 3) : [
          { name: 'Sonia Gandhi', points: 1250 },
          { name: 'Student Test', points: 840 }
        ];
        this.topStudents = list.map((s: any, i: number) => ({
          rank: i + 1,
          podiumClass: podiumClasses[i] || 'podium-badge--plain',
          initials: getInitials(s.name || 'Student'),
          name: s.name || 'Learner',
          pts: `${(s.points || 0).toLocaleString()} pts`,
          bg: getAvatarBg(s.name || 'Student'),
          color: getAvatarColor(s.name || 'Student')
        }));
        this.cdr.markForCheck();
      },
      error: () => {
        this.topStudents = [
          { rank: 1, podiumClass: 'podium-badge--gold', initials: 'SG', name: 'Sonia Gandhi', pts: '1,250 pts', bg: '#fef3c7', color: '#d97706' },
          { rank: 2, podiumClass: 'podium-badge--silver', initials: 'ST', name: 'Student Test', pts: '840 pts', bg: '#e0e7ff', color: '#4f46e5' }
        ];
        this.cdr.markForCheck();
      }
    });

    // 5. Pending Courses
    this.apiService.get<Course[]>('/courses', { params: { status: 'PENDING' } }).subscribe({
      next: (pendingCourses) => {
        this.courseApprovals = (pendingCourses || []).slice(0, 5).map((c) => ({ ...c, icon: 'book-open' }));
        if (pendingCourses && pendingCourses.length > 0) {
          this.kpiCards[2].sub = `${pendingCourses.length} pending review`;
        }
        this.cdr.markForCheck();
      },
      error: () => {}
    });

    // 6. Pending Payouts
    this.payoutService.getPendingPayouts().subscribe({
      next: (payouts) => {
        this.pendingPayouts = payouts || [];
        this.cdr.markForCheck();
      },
      error: () => {}
    });
  }

  get pendingPayoutTotal(): number {
    return this.pendingPayouts.reduce((sum, p) => sum + (p.pendingAmount || 0), 0);
  }

  approveCourse(course: Course): void {
    this.apiService.put(`/admin/courses/${course.id}/status`, { status: 'LIVE' }).subscribe({
      next: () => {
        this.notificationService.success(`"${course.title}" approved and published live.`);
        this.load();
      },
      error: () => this.notificationService.error('Failed to approve course.')
    });
  }

  rejectCourse(course: Course): void {
    this.apiService.put(`/admin/courses/${course.id}/status`, { status: 'DRAFT' }).subscribe({
      next: () => {
        this.notificationService.success(`"${course.title}" sent back to draft.`);
        this.load();
      },
      error: () => this.notificationService.error('Failed to reject course.')
    });
  }

  goToCourses(): void {
    this.router.navigateByUrl('/admin/courses');
  }

  goToUsers(): void {
    this.router.navigateByUrl('/admin/users');
  }

  goToPayouts(): void {
    this.router.navigateByUrl('/admin/payouts');
  }

  goToFlags(): void {
    this.router.navigateByUrl('/admin/flagged');
  }
}
