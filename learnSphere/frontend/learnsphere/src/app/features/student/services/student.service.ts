import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Note } from '@core/models/course.model';
import { ApiService } from '@core/services/api.service';

/**
 * StudentService handles all API integrations for the student workspace.
 * Resolves course lists, details, lecture states, watch progress, and notes CRUD.
 */
@Injectable({
  providedIn: 'root'
})
export class StudentService {
  constructor(private apiService: ApiService) {}

  /**
   * Fetches the list of courses the student is enrolled in.
   */
  getEnrolledCourses(): Observable<any[]> {
    return this.apiService.get<any[]>('/student/courses/enrolled');
  }

  /**
   * Fetches specific course details by ID.
   */
  getCourseDetail(courseId: string): Observable<any> {
    return this.apiService.get<any>(`/student/courses/${courseId}`);
  }

  /**
   * Fetches specific lecture details by course ID and lecture ID.
   */
  getLecture(courseId: string, lectureId: string): Observable<any> {
    return this.apiService.get<any>(`/student/courses/${courseId}/lectures/${lectureId}`);
  }

  /**
   * Marks a lecture as watched and completed by the student.
   */
  markLectureComplete(lectureId: string): Observable<any> {
    return this.apiService.post<any>(`/student/lectures/${lectureId}/complete`, {});
  }

  /**
   * Gets the last saved watch progress of a specific lecture.
   */
  getWatchProgress(lectureId: string): Observable<number> {
    return this.apiService.get<number>(`/student/lectures/${lectureId}/progress`);
  }

  /**
   * Fetches notes, optionally filtered by courseId.
   */
  getNotes(courseId?: string): Observable<Note[]> {
    const endpoint = courseId && courseId !== 'all'
      ? `/student/notes?courseId=${courseId}`
      : '/student/notes';
    return this.apiService.get<Note[]>(endpoint);
  }

  /**
   * Adds a new note via POST request.
   */
  createNote(note: Partial<Note>): Observable<Note> {
    return this.apiService.post<Note>('/student/notes', note);
  }

  /**
   * Updates an existing note via PUT request.
   */
  updateNote(noteId: string, updatedFields: Partial<Note>): Observable<Note> {
    return this.apiService.put<Note>(`/student/notes/${noteId}`, updatedFields);
  }

  /**
   * Deletes a note by ID.
   */
  deleteNote(noteId: string): Observable<{ success: boolean }> {
    return this.apiService.delete<{ success: boolean }>(`/student/notes/${noteId}`);
  }

  /**
   * Exports student notes in the requested format.
   * If format is 'clipboard', fetches the notes and copies them to the system clipboard.
   * For PDF / Course formats, requests the backend export pipeline.
   */
  exportNotes(format: 'pdf' | 'course' | 'clipboard', courseId?: string): Observable<boolean> {
    if (format === 'clipboard') {
      return new Observable<boolean>(subscriber => {
        this.getNotes(courseId).subscribe({
          next: (notes) => {
            const textContent = notes
              .map(n => `[${n.courseName} - ${n.lectureLabel} @ ${n.timestampSeconds}s]\nTitle: ${n.title}\nContent: ${n.content}\nTags: ${n.tags.join(', ')}`)
              .join('\n\n');
            if (navigator.clipboard) {
              navigator.clipboard.writeText(textContent).then(() => {
                subscriber.next(true);
                subscriber.complete();
              }).catch(err => {
                console.error('Clipboard copy failed:', err);
                subscriber.next(false);
                subscriber.complete();
              });
            } else {
              console.warn('Clipboard API not supported in this browser environment.');
              subscriber.next(false);
              subscriber.complete();
            }
          },
          error: (err) => {
            subscriber.error(err);
          }
        });
      });
    }

    return this.apiService.post<boolean>('/student/notes/export', { format, courseId });
  }
}