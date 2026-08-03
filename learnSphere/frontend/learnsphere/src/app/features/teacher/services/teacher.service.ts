import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { StudentStanding } from '@core/models/user.model';
import { ApiService } from '@core/services/api.service';

/**
 * TeacherService handles all API integrations for the teacher workspace.
 * Resolves student standings and warning nudge triggers.
 */
@Injectable({
  providedIn: 'root'
})
export class TeacherService {
  constructor(private apiService: ApiService) {}

  /**
   * Fetches the student standings for a specific course.
   */
  getStudentStandings(courseId: string): Observable<StudentStanding[]> {
    return this.apiService.get<StudentStanding[]>(`/teacher/courses/${courseId}/standings`);
  }

  /**
   * Sends a nudge/warning notification to a specific student about their course progress.
   */
  sendNudge(studentId: string, courseId: string): Observable<{ success: boolean; message: string }> {
    return this.apiService.post<{ success: boolean; message: string }>(`/teacher/courses/${courseId}/nudge/${studentId}`, {});
  }
}
