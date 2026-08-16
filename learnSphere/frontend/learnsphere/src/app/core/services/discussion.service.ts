import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { DiscussionPostDto, DiscussionPostRequest } from '@core/models/social.model';

/**
 * DiscussionService — shared across all surfaces that render course/lecture
 * discussion threads: the student/teacher course-wide discussion pages and
 * the per-lecture discussion widget embedded in the lecture player.
 */
@Injectable({
  providedIn: 'root'
})
export class DiscussionService {
  constructor(private api: ApiService) {}

  getCourseDiscussion(courseId: number): Observable<DiscussionPostDto[]> {
    return this.api.get<DiscussionPostDto[]>(`/courses/${courseId}/discussion`);
  }

  getLectureDiscussion(lectureId: number): Observable<DiscussionPostDto[]> {
    return this.api.get<DiscussionPostDto[]>(`/lectures/${lectureId}/discussion`);
  }

  createPost(payload: DiscussionPostRequest): Observable<DiscussionPostDto> {
    return this.api.post<DiscussionPostDto>('/discussion', payload);
  }

  deletePost(id: number): Observable<void> {
    return this.api.delete<void>(`/discussion/${id}`);
  }
}
