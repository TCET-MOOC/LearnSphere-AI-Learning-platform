import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

// Placeholder service — replace method bodies with real API calls
// using api.service.ts from core/services/ once your backend is ready.
// Example: return this.apiService.get<Course[]>('/student/courses');

@Injectable({
  providedIn: 'root'
})
export class StudentService {

  constructor() {}

  getEnrolledCourses(): Observable<any[]> {
    return of([]);
  }

  getCourseDetail(courseId: string): Observable<any> {
    return of(null);
  }

  getLecture(courseId: string, lectureId: string): Observable<any> {
    return of(null);
  }

  markLectureComplete(lectureId: string): Observable<any> {
    return of({ success: true });
  }

  getWatchProgress(lectureId: string): Observable<number> {
    return of(0);
  }

  createNote(note: any): Observable<any> {
    return of({ id: Date.now().toString(), ...note });
  }

  updateNote(noteId: string, note: any): Observable<any> {
    return of({ id: noteId, ...note });
  }

  deleteNote(noteId: string): Observable<any> {
    return of({ success: true });
  }

  getNotes(courseId?: string): Observable<any[]> {
    return of([]);
  }
}