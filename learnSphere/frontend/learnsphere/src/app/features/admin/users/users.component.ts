import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { LucideAngularModule } from 'lucide-angular';
import { AdminService, AdminUserRecord, UserActivitySummary } from '../services/admin.service';
import { ModerationService } from '../services/moderation.service';
import { NotificationService } from '@core/services/notification.service';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
import { timeAgo } from '@core/utils/time.util';
import { getInitials } from '@core/utils/avatar.util';

type DisplayStatus = 'Active' | 'Flagged' | 'Blacklisted';

export interface AdminUserRow {
  id: number;
  initials: string;
  name: string;
  email: string;
  role: string;
  department: string;
  status: DisplayStatus;
  tone: 'purple' | 'green' | 'amber' | 'red' | 'grey';
  meta: string;
  createdAt: string;
  lastActiveAt: string | null;
}

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatDialogModule,
    LucideAngularModule
  ],
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss']
})
export class UsersComponent implements OnInit {
  allUsers: AdminUserRow[] = [];
  users: AdminUserRow[] = [];
  tabs: string[] = ['All users'];
  activeTab = 'All users';
  searchQuery = '';

  summaryCards: { label: string; value: string; sub: string; tone: string }[] = [];
  activity: { label: string; value: number; tone: string }[] = [];
  flaggedCount = 0;
  loading = false;
  updatingId: number | null = null;
  selectedUserForInspect: AdminUserRow | null = null;

  constructor(
    private adminService: AdminService,
    private moderationService: ModerationService,
    private notificationService: NotificationService,
    private dialog: MatDialog,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.adminService.getUsers().subscribe({
      next: (records) => {
        this.allUsers = records.map((r) => this.toRow(r));
        this.buildTabs();
        this.applyFilters();
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.notificationService.error('Failed to load users.');
        this.loading = false;
        this.cdr.markForCheck();
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
        this.cdr.markForCheck();
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
      meta: r.lastActiveAt ? `Last active ${timeAgo(r.lastActiveAt)}` : 'Never signed in',
      createdAt: r.createdAt,
      lastActiveAt: r.lastActiveAt
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
    if (!this.tabs.some(t => t.startsWith(this.activeTab.split(' ')[0]))) {
      this.activeTab = this.tabs[0];
    }
  }

  setTab(tab: string): void {
    this.activeTab = tab;
    this.applyFilters();
    this.cdr.markForCheck();
  }

  onSearchChange(): void {
    this.applyFilters();
    this.cdr.markForCheck();
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.applyFilters();
    this.cdr.markForCheck();
  }

  private applyFilters(): void {
    let result = this.allUsers;

    if (this.activeTab.startsWith('Students')) {
      result = result.filter((u) => u.role === 'Student');
    } else if (this.activeTab.startsWith('Teachers')) {
      result = result.filter((u) => u.role === 'Teacher');
    } else if (this.activeTab.startsWith('Flagged')) {
      result = result.filter((u) => u.status === 'Flagged');
    } else if (this.activeTab.startsWith('Blacklisted')) {
      result = result.filter((u) => u.status === 'Blacklisted');
    }

    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase().trim();
      result = result.filter(u =>
        u.name.toLowerCase().includes(q) ||
        u.email.toLowerCase().includes(q) ||
        u.department.toLowerCase().includes(q) ||
        u.role.toLowerCase().includes(q)
      );
    }

    this.users = result;
  }

  getStatusClass(status: DisplayStatus): string {
    return {
      Active: 'pill--green',
      Flagged: 'pill--red',
      Blacklisted: 'pill--dark'
    }[status];
  }

  warnOrSuspend(user: AdminUserRow): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Flag User Account',
        message: `Are you sure you want to flag "${user.name}" (${user.email}) for moderation review?`,
        confirmLabel: 'Flag User',
        cancelLabel: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.setStatus(user, 'FLAGGED', `${user.name} has been flagged for review.`);
      }
    });
  }

  toggleBlacklist(user: AdminUserRow): void {
    const isRestoring = user.status === 'Blacklisted';
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: isRestoring ? 'Restore User Account' : 'Blacklist User Account',
        message: isRestoring
          ? `Are you sure you want to restore access for "${user.name}"? They will regain portal access.`
          : `Are you sure you want to blacklist "${user.name}"? They will be immediately blocked from signing in.`,
        confirmLabel: isRestoring ? 'Restore' : 'Blacklist',
        cancelLabel: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        if (isRestoring) {
          this.setStatus(user, 'ACTIVE', `${user.name} has been restored.`);
        } else {
          this.setStatus(user, 'BLACKLISTED', `${user.name} has been blacklisted.`);
        }
      }
    });
  }

  private setStatus(user: AdminUserRow, status: 'ACTIVE' | 'FLAGGED' | 'BLACKLISTED', successMessage: string): void {
    this.updatingId = user.id;
    this.adminService.updateUserStatus(user.id, status).subscribe({
      next: () => {
        this.updatingId = null;
        this.notificationService.success(successMessage);
        if (this.selectedUserForInspect && this.selectedUserForInspect.id === user.id) {
          this.selectedUserForInspect.status = status === 'FLAGGED' ? 'Flagged' : status === 'BLACKLISTED' ? 'Blacklisted' : 'Active';
          this.selectedUserForInspect.tone = this.selectedUserForInspect.status === 'Flagged' ? 'red' : this.selectedUserForInspect.status === 'Blacklisted' ? 'grey' : this.selectedUserForInspect.role === 'Teacher' ? 'amber' : 'purple';
        }
        this.load();
      },
      error: () => {
        this.updatingId = null;
        this.notificationService.error('Failed to update user status.');
      }
    });
  }

  addUser(): void {
    this.notificationService.info('Accounts are created through self-registration or college roster invitations.');
  }

  viewUser(user: AdminUserRow): void {
    this.selectedUserForInspect = user;
    this.cdr.markForCheck();
  }

  closeUserInspect(): void {
    this.selectedUserForInspect = null;
    this.cdr.markForCheck();
  }
}
