import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { Assignment, AssignmentSubmission } from '@core/models/course.model';

@Injectable({
  providedIn: 'root'
})
export class AssignmentService {

  constructor(private api: ApiService) {}

  getAssignment(assignmentId: number): Observable<Assignment> {
    return this.api.get<Assignment>(`/assignments/${assignmentId}`);
  }

  getAssignmentsByModule(moduleId: number): Observable<Assignment[]> {
    return this.api.get<Assignment[]>(`/modules/${moduleId}/assignments`);
  }

  getAssignmentsByCourse(courseId: number): Observable<Assignment[]> {
    return this.api.get<Assignment[]>(`/courses/${courseId}/assignments`);
  }

  createAssignment(moduleId: number, assignment: Partial<Assignment>): Observable<Assignment> {

    return this.api.post<Assignment>(`/modules/${moduleId}/assignments`, assignment);
  }

  updateAssignment(assignmentId: number, assignment: Partial<Assignment>): Observable<Assignment> {
    return this.api.put<Assignment>(`/assignments/${assignmentId}`, assignment);
  }

  deleteAssignment(assignmentId: number): Observable<void> {
    return this.api.delete<void>(`/assignments/${assignmentId}`);
  }

  getSubmissions(assignmentId: number): Observable<AssignmentSubmission[]> {
    return this.api.get<AssignmentSubmission[]>(`/assignments/${assignmentId}/submissions`);
  }

  submitAssignment(assignmentId: number, submission: Partial<AssignmentSubmission>): Observable<AssignmentSubmission> {
    return this.api.post<AssignmentSubmission>(`/assignments/${assignmentId}/submissions`, submission);
  }

  getMySubmissions(): Observable<AssignmentSubmission[]> {
    return this.api.get<AssignmentSubmission[]>('/submissions/me');
  }

  gradeSubmission(submissionId: number, marksAwarded: number, feedback?: string): Observable<AssignmentSubmission> {
    return this.api.patch<AssignmentSubmission>(`/submissions/${submissionId}/grade`, { marksAwarded, feedback });
  }
}
