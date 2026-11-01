/**
 * Shared by the phone Home resume card and the My Courses Continue bar: which lesson to
 * resume, from the server's resume target, with its position in the course.
 */
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Course } from '@core/models/course.model';
import { StudentService } from './services/student.service';

export interface ResumeLesson {
  courseId: number;
  courseTitle: string;
  title: string;
  /** 1-based lesson number in the course, 0 when unknown */
  index: number;
  total: number;
  percent: number;
  route: (string | number)[];
}

export function loadResumeLesson(studentService: StudentService, course: Course): Observable<ResumeLesson> {
  return forkJoin({
    target: studentService.getResumeTarget(course.id).pipe(catchError(() => of(null))),
    lectures: studentService.getCourseLectures(course.id).pipe(catchError(() => of([])))
  }).pipe(
    map(({ target, lectures }) => ({
      courseId: course.id,
      courseTitle: course.title,
      title: target?.title || course.title,
      index: target?.lectureId ? lectures.findIndex((l) => l.id === target.lectureId) + 1 : 0,
      total: lectures.length,
      percent: Math.round(target?.progressPercent ?? course.progressPercent ?? 0),
      route: target?.lectureId
        ? ['/student/courses', course.id, 'lectures', target.lectureId]
        : ['/student/courses', course.id, 'player']
    }))
  );
}
