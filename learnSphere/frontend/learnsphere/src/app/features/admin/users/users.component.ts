import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { LucideAngularModule } from 'lucide-angular';
import { Subject, Subscription, debounceTime, distinctUntilChanged } from 'rxjs';
import {
  AdminService, AdminUserCounts, AdminUserDetail, AdminUserRecord, RoleRecord, UserActivitySummary
} from '../services/admin.service';
import { ModerationService } from '../services/moderation.service';
import { NotificationService } from '@core/services/notification.service';
import { ErrorHandlerService } from '@core/services/error-handler.service';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
import { timeAgo } from '@core/utils/time.util';
import { getInitials } from '@core/utils/avatar.util';
import { A11yCopy } from '@core/content/a11y.content';
import { ADMIN_USERS_COPY } from '@core/content/admin-users.content';
import { DialogDirective } from '@shared/directives/dialog.directive';

type DisplayStatus = 'Active' | 'Flagged' | 'Blacklisted';
type TabKey = 'all' | 'students' | 'teachers' | 'flagged' | 'blacklisted';
interface UserTab { key: TabKey; label: string; filter: { role?: string; status?: string } }

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

/**
 * User management (SCR-51). Since W7-12 (audit.md D5; AUD-12, BR-03) the list is one server
 * page at a time, tabs and search filter on the server, tab counts come from
 * `GET /admin/users/counts`, and Inspect loads the account's roles, recent payments and audit
 * history from `GET /admin/users/{id}`, with role assign/revoke through `/users/{id}/roles`.
 */
@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [DialogDirective,
    CommonModule,
    FormsModule,
    RouterModule,
    MatDialogModule,
    LucideAngularModule
  ],
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss']
})
export class UsersComponent implements OnInit, OnDestroy {
  protected readonly a11y = A11yCopy.text;
  protected readonly copy = ADMIN_USERS_COPY;
  readonly pageSize = 20;

  users: AdminUserRow[] = [];
  tabs: UserTab[] = [];
  activeTab: TabKey = 'all';
  searchQuery = '';
  page = 0;
  totalPages = 0;

  summaryCards: { label: string; value: string; sub: string; tone: string }[] = [];
  activity: { label: string; value: number; tone: string }[] = [];
  flaggedCount = 0;
  loading = false;
  updatingId: number | null = null;
  selectedUserForInspect: AdminUserRow | null = null;

  // Inspect (W7-12): loaded from GET /admin/users/{id}
  detail: AdminUserDetail | null = null;
  detailLoading = false;
  detailError = false;
  allRoles: RoleRecord[] = [];
  roleToAssign: number | null = null;
  roleBusy = false;

  private counts: AdminUserCounts | null = null;
  private readonly search$ = new Subject<string>();
  private searchSub?: Subscription;

  constructor(
    private adminService: AdminService,
    private moderationService: ModerationService,
    private notificationService: NotificationService,
    private errors: ErrorHandlerService,
    private dialog: MatDialog,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.searchSub = this.search$.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => {
      this.page = 0;
      this.loadPage();
    });
    this.buildTabs();
    this.load();
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
  }

  load(): void {
    this.loadPage();
    this.loadCounts();

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

  /** One page from the server; the tab and the search filter on the server before paging. */
  private loadPage(): void {
    this.loading = true;
    this.cdr.markForCheck();
    const tab = this.tabs.find((t) => t.key === this.activeTab);
    const search = this.searchQuery.trim();
    this.adminService.getUsers({
      ...(tab?.filter ?? {}),
      ...(search ? { search } : {}),
      page: this.page,
      size: this.pageSize,
    }).subscribe({
      next: (result) => {
        this.users = result.content.map((r) => this.toRow(r));
        this.page = result.page;
        this.totalPages = result.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.notificationService.error('Failed to load users.');
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private loadCounts(): void {
    this.adminService.getUserCounts().subscribe({
      next: (counts) => {
        this.counts = counts;
        this.buildTabs();
        this.cdr.markForCheck();
      },
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
    const c = this.counts ?? { all: 0, students: 0, teachers: 0, flagged: 0, blacklisted: 0 };
    const t = this.copy.tabs;
    this.tabs = [
      { key: 'all', label: t.all(c.all), filter: {} },
      { key: 'students', label: t.students(c.students), filter: { role: 'STUDENT' } },
      { key: 'teachers', label: t.teachers(c.teachers), filter: { role: 'TEACHER' } },
      { key: 'flagged', label: t.flagged(c.flagged), filter: { status: 'FLAGGED' } },
      { key: 'blacklisted', label: t.blacklisted(c.blacklisted), filter: { status: 'BLACKLISTED' } },
    ];
  }

  setTab(tab: UserTab): void {
    this.activeTab = tab.key;
    this.page = 0;
    this.loadPage();
  }

  onSearchChange(): void {
    this.search$.next(this.searchQuery.trim());
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.search$.next('');
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages || page === this.page) return;
    this.page = page;
    this.loadPage();
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
          this.loadDetail(user.id);
        }
        this.loadPage();
        this.loadCounts();
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
    this.detail = null;
    this.roleToAssign = null;
    this.loadDetail(user.id);
    if (this.allRoles.length === 0) {
      this.adminService.getRoles().subscribe({
        next: (roles) => { this.allRoles = roles; this.cdr.markForCheck(); },
        error: () => {}
      });
    }
    this.cdr.markForCheck();
  }

  closeUserInspect(): void {
    this.selectedUserForInspect = null;
    this.detail = null;
    this.cdr.markForCheck();
  }

  private loadDetail(userId: number): void {
    this.detailLoading = true;
    this.detailError = false;
    this.adminService.getUserDetail(userId).subscribe({
      next: (detail) => {
        if (this.selectedUserForInspect?.id !== userId) return;
        this.detail = detail;
        this.detailLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.detailLoading = false;
        this.detailError = true;
        this.cdr.markForCheck();
      }
    });
  }

  roleName(name: string): string {
    return this.copy.roleNames[name] ?? name;
  }

  /** Roles from the vocabulary that the inspected account does not hold yet. */
  get assignableRoles(): RoleRecord[] {
    const held = new Set((this.detail?.roles ?? []).map((r) => r.roleId));
    return this.allRoles.filter((r) => !held.has(r.roleId));
  }

  assignRole(): void {
    const user = this.selectedUserForInspect;
    const role = this.allRoles.find((r) => r.roleId === Number(this.roleToAssign));
    if (!user || !role || this.roleBusy) return;
    this.roleBusy = true;
    this.adminService.assignRole(user.id, role.roleId).subscribe({
      next: () => {
        this.roleBusy = false;
        this.roleToAssign = null;
        this.notificationService.success(this.copy.inspect.assigned(this.roleName(role.name)));
        this.loadDetail(user.id);
        this.loadPage();
        this.loadCounts();
      },
      error: (err) => {
        this.roleBusy = false;
        this.notificationService.error(this.errors.extractMessage(err, this.copy.inspect.roleFailed));
        this.cdr.markForCheck();
      }
    });
  }

  revokeRole(roleId: number, roleName: string): void {
    const user = this.selectedUserForInspect;
    if (!user || this.roleBusy) return;
    this.roleBusy = true;
    this.adminService.revokeRole(user.id, roleId).subscribe({
      next: () => {
        this.roleBusy = false;
        this.notificationService.success(this.copy.inspect.revoked(this.roleName(roleName)));
        this.loadDetail(user.id);
        this.loadPage();
        this.loadCounts();
      },
      error: (err) => {
        this.roleBusy = false;
        this.notificationService.error(this.errors.extractMessage(err, this.copy.inspect.roleFailed));
        this.cdr.markForCheck();
      }
    });
  }

  auditLabel(action: string): string {
    return this.copy.inspect.actions[action] ?? action;
  }

  /** "ACTIVE → FLAGGED" for a status change; the role name for a role change. */
  auditChange(entry: AdminUserDetail['audit'][number]): string {
    const before = entry.before ?? {};
    const after = entry.after ?? {};
    if ('status' in before || 'status' in after) {
      return `${before['status'] ?? '—'} → ${after['status'] ?? '—'}`;
    }
    const role = (after['role'] ?? before['role']) as string | undefined;
    return role ? this.roleName(role) : '';
  }
}
