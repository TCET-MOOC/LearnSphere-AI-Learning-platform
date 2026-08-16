import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { QuestionDraft } from '@core/models/assessment.model';

export interface TeacherTestDraft {
  course: { id: number };
  title: string;
  durationMinutes: number;
  securityPolicy?: string;
  isRemedial?: boolean;
}

/**
 * TeacherAssessmentService lets a teacher author a test on one of their own
 * courses (create the test, then add questions to it). Backend enforces
 * that the course belongs to the authenticated teacher.
 */
@Injectable({
  providedIn: 'root'
})
export class TeacherAssessmentService {
  constructor(private apiService: ApiService) {}

  createTest(draft: TeacherTestDraft): Observable<any> {
    return this.apiService.post<any>('/teacher/tests', draft);
  }

  addQuestion(testId: number, question: QuestionDraft): Observable<any> {
    return this.apiService.post<any>(`/teacher/tests/${testId}/questions`, question);
  }
}
