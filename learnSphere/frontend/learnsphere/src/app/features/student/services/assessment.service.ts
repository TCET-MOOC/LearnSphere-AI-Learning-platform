import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import {
  Assessment,
  AssessmentDetail,
  AttemptReview,
  StudentAnswerAck,
  TestAttempt
} from '@core/models/assessment.model';

/**
 * AssessmentService owns the student-facing assessment (test/quiz) API
 * integrations: browsing available tests, starting/answering/finalizing an
 * attempt, and reviewing a past attempt.
 */
@Injectable({
  providedIn: 'root'
})
export class AssessmentService {

  constructor(private apiService: ApiService) {}

  /** Tests available for a course, or all visible tests if courseId is omitted. */
  getAssessments(courseId?: number): Observable<Assessment[]> {
    const params = courseId != null ? { courseId: String(courseId) } : undefined;
    return this.apiService.get<Assessment[]>('/assessments', { params });
  }

  /** Full test detail (questions, without correct answers). */
  getAssessment(id: number): Observable<AssessmentDetail> {
    return this.apiService.get<AssessmentDetail>(`/assessments/${id}`);
  }

  /** Starts a new attempt for the current student. */
  startAssessment(id: number): Observable<TestAttempt> {
    return this.apiService.post<TestAttempt>(`/assessments/${id}/start`, {});
  }

  /** Submits (or overwrites) the answer to a single question within an attempt. */
  submitAnswer(attemptId: number, questionId: number, answerText: string): Observable<StudentAnswerAck> {
    return this.apiService.post<StudentAnswerAck>(`/assessments/attempts/${attemptId}/answers`, {
      questionId,
      answerText
    });
  }

  /** Grades the attempt and returns the full per-question review. */
  finalizeAssessment(attemptId: number): Observable<AttemptReview> {
    return this.apiService.post<AttemptReview>(`/assessments/attempts/${attemptId}/finalize`, {});
  }

  /** Re-fetches the review for a past attempt (must belong to the current student). */
  getAttemptReview(attemptId: number): Observable<AttemptReview> {
    return this.apiService.get<AttemptReview>(`/assessments/attempts/${attemptId}`);
  }
}
