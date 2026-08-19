import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { DiscussionService } from '@core/services/discussion.service';
import { StudentService } from '../services/student.service';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { DiscussionPostDto } from '@core/models/social.model';
import { Course } from '@core/models/course.model';
import { getAvatarBg, getAvatarColor, getInitials } from '@core/utils/avatar.util';
import { timeAgo } from '@core/utils/time.util';
import { LucideAngularModule, MessageCircle, CornerDownLeft, Trash2, Plus, Search, BookOpen } from 'lucide-angular';

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

  posts: DiscussionPostDto[] = [];
  loading = false;
  newPostText = '';
  replyingTo: number | null = null;
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

  get filteredPosts(): DiscussionPostDto[] {
    let list = this.posts;
    if (this.mineOnly) {
      const uid = this.authService.currentUser?.id;
      list = list.filter(p => p.authorId === uid);
    }
    if (this.searchText) {
      const q = this.searchText.toLowerCase();
      list = list.filter(p => p.body.toLowerCase().includes(q) || p.authorName.toLowerCase().includes(q));
    }
    return list;
  }

  selectCourse(courseId: number): void {
    this.activeCourseId = courseId;
    this.mineOnly = false;
    this.loadPosts();
  }

  toggleMine(): void {
    this.mineOnly = !this.mineOnly;
  }

  private loadPosts(): void {
    if (!this.activeCourseId) return;
    this.loading = true;
    this.discussionService.getCourseDiscussion(this.activeCourseId).subscribe({
      next: (posts) => {
        this.posts = posts;
        this.loading = false;
      },
      error: () => {
        this.posts = [];
        this.loading = false;
        this.notify.error('Could not load discussion.');
      }
    });
  }

  submitPost(): void {
    const body = this.newPostText.trim();
    if (!body || !this.activeCourseId) return;
    this.discussionService.createPost({ courseId: this.activeCourseId, body }).subscribe({
      next: () => {
        this.newPostText = '';
        this.loadPosts();
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
    if (!body || !this.activeCourseId) return;
    this.discussionService.createPost({ courseId: this.activeCourseId, parentPostId: post.id, body }).subscribe({
      next: () => {
        this.replyText = '';
        this.replyingTo = null;
        this.loadPosts();
      },
      error: () => this.notify.error('Could not post reply.')
    });
  }

  isMine(post: DiscussionPostDto): boolean {
    return post.authorId === this.authService.currentUser?.id;
  }

  canDelete(post: DiscussionPostDto): boolean {
    return this.isMine(post);
  }

  remove(post: DiscussionPostDto): void {
    if (!window.confirm('Delete this post?')) return;
    this.discussionService.deletePost(post.id).subscribe({
      next: () => this.loadPosts(),
      error: () => this.notify.error('Could not delete post.')
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
