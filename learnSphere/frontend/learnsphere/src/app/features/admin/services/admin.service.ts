import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { College, AffiliationRequest } from '@core/models/user.model';
import { ApiService } from '@core/services/api.service';

export interface AdminUserRecord {
  id: number;
  name: string;
  email: string;
  role: string;
  department: string | null;
  college: string | null;
  status: 'ACTIVE' | 'FLAGGED' | 'BLACKLISTED';
  lastActiveAt: string | null;
  createdAt: string;
}

/** The backend's standard page envelope (`Dto/PageResponse`, P0-09). */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AdminUserQuery {
  role?: string;
  status?: string;
  search?: string;
  page: number;
  size: number;
}

export interface AdminUserCounts {
  all: number;
  students: number;
  teachers: number;
  flagged: number;
  blacklisted: number;
}

export interface RoleRecord {
  roleId: number;
  name: string;
}

export interface RoleAssignment {
  id: number;
  userId: number;
  roleId: number;
  roleName: string;
}

export interface AdminUserDetail {
  user: AdminUserRecord;
  roles: RoleAssignment[];
  payments: {
    id: number;
    courseId: number | null;
    courseTitle: string | null;
    amount: number | null;
    currency: string | null;
    status: string | null;
    gatewayPaymentId: string | null;
    createdAt: string | null;
    paidAt: string | null;
  }[];
  audit: {
    id: number;
    actorId: number | null;
    action: string;
    before: Record<string, unknown> | null;
    after: Record<string, unknown> | null;
    createdAt: string;
  }[];
}

export interface UserActivitySummary {
  totalUsers: number;
  dailyActive: number;
  weeklyActive: number;
  inactive30Days: number;
}

export interface TopTeacher {
  id: number;
  name: string;
  students: number;
  earnings: number;
}

/**
 * AdminService handles all API integrations for the admin workspace.
 * Resolves college registrations, verifications, rejections, and teacher affiliation moderations.
 */
@Injectable({
  providedIn: 'root'
})
export class AdminService {
  constructor(private apiService: ApiService) {}

  /**
   * Fetches the registered colleges list.
   */
  getColleges(): Observable<College[]> {
    return this.apiService.get<College[]>('/admin/colleges');
  }

  /**
   * Verifies a pending college.
   */
  verifyCollege(collegeId: number): Observable<{ success: boolean }> {
    return this.apiService.put<{ success: boolean }>(`/admin/colleges/${collegeId}/verify`, {});
  }

  /**
   * Rejects a pending college, removing it.
   */
  rejectCollege(collegeId: number): Observable<{ success: boolean }> {
    return this.apiService.delete<{ success: boolean }>(`/admin/colleges/${collegeId}`);
  }

  /**
   * Registers a new college manually.
   */
  createCollege(college: Partial<College>): Observable<College> {
    return this.apiService.post<College>('/admin/colleges', college);
  }

  /**
   * Fetches all pending teacher affiliation requests.
   */
  getAffiliationRequests(): Observable<AffiliationRequest[]> {
    return this.apiService.get<AffiliationRequest[]>('/admin/colleges/affiliations');
  }

  /**
   * Approves a teacher's affiliation request.
   */
  approveAffiliation(requestId: number): Observable<{ success: boolean }> {
    return this.apiService.put<{ success: boolean }>(`/admin/colleges/affiliations/${requestId}/approve`, {});
  }

  /**
   * Rejects a teacher's affiliation request.
   */
  rejectAffiliation(requestId: number): Observable<{ success: boolean }> {
    return this.apiService.put<{ success: boolean }>(`/admin/colleges/affiliations/${requestId}/reject`, {});
  }

  /**
   * One page of users for User Management (W7-12: `PageResponse`; filters apply before paging).
   */
  getUsers(query: AdminUserQuery): Observable<PageResponse<AdminUserRecord>> {
    const params = new URLSearchParams();
    if (query.role) params.set('role', query.role);
    if (query.status) params.set('status', query.status);
    if (query.search) params.set('search', query.search);
    params.set('page', String(query.page));
    params.set('size', String(query.size));
    return this.apiService.get<PageResponse<AdminUserRecord>>(`/admin/users?${params.toString()}`);
  }

  /** Tab counts over every account (W7-12). */
  getUserCounts(): Observable<AdminUserCounts> {
    return this.apiService.get<AdminUserCounts>('/admin/users/counts');
  }

  /** Inspect: account, role assignments, recent payments and audit history (W7-12, audit D5). */
  getUserDetail(userId: number): Observable<AdminUserDetail> {
    return this.apiService.get<AdminUserDetail>(`/admin/users/${userId}`);
  }

  /** The role vocabulary with ids, for assigning roles. */
  getRoles(): Observable<RoleRecord[]> {
    return this.apiService.get<RoleRecord[]>('/roles');
  }

  /** Assigns a role (admin-only, audited; BR-03). */
  assignRole(userId: number, roleId: number): Observable<RoleAssignment> {
    return this.apiService.post<RoleAssignment>(`/users/${userId}/roles`, { roleId });
  }

  /** Revokes a role (admin-only, audited; an admin cannot revoke their own ADMIN role). */
  revokeRole(userId: number, roleId: number): Observable<void> {
    return this.apiService.delete<void>(`/users/${userId}/roles/${roleId}`);
  }

  /**
   * Updates a user's status (ACTIVE / FLAGGED / BLACKLISTED).
   */
  updateUserStatus(userId: number, status: 'ACTIVE' | 'FLAGGED' | 'BLACKLISTED'): Observable<AdminUserRecord> {
    return this.apiService.put<AdminUserRecord>(`/admin/users/${userId}/status`, { status });
  }

  /**
   * Real daily/weekly/inactive counts derived from lastActiveAt.
   */
  getActivitySummary(): Observable<UserActivitySummary> {
    return this.apiService.get<UserActivitySummary>('/admin/users/activity-summary');
  }

  /**
   * Top-earning teachers, ranked by royalty balance and enrolled students.
   */
  getTopTeachers(limit = 3): Observable<TopTeacher[]> {
    return this.apiService.get<TopTeacher[]>(`/admin/users/top-teachers?limit=${limit}`);
  }
}
