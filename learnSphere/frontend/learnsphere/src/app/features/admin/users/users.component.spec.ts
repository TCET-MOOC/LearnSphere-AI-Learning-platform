import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { LucideAngularModule, ChevronLeft, ChevronRight, Eye, Plus, Search, Users, X } from 'lucide-angular';
import { UsersComponent } from './users.component';
import { AdminService } from '../services/admin.service';
import { ModerationService } from '../services/moderation.service';
import { NotificationService } from '@core/services/notification.service';

/**
 * W7-12 (audit.md D5; AUD-12 inspect part, BR-03/AC-03): User management pages on the server,
 * takes its tab counts from the server, and Inspect loads the account's roles, payments and
 * audit history, with role assign/revoke through the admin role endpoints.
 */
describe('Admin UsersComponent (W7-12)', () => {
  let fixture: ComponentFixture<UsersComponent>;
  let el: HTMLElement;
  let admin: Record<string, ReturnType<typeof vi.fn>>;
  let notify: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn>; info: ReturnType<typeof vi.fn> };

  const user = (id: number, name: string, role = 'STUDENT', status = 'ACTIVE') => ({
    id, name, email: `${name.split(' ')[0].toLowerCase()}@learnsphere.test`, role, department: 'CS', college: 'TCET',
    status, lastActiveAt: null, createdAt: '2026-01-01T00:00:00',
  });
  const page = (content: unknown[], pageNo = 0, totalElements = 42, totalPages = 3) =>
    of({ content, page: pageNo, size: 20, totalElements, totalPages });

  const detail = {
    user: user(2, 'Aarav Sharma'),
    roles: [{ id: 11, userId: 2, roleId: 1, roleName: 'STUDENT' }],
    payments: [{
      id: 5, courseId: 9, courseTitle: 'Data Structures', amount: 499, currency: 'INR', status: 'SUCCESS',
      gatewayPaymentId: 'pay_abc123', createdAt: '2026-02-01T10:00:00', paidAt: '2026-02-01T10:01:00',
    }],
    audit: [{ id: 70, actorId: 3, action: 'USER_STATUS_CHANGED', before: { status: 'ACTIVE' }, after: { status: 'FLAGGED' }, createdAt: '2026-03-01T10:00:00' }],
  };

  async function setUp(overrides: Record<string, ReturnType<typeof vi.fn>> = {}) {
    admin = {
      getUsers: vi.fn((q: { page?: number }) => page([user(2, 'Aarav Sharma'), user(5, 'Meera Iyer', 'TEACHER')], q?.page ?? 0)),
      getUserCounts: vi.fn(() => of({ all: 42, students: 30, teachers: 10, flagged: 2, blacklisted: 1 })),
      getUserDetail: vi.fn(() => of(detail)),
      getRoles: vi.fn(() => of([{ roleId: 1, name: 'STUDENT' }, { roleId: 2, name: 'TEACHER' }, { roleId: 3, name: 'ADMIN' }])),
      assignRole: vi.fn(() => of({ id: 12, userId: 2, roleId: 2, roleName: 'TEACHER' })),
      revokeRole: vi.fn(() => of(undefined)),
      updateUserStatus: vi.fn(() => of(user(2, 'Aarav Sharma'))),
      getActivitySummary: vi.fn(() => of({ totalUsers: 42, dailyActive: 4, weeklyActive: 20, inactive30Days: 5 })),
      ...overrides,
    };
    notify = { success: vi.fn(), error: vi.fn(), info: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [UsersComponent, LucideAngularModule.pick({ ChevronLeft, ChevronRight, Eye, Plus, Search, Users, X })],
      providers: [
        provideRouter([]),
        { provide: AdminService, useValue: admin },
        { provide: ModerationService, useValue: { getFlagged: () => of([]) } },
        { provide: NotificationService, useValue: notify },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(true) }) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(UsersComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    el = fixture.nativeElement;
  }

  const button = (text: string) =>
    Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.trim() === text || b.getAttribute('aria-label') === text) as
      HTMLButtonElement | undefined;
  const settle = async () => { fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges(); };
  const lastQuery = () => admin['getUsers'].mock.calls.at(-1)![0];

  it('asks the server for one page and takes the tab counts from the server', async () => {
    await setUp();
    expect(lastQuery()).toEqual({ page: 0, size: 20 });
    expect(el.querySelectorAll('.user-row:not(.user-row--head)').length).toBe(2);
    const tabs = Array.from(el.querySelectorAll('.tab')).map((t) => t.textContent!.trim());
    expect(tabs).toEqual(['All users (42)', 'Students (30)', 'Teachers (10)', 'Flagged (2)', 'Blacklisted (1)']);
  });

  it('tabs filter on the server and restart at the first page', async () => {
    await setUp();
    button('Next page')!.click();
    await settle();
    button('Students (30)')!.click();
    await settle();
    expect(lastQuery()).toEqual({ role: 'STUDENT', page: 0, size: 20 });
    button('Flagged (2)')!.click();
    await settle();
    expect(lastQuery()).toEqual({ status: 'FLAGGED', page: 0, size: 20 });
  });

  it('pages forward and back with the pager', async () => {
    await setUp();
    expect(el.querySelector('.pager')?.textContent).toContain('Page 1 of 3');
    expect(button('Previous page')!.disabled).toBe(true);
    button('Next page')!.click();
    await settle();
    expect(lastQuery()).toEqual({ page: 1, size: 20 });
    expect(el.querySelector('.pager')?.textContent).toContain('Page 2 of 3');
    button('Previous page')!.click();
    await settle();
    expect(lastQuery()).toEqual({ page: 0, size: 20 });
  });

  it('search is sent to the server after typing stops', async () => {
    vi.useFakeTimers();
    try {
      await setUp();
      const input = el.querySelector('input[aria-label="Search users"]') as HTMLInputElement;
      input.value = 'meera';
      input.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      expect(lastQuery()).toEqual({ page: 0, size: 20 });
      vi.advanceTimersByTime(350);
      fixture.detectChanges();
      expect(lastQuery()).toEqual({ search: 'meera', page: 0, size: 20 });
    } finally {
      vi.useRealTimers();
    }
  });

  it('Inspect loads the account detail: roles, payments and audit history', async () => {
    await setUp();
    (el.querySelector('.action-set button') as HTMLButtonElement).click();
    await settle();
    expect(admin['getUserDetail']).toHaveBeenCalledWith(2);
    const modal = el.querySelector('.user-inspect-modal') as HTMLElement;
    expect(modal.querySelector('.inspect-roles')?.textContent).toContain('Student');
    expect(modal.querySelector('.inspect-payments')?.textContent).toContain('pay_abc123');
    expect(modal.querySelector('.inspect-payments')?.textContent).toContain('Data Structures');
    expect(modal.querySelector('.inspect-audit')?.textContent).toContain('Status changed');
    expect(modal.querySelector('.inspect-audit')?.textContent).toContain('ACTIVE → FLAGGED');
  });

  it('assigns a role the account does not hold, then reloads the detail', async () => {
    await setUp();
    (el.querySelector('.action-set button') as HTMLButtonElement).click();
    await settle();
    const select = el.querySelector('select[aria-label="Role to assign"]') as HTMLSelectElement;
    expect(select.options[0].textContent!.trim()).toBe('Choose a role');
    const options = Array.from(select.options).slice(1).map((o) => o.textContent!.trim());
    expect(options).toEqual(['Teacher', 'Admin']);
    select.value = select.options[Array.from(select.options).findIndex((o) => o.textContent!.trim() === 'Teacher')].value;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    button('Assign role')!.click();
    await settle();
    expect(admin['assignRole']).toHaveBeenCalledWith(2, 2);
    expect(admin['getUserDetail']).toHaveBeenCalledTimes(2);
    expect(notify.success).toHaveBeenCalled();
  });

  it('revokes a role; a refused revoke shows the server reason and claims no success', async () => {
    await setUp({
      revokeRole: vi.fn(() => throwError(() => ({ status: 409, error: { detail: 'You cannot remove your own admin role.' } }))),
    });
    (el.querySelector('.action-set button') as HTMLButtonElement).click();
    await settle();
    button('Revoke Student')!.click();
    await settle();
    expect(admin['revokeRole']).toHaveBeenCalledWith(2, 1);
    expect(notify.error).toHaveBeenCalledWith('You cannot remove your own admin role.');
    expect(notify.success).not.toHaveBeenCalled();
  });

  it('a failed detail load says so inside Inspect', async () => {
    await setUp({ getUserDetail: vi.fn(() => throwError(() => ({ status: 500 }))) });
    (el.querySelector('.action-set button') as HTMLButtonElement).click();
    await settle();
    expect(el.querySelector('.user-inspect-modal')?.textContent).toContain('Could not load this account');
  });
});
