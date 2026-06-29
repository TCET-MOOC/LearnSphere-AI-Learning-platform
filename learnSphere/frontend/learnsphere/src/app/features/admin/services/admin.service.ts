import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { College, AffiliationRequest } from '@core/models/user.model';
import { ApiService } from '@core/services/api.service';

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
  verifyCollege(collegeId: string): Observable<{ success: boolean }> {
    return this.apiService.put<{ success: boolean }>(`/admin/colleges/${collegeId}/verify`, {});
  }

  /**
   * Rejects a pending college, removing it.
   */
  rejectCollege(collegeId: string): Observable<{ success: boolean }> {
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
  approveAffiliation(requestId: string): Observable<{ success: boolean }> {
    return this.apiService.put<{ success: boolean }>(`/admin/colleges/affiliations/${requestId}/approve`, {});
  }

  /**
   * Rejects a teacher's affiliation request.
   */
  rejectAffiliation(requestId: string): Observable<{ success: boolean }> {
    return this.apiService.put<{ success: boolean }>(`/admin/colleges/affiliations/${requestId}/reject`, {});
  }
}
