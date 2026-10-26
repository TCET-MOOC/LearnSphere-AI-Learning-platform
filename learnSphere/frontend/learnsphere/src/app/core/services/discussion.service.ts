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

  // --- Two-Tier Discussion Forum (Section 10 of LMS_Entity_Reference.pdf) ---

  getThreads(courseId: number): Observable<any[]> {
    return this.api.get<any[]>(`/courses/${courseId}/threads`);
  }

  getThread(threadId: number): Observable<any> {
    return this.api.get<any>(`/threads/${threadId}`);
  }

  createThread(courseId: number, thread: { title: string; content: string }): Observable<any> {
    return this.api.post<any>(`/courses/${courseId}/threads`, thread);
  }

  updateThread(threadId: number, thread: { title?: string; content?: string }): Observable<any> {
    return this.api.put<any>(`/threads/${threadId}`, thread);
  }

  deleteThread(threadId: number): Observable<void> {
    return this.api.delete<void>(`/threads/${threadId}`);
  }

  getThreadPosts(threadId: number): Observable<any[]> {
    return this.api.get<any[]>(`/threads/${threadId}/posts`);
  }

  postReply(threadId: number, body: string, parentPostId?: number): Observable<any> {
    return this.api.post<any>(`/threads/${threadId}/posts`, { body, parentPostId });
  }

  updatePost(postId: number, body: string): Observable<any> {
    return this.api.put<any>(`/posts/${postId}`, { body });
  }

  deletePost(id: number): Observable<void> {
    return this.api.delete<void>(`/posts/${id}`);
  }
}

