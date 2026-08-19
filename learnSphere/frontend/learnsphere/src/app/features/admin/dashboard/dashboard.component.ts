import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';
import { AdminService } from '../services/admin.service';
import { ReportsService } from '../services/reports.service';
import { RevenueService } from '../services/revenue.service';
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
}

interface RankedPerson {
  rank: string;
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
  kpiCards: KpiCard[] = [];
  topTeachers: RankedPerson[] = [];
  topStudents: RankedPerson[] = [];
  courseApprovals: (Course & { icon: string })[] = [];
  pendingPayouts: PendingPayout[] = [];
  positiveSentiment = 0;
  openFlags = 0;

  private readonly medals = ['#1', '#2', '#3'];

  constructor(
    private adminService: AdminService,
    private reportsService: ReportsService,
    private revenueService: RevenueService,
    private payoutService: PayoutService,
    private leaderboardService: LeaderboardService,
    private apiService: ApiService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    forkJoin({
      reports: this.reportsService.getSummary(),
      revenue: this.revenueService.getRevenue(),
      teachers: this.adminService.getTopTeachers(3),
      students: this.leaderboardService.getLeaderboard('global'),
      pendingCourses: this.apiService.get<Course[]>('/courses', { params: { status: 'PENDING' } }),
      payouts: this.payoutService.getPendingPayouts()
    }).subscribe({
      next: ({ reports, revenue, teachers, students, pendingCourses, payouts }) => {
        this.kpiCards = [
          { label: 'Total revenue', value: `₹${revenue.totalRevenue.toLocaleString()}`, sub: `${revenue.platformCutPercent}% platform cut`, subColor: 'var(--status-green-text)' },
          { label: 'Registered users', value: reports.totalUsers.toLocaleString(), sub: `${reports.usersByRole['STUDENT'] ?? 0} students`, subColor: 'var(--brand-primary)' },
          { label: 'Total courses', value: reports.totalCourses.toLocaleString(), sub: `${pendingCourses.length} pending review`, subColor: 'var(--status-amber-text)' },
          { label: 'Flagged items', value: reports.flaggedContentPending.toLocaleString(), sub: 'Needs action', subColor: 'var(--status-red-text)' }
        ];

        this.topTeachers = teachers.map((t, i) => ({
          rank: this.medals[i] ?? `#${i + 1}`,
          initials: getInitials(t.name),
          name: t.name,
          stats: `${t.students} students`,
          earnings: `₹${t.earnings.toLocaleString()}`,
          bg: getAvatarBg(t.name),
          color: getAvatarColor(t.name)
        }));

        this.topStudents = students.slice(0, 3).map((s, i) => ({
          rank: this.medals[i] ?? `#${i + 1}`,
          initials: getInitials(s.name),
          name: s.name,
          pts: `${s.points.toLocaleString()} pts`,
          bg: getAvatarBg(s.name),
          color: getAvatarColor(s.name)
        }));

        this.courseApprovals = pendingCourses.slice(0, 5).map((c) => ({ ...c, icon: 'book-open' }));
        this.pendingPayouts = payouts;
        this.openFlags = reports.flaggedContentPending;

        this.loading = false;
      },
      error: () => {
        this.notificationService.error('Failed to load dashboard data.');
        this.loading = false;
      }
    });
  }

  get pendingPayoutTotal(): number {
    return this.pendingPayouts.reduce((sum, p) => sum + p.pendingAmount, 0);
  }

  approveCourse(course: Course): void {
    this.apiService.put(`/admin/courses/${course.id}/status`, { status: 'LIVE' }).subscribe({
      next: () => {
        this.notificationService.success(`"${course.title}" approved and is now live.`);
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
