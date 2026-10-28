import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { Course, Lecture, LectureProgress, Note, ResumeTarget } from '@core/models/course.model';

export interface ExploreCourseFilters {
  status?: string;
  department?: string;
  teacherId?: number;
  search?: string;
}

/**
 * StudentService owns all student-facing API integrations: enrolled courses,
 * course/lecture detail, watch progress, and personal notes.
 */
@Injectable({
  providedIn: 'root'
})
export class StudentService {

  constructor(private apiService: ApiService) {}

  /** Courses the current student is enrolled in. */
  getEnrolledCourses(): Observable<Course[]> {
    return this.apiService.get<Course[]>('/student/courses');
  }

  /** Resolves the next incomplete content target for Continue/resume. */
  getResumeTarget(courseId: number): Observable<ResumeTarget> {
    return this.apiService.get<ResumeTarget>(`/student/courses/${courseId}/resume`);
  }

  /** Enrolled course detail (404s if not enrolled) — used to confirm access. */
  getCourseDetail(courseId: number): Observable<Course> {
    return this.apiService.get<Course>(`/student/courses/${courseId}`);
  }

  /** Public course detail — visible to anyone, enrolled or not (course landing page). */
  getPublicCourse(courseId: number): Observable<Course> {
    return this.apiService.get<Course>(`/courses/${courseId}`);
  }

  /** Public course browsing / explore list. */
  getExploreCourses(filters?: ExploreCourseFilters): Observable<Course[]> {
    const params: Record<string, string> = {};
    if (filters?.status) params['status'] = filters.status;
    if (filters?.department) params['department'] = filters.department;
    if (filters?.teacherId != null) params['teacherId'] = String(filters.teacherId);
    if (filters?.search?.trim()) params['search'] = filters.search.trim();
    return this.apiService.get<Course[]>('/courses', { params });
  }

  /** Modules belonging to a course (Section 3 of LMS_Entity_Reference.pdf). */
  getCourseModules(courseId: number): Observable<any[]> {
    return this.apiService.get<any[]>(`/courses/${courseId}/modules`);
  }

  /** Content items inside a module (Section 4 of LMS_Entity_Reference.pdf). */
  getModuleContents(moduleId: number): Observable<any[]> {
    return this.apiService.get<any[]>(`/modules/${moduleId}/contents`);
  }

  /** Single content item. */
  getContent(contentId: number): Observable<any> {
    return this.apiService.get<any>(`/contents/${contentId}`);
  }

  /** All lectures belonging to a public course (used by course detail page). */
  getCourseLectures(courseId: number): Observable<Lecture[]> {
    return this.apiService.get<Lecture[]>(`/courses/${courseId}/lectures`);
  }


  getLecture(courseId: number, lectureId: number): Observable<Lecture> {
    return this.apiService.get<Lecture>(`/student/courses/${courseId}/lectures/${lectureId}`);
  }

  markLectureComplete(lectureId: number): Observable<LectureProgress> {
    return this.apiService.post<LectureProgress>(`/student/lectures/${lectureId}/complete`, {});
  }

  unmarkLectureComplete(lectureId: number): Observable<LectureProgress> {
    return this.apiService.post<LectureProgress>(`/student/lectures/${lectureId}/uncomplete`, {});
  }

  getWatchProgress(lectureId: number): Observable<LectureProgress> {
    return this.apiService.get<LectureProgress>(`/student/lectures/${lectureId}/progress`);
  }

  getCourseProgress(courseId: number): Observable<LectureProgress[]> {
    return this.apiService.get<LectureProgress[]>(`/student/courses/${courseId}/progress`);
  }

  enrollInCourse(courseId: number): Observable<Course> {
    return this.apiService.post<Course>(`/student/courses/${courseId}/enroll`, {});
  }

  // --- Notes ---

  createNote(note: Partial<Note>): Observable<Note> {
    return this.apiService.post<Note>('/notes', note);
  }

  updateNote(noteId: number, note: Partial<Note>): Observable<Note> {
    return this.apiService.put<Note>(`/notes/${noteId}`, note);
  }

  deleteNote(noteId: number): Observable<void> {
    return this.apiService.delete<void>(`/notes/${noteId}`);
  }

  getNotes(courseId?: number): Observable<Note[]> {
    const params = courseId != null ? { courseId: String(courseId) } : undefined;
    return this.apiService.get<Note[]>('/notes', { params });
  }

  // --- Live Sessions ---

  getLiveSessions(): Observable<any[]> {
    return this.apiService.get<any[]>('/student/live-sessions');
  }

  getLiveSession(id: number): Observable<any> {
    return this.apiService.get<any>(`/student/live-sessions/${id}`);
  }
}

