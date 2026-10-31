import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { DiscussionThreadDto, ThreadPostDto } from '@core/models/social.model';

/**
 * DiscussionService — canonical two-tier course discussion forum service
 * conforming to Section 10 of LMS_Entity_Reference.pdf.
 * Endpoints:
 * - GET /api/courses/{courseId}/threads
 * - POST /api/courses/{courseId}/threads
 * - GET /api/threads/{threadId}
 * - PUT /api/threads/{threadId}
 * - DELETE /api/threads/{threadId}
 * - GET /api/threads/{threadId}/posts
 * - POST /api/threads/{threadId}/posts
 * - PUT /api/posts/{postId}
 * - DELETE /api/posts/{postId}
 */
@Injectable({
  providedIn: 'root'
})
export class DiscussionService {
  constructor(private api: ApiService) {}

  getThreads(courseId: number): Observable<DiscussionThreadDto[]> {
    return this.api.get<DiscussionThreadDto[]>(`/courses/${courseId}/threads`);
  }

  getThread(threadId: number): Observable<DiscussionThreadDto> {
    return this.api.get<DiscussionThreadDto>(`/threads/${threadId}`);
  }

  createThread(courseId: number, thread: { title: string; content: string }): Observable<DiscussionThreadDto> {
    return this.api.post<DiscussionThreadDto>(`/courses/${courseId}/threads`, thread);
  }

  updateThread(threadId: number, thread: { title?: string; content?: string }): Observable<DiscussionThreadDto> {
    return this.api.put<DiscussionThreadDto>(`/threads/${threadId}`, thread);
  }

  deleteThread(threadId: number): Observable<void> {
    return this.api.delete<void>(`/threads/${threadId}`);
  }

  getThreadPosts(threadId: number): Observable<ThreadPostDto[]> {
    return this.api.get<ThreadPostDto[]>(`/threads/${threadId}/posts`);
  }

  postReply(threadId: number, body: string, parentPostId?: number): Observable<ThreadPostDto> {
    return this.api.post<ThreadPostDto>(`/threads/${threadId}/posts`, { body, parentPostId });
  }

  updatePost(postId: number, body: string): Observable<ThreadPostDto> {
    return this.api.put<ThreadPostDto>(`/posts/${postId}`, { body });
  }

  deletePost(id: number): Observable<void> {
    return this.api.delete<void>(`/posts/${id}`);
  }
}
