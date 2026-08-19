import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { QuestionDraft } from '@core/models/assessment.model';

export interface TeacherTestDraft {
  course: { id: number };
  lecture?: { id: number };
  title: string;
  durationMinutes: number;
  securityPolicy?: string;
  isRemedial?: boolean;
}

/**
 * TeacherAssessmentService lets a teacher author a test on one of their own
 * courses or lectures (create the test, then add questions to it with or without AI).
 */
@Injectable({
  providedIn: 'root'
})
export class TeacherAssessmentService {
  constructor(private apiService: ApiService) {}

  createTest(draft: TeacherTestDraft): Observable<any> {
    return this.apiService.post<any>('/teacher/tests', draft);
  }

  getTestsForCourse(courseId: number): Observable<any[]> {
    return this.apiService.get<any[]>(`/teacher/tests/course/${courseId}`);
  }

  getTestsForLecture(lectureId: number): Observable<any[]> {
    return this.apiService.get<any[]>(`/teacher/tests/lecture/${lectureId}`);
  }

  getTestById(testId: number): Observable<any> {
    return this.apiService.get<any>(`/teacher/tests/${testId}`);
  }

  deleteTest(testId: number): Observable<void> {
    return this.apiService.delete<void>(`/teacher/tests/${testId}`);
  }

  addQuestion(testId: number, question: QuestionDraft): Observable<any> {
    return this.apiService.post<any>(`/teacher/tests/${testId}/questions`, question);
  }

  addQuestionsBulk(testId: number, questions: QuestionDraft[]): Observable<any[]> {
    return this.apiService.post<any[]>(`/teacher/tests/${testId}/questions/bulk`, questions);
  }

  extractAiQuestions(transcript: string, count: number = 3): Observable<any> {
    return this.apiService.post<any>('/ai/extract-questions', { transcript, count });
  }
}
