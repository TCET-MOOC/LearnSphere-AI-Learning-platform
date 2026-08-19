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
