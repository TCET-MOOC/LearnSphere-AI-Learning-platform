import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { DiscussionService } from '@core/services/discussion.service';
import { StudentService } from '../services/student.service';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { DiscussionThreadDto, ThreadPostDto } from '@core/models/social.model';
import { Course } from '@core/models/course.model';
import { getAvatarBg, getAvatarColor, getInitials } from '@core/utils/avatar.util';
import { timeAgo } from '@core/utils/time.util';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-discussion',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule,
    RouterModule,
    LucideAngularModule
  ],
  templateUrl: './discussion.component.html',
  styleUrls: ['./discussion.component.scss']
})
export class DiscussionComponent implements OnInit {
  courses: Course[] = [];
  activeCourseId: number | null = null;
  mineOnly = false;
  searchText = '';

  threads: DiscussionThreadDto[] = [];
  loading = false;
  newThreadTitle = '';
  newThreadContent = '';
  replyingToThreadId: number | null = null;
  replyText = '';

  constructor(
    private discussionService: DiscussionService,
    private studentService: StudentService,
    private authService: AuthService,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.studentService.getEnrolledCourses().subscribe({
      next: (courses) => {
        this.courses = courses;
        if (courses.length > 0) {
          this.selectCourse(courses[0].id);
        }
      },
      error: () => this.notify.error('Could not load your courses.')
    });
  }

  get activeCourseName(): string {
    return this.courses.find(c => c.id === this.activeCourseId)?.title || '';
  }

  get filteredThreads(): DiscussionThreadDto[] {
    let list = this.threads;
    if (this.mineOnly) {
      const uid = this.authService.currentUser?.id;
      list = list.filter(t => t.userId === uid);
    }
    if (this.searchText) {
      const q = this.searchText.toLowerCase();
      list = list.filter(t => 
        (t.title && t.title.toLowerCase().includes(q)) || 
        (t.content && t.content.toLowerCase().includes(q)) || 
        (t.authorName && t.authorName.toLowerCase().includes(q))
      );
    }
    return list;
  }

  selectCourse(courseId: number): void {
    this.activeCourseId = courseId;
    this.mineOnly = false;
    this.loadThreads();
  }

  toggleMine(): void {
    this.mineOnly = !this.mineOnly;
  }

  loadThreads(): void {
    if (!this.activeCourseId) return;
    this.loading = true;
    this.discussionService.getThreads(this.activeCourseId).subscribe({
      next: (threads) => {
        this.threads = threads;
        this.loading = false;
      },
      error: () => {
        this.threads = [];
        this.loading = false;
        this.notify.error('Could not load discussion threads.');
      }
    });
  }

  submitThread(): void {
    const title = this.newThreadTitle.trim();
    const content = this.newThreadContent.trim();
    if (!title || !content || !this.activeCourseId) return;

    this.discussionService.createThread(this.activeCourseId, { title, content }).subscribe({
      next: () => {
        this.newThreadTitle = '';
        this.newThreadContent = '';
        this.loadThreads();
      },
      error: () => this.notify.error('Could not create discussion thread.')
    });
  }

  toggleReply(threadId: number): void {
    this.replyingToThreadId = this.replyingToThreadId === threadId ? null : threadId;
    this.replyText = '';
  }

  submitReply(thread: DiscussionThreadDto): void {
    const body = this.replyText.trim();
    if (!body) return;

    this.discussionService.postReply(thread.threadId, body).subscribe({
      next: (newPost) => {
        this.replyText = '';
        this.replyingToThreadId = null;
        if (!thread.posts) {
          thread.posts = [];
        }
        thread.posts.push(newPost);
      },
      error: () => this.notify.error('Could not post reply.')
    });
  }

  isMyThread(thread: DiscussionThreadDto): boolean {
    return thread.userId === this.authService.currentUser?.id;
  }

  canDeleteThread(thread: DiscussionThreadDto): boolean {
    return this.isMyThread(thread);
  }

  removeThread(thread: DiscussionThreadDto): void {
    if (!window.confirm('Delete this thread and all replies?')) return;
    this.discussionService.deleteThread(thread.threadId).subscribe({
      next: () => this.loadThreads(),
      error: () => this.notify.error('Could not delete thread.')
    });
  }

  canDeletePost(post: ThreadPostDto): boolean {
    return post.userId === this.authService.currentUser?.id;
  }

  removePost(thread: DiscussionThreadDto, post: ThreadPostDto): void {
    if (!window.confirm('Delete this reply?')) return;
    this.discussionService.deletePost(post.postId).subscribe({
      next: () => {
        if (thread.posts) {
          thread.posts = thread.posts.filter(p => p.postId !== post.postId);
        }
      },
      error: () => this.notify.error('Could not delete reply.')
    });
  }

  get myInitials(): string { return getInitials(this.authService.currentUser?.fullName); }
  get myBg(): string { return getAvatarBg(this.authService.currentUser?.fullName); }
  get myColor(): string { return getAvatarColor(this.authService.currentUser?.fullName); }

  getInitials(name: string): string { return getInitials(name); }
  getBg(name: string): string { return getAvatarBg(name); }
  getColor(name: string): string { return getAvatarColor(name); }
  timeAgo(iso: string): string { return timeAgo(iso); }
}
