import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { Bookmark } from '@core/models/course.model';

export interface CreateBookmarkRequest {
  lectureId: number;
  timestampSeconds: number;
  label: string;
}

/**
 * BookmarkService owns API integrations for a student's saved lecture
 * timestamps (bookmarks) — list, create, delete.
 */
@Injectable({
  providedIn: 'root'
})
export class BookmarkService {
  constructor(private apiService: ApiService) {}

  getBookmarks(): Observable<Bookmark[]> {
    return this.apiService.get<Bookmark[]>('/bookmarks');
  }

  getBookmarksByLecture(lectureId: number): Observable<Bookmark[]> {
    return this.apiService.get<Bookmark[]>(`/bookmarks/lecture/${lectureId}`);
  }

  createBookmark(request: CreateBookmarkRequest): Observable<Bookmark> {
    return this.apiService.post<Bookmark>('/bookmarks', request);
  }

  deleteBookmark(id: number): Observable<void> {
    return this.apiService.delete<void>(`/bookmarks/${id}`);
  }

  deleteBookmarkByLecture(lectureId: number): Observable<void> {
    return this.apiService.delete<void>(`/bookmarks/lecture/${lectureId}`);
  }
}
