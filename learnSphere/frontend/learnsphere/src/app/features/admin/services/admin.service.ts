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
   * Fetches users for the User Management screen, optionally filtered.
   */
  getUsers(filters?: { role?: string; status?: string; search?: string }): Observable<AdminUserRecord[]> {
    const params = new URLSearchParams();
    if (filters?.role) params.set('role', filters.role);
    if (filters?.status) params.set('status', filters.status);
    if (filters?.search) params.set('search', filters.search);
    const query = params.toString();
    return this.apiService.get<AdminUserRecord[]>(`/admin/users${query ? '?' + query : ''}`);
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
