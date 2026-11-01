/** Approved 360 px My Courses: pure rules for a course row and the filter chip counts. */
import { MY_COURSES_MOBILE_COPY } from '@core/content/my-courses-mobile.content';

export interface CourseLike {
  lectureCount?: number | null;
  completedLecturesCount?: number | null;
  progressPercent?: number | null;
  completed?: boolean | null;
}

export interface CourseRowState {
  percent: number;
  /** "5 of 12", or '' when the lesson count is unknown */
  progress: string;
  action: string;
  completed: boolean;
}

export const isCourseComplete = (c: CourseLike) => !!c.completed || (c.progressPercent ?? 0) >= 100;

export function courseRowState(c: CourseLike): CourseRowState {
  const copy = MY_COURSES_MOBILE_COPY;
  const total = c.lectureCount ?? 0;
  const done = Math.min(c.completedLecturesCount ?? 0, total);
  const completed = isCourseComplete(c);
  const percent = completed ? 100 : Math.max(0, Math.min(100, Math.round(c.progressPercent ?? 0)));
  const progress = total > 0 ? copy.progress(done, total) : '';
  let action: string;
  if (completed) action = copy.completed;
  else if (done === 0) action = copy.start;
  else action = copy.resume(Math.min(done + 1, total));
  return { percent, progress, action, completed };
}

export function filterCounts(courses: ReadonlyArray<CourseLike>): { ALL: number; IN_PROGRESS: number; COMPLETED: number } {
  const completed = courses.filter(isCourseComplete).length;
  return { ALL: courses.length, IN_PROGRESS: courses.length - completed, COMPLETED: completed };
}
