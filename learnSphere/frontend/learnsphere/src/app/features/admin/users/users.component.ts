import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminService, AdminUserRecord, UserActivitySummary } from '../services/admin.service';
import { ModerationService } from '../services/moderation.service';
import { NotificationService } from '@core/services/notification.service';
import { timeAgo } from '@core/utils/time.util';
import { getInitials } from '@core/utils/avatar.util';

type DisplayStatus = 'Active' | 'Flagged' | 'Blacklisted';

interface AdminUserRow {
  id: number;
  initials: string;
  name: string;
  email: string;
  role: string;
  department: string;
  status: DisplayStatus;
  tone: 'purple' | 'green' | 'amber' | 'red' | 'grey';
  meta: string;
}

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss']
})
export class UsersComponent implements OnInit {
  allUsers: AdminUserRow[] = [];
  users: AdminUserRow[] = [];
  tabs: string[] = ['All users'];
  activeTab = 'All users';

  summaryCards: { label: string; value: string; sub: string; tone: string }[] = [];
  activity: { label: string; value: number; tone: string }[] = [];
  flaggedCount = 0;
  loading = false;
  updatingId: number | null = null;

  constructor(
    private adminService: AdminService,
    private moderationService: ModerationService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.adminService.getUsers().subscribe({
      next: (records) => {
        this.allUsers = records.map((r) => this.toRow(r));
        this.buildTabs();
        this.applyTab();
        this.loading = false;
      },
      error: () => {
        this.notificationService.error('Failed to load users.');
        this.loading = false;
      }
    });

    this.adminService.getActivitySummary().subscribe({
      next: (summary: UserActivitySummary) => {
        const total = summary.totalUsers || 1;
        this.activity = [
          { label: 'Daily active', value: Math.round((summary.dailyActive / total) * 100), tone: 'purple' },
          { label: 'Weekly active', value: Math.round((summary.weeklyActive / total) * 100), tone: 'green' },
          { label: 'Inactive 30d+', value: Math.round((summary.inactive30Days / total) * 100), tone: 'red' }
        ];
        this.summaryCards = [
          { label: 'Total users', value: String(summary.totalUsers), sub: `${summary.dailyActive} active today`, tone: 'green' },
          { label: 'Weekly active', value: String(summary.weeklyActive), sub: 'Signed in this week', tone: 'purple' },
          { label: 'Inactive 30d+', value: String(summary.inactive30Days), sub: 'No recent activity', tone: 'blue' }
        ];
      },
      error: () => {}
    });

    this.moderationService.getFlagged().subscribe({
      next: (items) => (this.flaggedCount = items.length),
      error: () => {}
    });
  }

  private toRow(r: AdminUserRecord): AdminUserRow {
    const status = (r.status ?? 'ACTIVE') as DisplayStatus | 'ACTIVE' | 'FLAGGED' | 'BLACKLISTED';
    const displayStatus: DisplayStatus =
      status === 'FLAGGED' ? 'Flagged' : status === 'BLACKLISTED' ? 'Blacklisted' : 'Active';
    const roleLabel = this.formatRole(r.role);
    return {
      id: r.id,
      initials: getInitials(r.name),
      name: r.name,
      email: r.email,
      role: roleLabel,
      department: r.department || r.college || '—',
      status: displayStatus,
      tone: displayStatus === 'Flagged' ? 'red' : displayStatus === 'Blacklisted' ? 'grey' : roleLabel === 'Teacher' ? 'amber' : 'purple',
      meta: r.lastActiveAt ? `Last active ${timeAgo(r.lastActiveAt)}` : 'Never signed in'
    };
  }

  private formatRole(role: string): string {
    if (!role) return 'Unassigned';
    if (role.includes('ADMIN')) return 'Admin';
    if (role.includes('TEACHER')) return 'Teacher';
    if (role.includes('STUDENT')) return 'Student';
    return 'Unassigned';
  }

  private buildTabs(): void {
    const students = this.allUsers.filter((u) => u.role === 'Student').length;
    const teachers = this.allUsers.filter((u) => u.role === 'Teacher').length;
    const flagged = this.allUsers.filter((u) => u.status === 'Flagged').length;
    const blacklisted = this.allUsers.filter((u) => u.status === 'Blacklisted').length;
    this.tabs = [
      `All users (${this.allUsers.length})`,
      `Students (${students})`,
      `Teachers (${teachers})`,
      `Flagged (${flagged})`,
      `Blacklisted (${blacklisted})`
    ];
    this.activeTab = this.tabs[0];
  }

  setTab(tab: string): void {
    this.activeTab = tab;
    this.applyTab();
  }

  private applyTab(): void {
    if (this.activeTab.startsWith('Students')) {
      this.users = this.allUsers.filter((u) => u.role === 'Student');
    } else if (this.activeTab.startsWith('Teachers')) {
      this.users = this.allUsers.filter((u) => u.role === 'Teacher');
    } else if (this.activeTab.startsWith('Flagged')) {
      this.users = this.allUsers.filter((u) => u.status === 'Flagged');
    } else if (this.activeTab.startsWith('Blacklisted')) {
      this.users = this.allUsers.filter((u) => u.status === 'Blacklisted');
    } else {
      this.users = this.allUsers;
    }
  }

  getStatusClass(status: DisplayStatus): string {
    return {
      Active: 'pill--green',
      Flagged: 'pill--red',
      Blacklisted: 'pill--dark'
    }[status];
  }

  warnOrSuspend(user: AdminUserRow): void {
    this.setStatus(user, 'FLAGGED', `${user.name} has been flagged for review.`);
  }

  toggleBlacklist(user: AdminUserRow): void {
    if (user.status === 'Blacklisted') {
      this.setStatus(user, 'ACTIVE', `${user.name} has been restored.`);
    } else {
      this.setStatus(user, 'BLACKLISTED', `${user.name} has been blacklisted.`);
    }
  }

  private setStatus(user: AdminUserRow, status: 'ACTIVE' | 'FLAGGED' | 'BLACKLISTED', successMessage: string): void {
    this.updatingId = user.id;
    this.adminService.updateUserStatus(user.id, status).subscribe({
      next: () => {
        this.updatingId = null;
        this.notificationService.success(successMessage);
        this.load();
      },
      error: () => {
        this.updatingId = null;
        this.notificationService.error('Failed to update user status.');
      }
    });
  }

  addUser(): void {
    this.notificationService.info('Accounts are created through self-registration — invite the user to sign up instead.');
  }

  viewUser(user: AdminUserRow): void {
    this.notificationService.info(`${user.name} · ${user.email}`);
  }
}
