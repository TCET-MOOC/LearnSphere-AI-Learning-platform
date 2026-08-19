import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DiscussionService } from '@core/services/discussion.service';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { DiscussionPostDto } from '@core/models/social.model';
import { UserRole } from '@core/models/user.model';
import { getAvatarBg, getAvatarColor, getInitials } from '@core/utils/avatar.util';
import { timeAgo } from '@core/utils/time.util';

/**
 * LectureDiscussionComponent is a per-lecture discussion widget embedded in the
 * lecture player page (mirrors the @Input contract of the sibling
 * NotesDrawerComponent so it can be dropped in next to it).
 * Not yet wired into lecture.component.html — that file belongs to the
 * courses workstream. Usage once wired:
 *   <app-lecture-discussion [courseId]="courseId" [lectureId]="currentLecture.id"
 *     [lectureLabel]="currentLecture.title"></app-lecture-discussion>
 */
@Component({
  selector: 'app-lecture-discussion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="card">
      <div class="card-head">
        <h2>Lecture discussion</h2>
      </div>

      <div class="new-post">
        <textarea rows="2" placeholder="Ask a question about this lecture…" [(ngModel)]="newPostText"></textarea>
        <button class="post-btn" type="button" [disabled]="!newPostText.trim() || !lectureId" (click)="submitPost()">Post</button>
      </div>

      <p class="hint" *ngIf="loading">Loading discussion…</p>
      <div class="posts" *ngIf="!loading">
        <div class="post" *ngFor="let post of posts">
          <div class="post-ava" [style.background]="getBg(post.authorName)" [style.color]="getColor(post.authorName)">{{ getInitials(post.authorName) }}</div>
          <div class="post-body">
            <div class="post-head">
              <span class="post-author">{{ post.authorName }}</span>
              <span class="post-time">{{ timeAgo(post.createdAt) }}</span>
              <button class="del-btn" type="button" *ngIf="canDelete(post)" (click)="remove(post)">Delete</button>
            </div>
            <p class="post-text">{{ post.body }}</p>

            <div class="replies" *ngIf="post.replies.length">
              <div class="reply" *ngFor="let reply of post.replies">
                <div class="post-ava post-ava--sm" [style.background]="getBg(reply.authorName)" [style.color]="getColor(reply.authorName)">{{ getInitials(reply.authorName) }}</div>
                <div>
                  <div class="post-head">
                    <span class="post-author">{{ reply.authorName }}</span>
                    <span class="post-time">{{ timeAgo(reply.createdAt) }}</span>
                    <button class="del-btn" type="button" *ngIf="canDelete(reply)" (click)="remove(reply)">Delete</button>
                  </div>
                  <p class="post-text">{{ reply.body }}</p>
                </div>
              </div>
            </div>

            <button class="link-btn" type="button" (click)="toggleReply(post.id)">
              {{ replyingTo === post.id ? 'Cancel' : 'Reply' }}
            </button>
            <div class="reply-input" *ngIf="replyingTo === post.id">
              <input type="text" placeholder="Write a reply…" [(ngModel)]="replyText" (keydown.enter)="submitReply(post)" />
              <button class="post-btn" type="button" [disabled]="!replyText.trim()" (click)="submitReply(post)">Reply</button>
            </div>
          </div>
        </div>
        <p class="hint" *ngIf="posts.length === 0">No discussion yet for this lecture. Be the first to ask a question.</p>
      </div>
    </section>
  `,
  styles: [`
    .card { background: var(--bg-surface); border: 1px solid var(--border-color); border-radius: 12px; padding: 16px; color: var(--text-primary); }
    .card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
    .card-head h2 { margin: 0; font-size: 14px; color: var(--text-primary); font-weight: 600; }
    .hint { font-size: 12px; color: var(--text-muted); }
    .new-post { display: flex; flex-direction: column; gap: 8px; margin-bottom: 14px; }
    .new-post textarea { border: 1px solid var(--border-color); background: var(--bg-input); color: var(--text-primary); border-radius: 8px; padding: 8px 10px; font: 400 12.5px inherit; resize: vertical; outline: none; }
    .new-post textarea:focus { border-color: var(--brand-primary); }
    .post-btn { align-self: flex-end; border: none; background: var(--brand-primary); color: #fff; font: 600 11.5px inherit; padding: 6px 14px; border-radius: 8px; cursor: pointer; transition: background 0.15s ease; }
    .post-btn:hover:not(:disabled) { background: var(--brand-primary-hover); }
    .post-btn:disabled { opacity: .5; cursor: not-allowed; }
    .posts { display: flex; flex-direction: column; gap: 14px; }
    .post { display: flex; gap: 10px; }
    .post-ava { width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font: 700 11px inherit; flex-shrink: 0; }
    .post-ava--sm { width: 24px; height: 24px; font-size: 10px; }
    .post-body { flex: 1; }
    .post-head { display: flex; align-items: center; gap: 8px; }
    .post-author { font: 600 12.5px inherit; color: var(--text-primary); }
    .post-time { font-size: 11px; color: var(--text-muted); }
    .del-btn { margin-left: auto; border: none; background: transparent; color: var(--status-red-text); font-size: 11px; cursor: pointer; }
    .post-text { margin: 4px 0 6px; font: 400 12.5px inherit; color: var(--text-secondary); line-height: 1.45; }
    .link-btn { border: none; background: transparent; color: var(--brand-primary); font: 600 11px inherit; cursor: pointer; padding: 0; }
    .replies { margin: 8px 0 8px 10px; padding-left: 10px; border-left: 2px solid var(--border-color); display: flex; flex-direction: column; gap: 10px; }
    .reply { display: flex; gap: 8px; }
    .reply-input { display: flex; gap: 8px; margin-top: 8px; }
    .reply-input input { flex: 1; border: 1px solid var(--border-color); background: var(--bg-input); color: var(--text-primary); border-radius: 8px; padding: 6px 10px; font: 400 12px inherit; outline: none; }
    .reply-input input:focus { border-color: var(--brand-primary); }
  `]
})
export class LectureDiscussionComponent implements OnChanges {
  @Input() courseId?: number;
  @Input() courseName = '';
  @Input() lectureId?: number;
  @Input() lectureLabel = '';

  posts: DiscussionPostDto[] = [];
  loading = false;
  newPostText = '';
  replyingTo: number | null = null;
  replyText = '';

  constructor(
    private discussionService: DiscussionService,
    private authService: AuthService,
    private notify: NotificationService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['lectureId']) {
      this.reload();
    }
  }

  reload(): void {
    if (!this.lectureId) {
      this.posts = [];
      return;
    }
    this.loading = true;
    this.discussionService.getLectureDiscussion(this.lectureId).subscribe({
      next: (posts) => {
        this.posts = posts;
        this.loading = false;
      },
      error: () => {
        this.posts = [];
        this.loading = false;
      }
    });
  }

  submitPost(): void {
    const body = this.newPostText.trim();
    if (!body || !this.lectureId) return;
    this.discussionService.createPost({ lectureId: this.lectureId, body }).subscribe({
      next: () => {
        this.newPostText = '';
        this.reload();
      },
      error: () => this.notify.error('Could not post to discussion.')
    });
  }

  toggleReply(postId: number): void {
    this.replyingTo = this.replyingTo === postId ? null : postId;
    this.replyText = '';
  }

  submitReply(post: DiscussionPostDto): void {
    const body = this.replyText.trim();
    if (!body) return;
    this.discussionService.createPost({ lectureId: this.lectureId, parentPostId: post.id, body }).subscribe({
      next: () => {
        this.replyText = '';
        this.replyingTo = null;
        this.reload();
      },
      error: () => this.notify.error('Could not post reply.')
    });
  }

  canDelete(post: DiscussionPostDto): boolean {
    const currentUserId = this.authService.currentUser?.id;
    const isAdmin = this.authService.currentUser?.role === UserRole.ADMIN;
    return isAdmin || post.authorId === currentUserId;
  }

  remove(post: DiscussionPostDto): void {
    if (!window.confirm('Delete this post?')) return;
    this.discussionService.deletePost(post.id).subscribe({
      next: () => this.reload(),
      error: () => this.notify.error('Could not delete post.')
    });
  }

  getInitials(name: string): string { return getInitials(name); }
  getBg(name: string): string { return getAvatarBg(name); }
  getColor(name: string): string { return getAvatarColor(name); }
  timeAgo(iso: string): string { return timeAgo(iso); }
}
