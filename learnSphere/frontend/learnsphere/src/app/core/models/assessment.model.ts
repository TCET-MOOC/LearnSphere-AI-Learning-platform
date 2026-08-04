/**
 * Models for the Assessments (tests/quizzes) and Certificates domain.
 * Mirrors the DTOs returned by AssessmentController / TeacherAssessmentController /
 * CertificateController on the backend.
 */

/** A lightweight summary of a test, as shown in a course's assessment list. */
export interface Assessment {
  id: number;
  courseId: number;
  courseTitle?: string;
  title: string;
  durationMinutes: number;
  isRemedial: boolean;
  questionCount: number;
  scheduledAt?: string | null;
}

/** A question as seen by a student — never includes the correct answer. */
export interface Question {
  questionId: number;
  body: string;
  questionType: string; // e.g. 'MCQ' | 'SHORT_ANSWER' | 'ESSAY'
  marks: number;
  options?: string[] | null;
}

/** Full detail of a test, including its (student-safe) questions. */
export interface AssessmentDetail {
  id: number;
  courseId: number;
  title: string;
  durationMinutes: number;
  securityPolicy?: string;
  isRemedial: boolean;
  scheduledAt?: string | null;
  questions: Question[];
}

/** A student's attempt at a test. */
export interface TestAttempt {
  attemptId: number;
  testId: number;
  status: 'IN_PROGRESS' | 'COMPLETED' | string;
  score?: number | null;
  attemptedAt?: string;
}

/** Response returned after submitting a single answer. */
export interface StudentAnswerAck {
  studentAnswerId: number;
  questionId: number;
  answerText: string;
  saved: boolean;
}

/** Per-question review row shown after an attempt is finalized. */
export interface AnswerReview {
  questionId: number;
  questionBody: string;
  questionType: string;
  marks: number;
  options?: string[] | null;
  correctAnswer?: string | null;
  studentAnswer?: string | null;
  marksAwarded?: number | null;
}

/** Full review of a completed (or in-progress) attempt. */
export interface AttemptReview {
  attemptId: number;
  testId: number;
  testTitle: string;
  status: string;
  score?: number | null;
  maxScore: number;
  isRemedial?: boolean;
  attemptedAt?: string;
  answers: AnswerReview[];
}

/** A question being authored by a teacher (includes correctAnswer, unlike the student-facing Question). */
export interface QuestionDraft {
  body: string;
  questionType: string;
  marks: number;
  options?: string[];
  correctAnswer?: string;
}

export type CertificateType = 'STANDARD' | 'REMEDIAL';

export interface Certificate {
  id: number;
  courseId: number;
  courseTitle?: string;
  type: CertificateType;
  title: string;
  issuedAt: string;
}
