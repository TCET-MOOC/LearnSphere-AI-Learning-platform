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

/** One of the teacher's tests on the assessments screen (`GET /teacher/assessments`). */
export interface TeacherTestSummary {
  testId: number;
  title: string;
  courseId: number | null;
  courseTitle: string | null;
  lectureId: number | null;
  lectureTitle: string | null;
  questionCount: number;
  textQuestionCount: number;
  pendingCount: number;
}

/** A TEXT answer waiting for a grade (`GET /teacher/assessments/pending`); never carries the key. */
export interface PendingAnswer {
  answerId: number;
  attemptId: number;
  testId: number;
  testTitle: string;
  courseTitle: string | null;
  studentId: number;
  studentName: string | null;
  questionId: number;
  questionText: string;
  maxMarks: number | null;
  answerText: string | null;
  submittedAt: string | null;
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

  /** The teacher's tests with question and pending-grade counts (W7-16, SCR-35). */
  getTeacherAssessments(): Observable<TeacherTestSummary[]> {
    return this.apiService.get<TeacherTestSummary[]>('/teacher/assessments');
  }

  /** TEXT answers on finished attempts that still need marks; optionally one test. */
  getPendingAnswers(testId?: number): Observable<PendingAnswer[]> {
    return this.apiService.get<PendingAnswer[]>(`/teacher/assessments/pending${testId ? `?testId=${testId}` : ''}`);
  }

  /** Grades one answer; the server bounds the score by the question and recomputes the attempt. */
  gradeAnswer(answerId: number, marksAwarded: number): Observable<{ studentAnswerId: number; marksAwarded: number }> {
    return this.apiService.patch<{ studentAnswerId: number; marksAwarded: number }>(`/answers/${answerId}/grade`, { marksAwarded });
  }

  extractAiQuestions(transcript: string, count: number = 3): Observable<any> {
    return this.apiService.post<any>('/ai/extract-questions', { transcript, count });
  }
}
