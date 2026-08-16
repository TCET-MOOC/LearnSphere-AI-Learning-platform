import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { StudentStanding } from '@core/models/user.model';
import { ApiService } from '@core/services/api.service';
import { Course, Lecture } from '@core/models/course.model';

/** A single row in GET /api/courses/trending. */
export interface TrendingCourse {
  id: number;
  title: string;
  thumbnail: string | null;
  teacherName: string | null;
  teacherId: number | null;
  enrolledCount: number;
  rank: number;
}

/**
 * TeacherService handles all API integrations for the teacher workspace.
 * Resolves student standings, warning nudge triggers, and course/lecture
 * authoring (create/update/delete) for the teacher's own courses.
 */
@Injectable({
  providedIn: 'root'
})
export class TeacherService {
  constructor(private apiService: ApiService) {}

  /**
   * Fetches the student standings for a specific course.
   */
  getStudentStandings(courseId: string): Observable<StudentStanding[]> {
    return this.apiService.get<StudentStanding[]>(`/teacher/courses/${courseId}/standings`);
  }

  /**
   * Sends a nudge/warning notification to a specific student about their course progress.
   */
  sendNudge(studentId: number, courseId: string): Observable<{ success: boolean; message: string }> {
    return this.apiService.post<{ success: boolean; message: string }>(`/teacher/courses/${courseId}/nudge/${studentId}`, {});
  }

  // --- Course CRUD ---

  /** All courses owned by the current teacher. */
  getCourses(): Observable<Course[]> {
    return this.apiService.get<Course[]>('/teacher/courses');
  }

  getCourse(id: number): Observable<Course> {
    return this.apiService.get<Course>(`/courses/${id}`);
  }

  createCourse(payload: Partial<Course>): Observable<Course> {
    return this.apiService.post<Course>('/teacher/courses', payload);
  }

  updateCourse(id: number, payload: Partial<Course>): Observable<Course> {
    return this.apiService.put<Course>(`/teacher/courses/${id}`, payload);
  }

  deleteCourse(id: number): Observable<void> {
    return this.apiService.delete<void>(`/teacher/courses/${id}`);
  }

  // --- Lecture CRUD ---

  getLectures(courseId: number): Observable<Lecture[]> {
    return this.apiService.get<Lecture[]>(`/courses/${courseId}/lectures`);
  }

  addLecture(courseId: number, payload: Partial<Lecture>): Observable<Lecture> {
    return this.apiService.post<Lecture>(`/teacher/courses/${courseId}/lectures`, payload);
  }

  updateLecture(lectureId: number, payload: Partial<Lecture>): Observable<Lecture> {
    return this.apiService.put<Lecture>(`/teacher/lectures/${lectureId}`, payload);
  }

  deleteLecture(lectureId: number): Observable<void> {
    return this.apiService.delete<void>(`/teacher/lectures/${lectureId}`);
  }

  // --- Trending ---

  /** Top LIVE courses platform-wide, ranked by enrollment count. */
  getTrendingCourses(limit?: number): Observable<TrendingCourse[]> {
    const options = limit ? { params: { limit } } : undefined;
    return this.apiService.get<TrendingCourse[]>('/courses/trending', options);
  }
}
