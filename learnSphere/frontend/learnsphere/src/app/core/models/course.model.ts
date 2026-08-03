/**
 * Course status type representing the lifecycle of a course.
 */
export type CourseStatus = 'DRAFT' | 'PENDING' | 'LIVE' | 'ARCHIVED';

/**
 * Interface representing a Course.
 * Used to type course information across student and teacher panels.
 */
export interface Course {
  id: string;
  title: string;
  description: string;
  teacherId: string;
  department: string;
  thumbnail: string;
  status: CourseStatus;
  price: number;
}

/**
 * Interface representing a Lecture of a Course.
 * Contains metadata and the HLS stream URL for playback.
 */
export interface Lecture {
  id: string;
  courseId: string;
  title: string;
  number: number;
  videoUrl: string;
  duration: number; // in seconds
  isDownloadable: boolean;
  status: 'PUBLISHED' | 'DRAFT' | 'PROCESSING';
}

/**
 * Interface representing a student's personal Note on a lecture moment.
 * Tied to a course and lecture at a specific playback position.
 */
export interface Note {
  id: string;
  courseId: string;
  courseName: string;
  lectureId: string;
  lectureLabel: string;     // e.g. "Lec 8"
  timestampSeconds: number; // playback position in seconds
  title: string;
  content: string;
  tags: string[];
  createdAt: string;        // ISO date string
}
