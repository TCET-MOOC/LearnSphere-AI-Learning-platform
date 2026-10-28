/**
 * Course status type representing the lifecycle of a course.
 */
export type CourseStatus = 'DRAFT' | 'PENDING' | 'LIVE' | 'ARCHIVED';

/**
 * Interface representing a Course.
 * Used to type course information across student and teacher panels.
 */
export interface Course {
  id: number;
  title: string;
  description: string;
  teacherId: number;
  teacherName?: string;
  department: string;
  thumbnail: string;
  status: CourseStatus;
  price: number;
  createdAt?: string;
  lectureCount?: number;
  completedLecturesCount?: number;
  progressPercent?: number;
  completed?: boolean;
}

/**
 * Interface representing a Lecture of a Course.
 * Contains metadata and the HLS stream URL for playback.
 */
export interface Lecture {
  id: number;
  courseId: number;
  title: string;
  number: number;
  videoUrl: string;
  duration: number; // in seconds
  isDownloadable: boolean;
  status: 'PUBLISHED' | 'DRAFT' | 'PROCESSING';
}

/**
 * Interface representing a student's personal Note on a lecture moment.
 * Tied to a course and lecture at a specific playback position (optional —
 * a note can also be a freestanding note not linked to any lecture).
 */
export interface Note {
  id: number;
  courseId?: number;
  courseName?: string;
  lectureId?: number;
  lectureLabel?: string;     // e.g. "Lec 8"
  timestampSeconds: number; // playback position in seconds
  title: string;
  content: string;
  tags: string[];
  createdAt: string;        // ISO date string
}

/**
 * A student's progress on a single lecture.
 */
export interface LectureProgress {
  id?: number;
  lectureId: number;
  progressPercent: number;
  secondsWatched: number;
  completedAt?: string | null;
}

/**
 * A bookmark placed at a specific timestamp within a lecture.
 */
export interface Bookmark {
  id: number;
  lectureId: number;
  lectureTitle?: string;
  courseId?: number;
  courseName?: string;
  timestampSeconds: number;
  label: string;
  createdAt: string;
}

/**
 * Module within a course (Section 3 of LMS_Entity_Reference.pdf).
 */
export interface Module {
  moduleId: number;
  courseId: number;
  title: string;
  position: number;
  createdAt?: string;
  contents?: CourseContent[];
  assignments?: Assignment[];
}

/**
 * CourseContent (Section 4 of LMS_Entity_Reference.pdf)
 * Represents individual Video, PDF, or Quiz items inside a Module.
 */
export type ContentType = 'VIDEO' | 'PDF' | 'QUIZ';

export interface CourseContent {
  contentId: number;
  moduleId: number;
  contentType: ContentType;
  title: string;
  filePath: string;
  durationSeconds: number;
  position: number;
  uploadedAt?: string;
}

/**
 * Assignment (Section 4 of LMS_Entity_Reference.pdf)
 */
export interface Assignment {
  assignmentId: number;
  moduleId: number;
  title: string;
  description: string;
  maxMarks: number;
  dueDate: string;
  createdAt?: string;
}

/**
 * Assignment Submission (Section 8 of LMS_Entity_Reference.pdf)
 */
export interface AssignmentSubmission {
  submissionId?: number;
  assignmentId: number;
  studentId?: number;
  studentName?: string;
  submissionText?: string;
  fileUrl?: string;
  marksAwarded?: number | null;
  feedback?: string | null;
  submittedAt?: string;
}

/**
 * Course Feedback / Review (Section 9 of LMS_Entity_Reference.pdf)
 */
export interface CourseFeedback {
  feedbackId?: number;
  userId?: number;
  userName?: string;
  courseId: number;
  rating: number; // 1 to 5
  message?: string;
  createdAt?: string;
}

export interface CourseRatingSummary {
  courseId: number;
  averageRating: number;
  totalReviews: number;
}

export interface ResumeTarget {
  courseId: number;
  contentId?: number;
  lectureId?: number;
  moduleId?: number;
  moduleTitle?: string;
  title: string;
  contentType: string;
  position: number;
  progressPercent: number;
  secondsWatched: number;
  completed: boolean;
}

