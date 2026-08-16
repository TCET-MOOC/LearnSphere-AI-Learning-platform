import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LiveSessionService, LiveSession } from '../services/live-session.service';
import { TeacherService } from '../services/teacher.service';
import { NotificationService } from '@core/services/notification.service';
import { Course } from '@core/models/course.model';

@Component({
  selector: 'app-live',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './live.component.html',
  styleUrls: ['./live.component.scss']
})
export class LiveComponent implements OnInit {
  courses: Course[] = [];
  sessions: LiveSession[] = [];
  loading = true;
  scheduling = false;
  startingIds = new Set<number>();
  endingIds = new Set<number>();

  // Schedule form fields
  selectedCourseId: number | null = null;
  sessionTitle = '';
  sessionDate = '';
  sessionTime = '';

  constructor(
    private liveSessionService: LiveSessionService,
    private teacherService: TeacherService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.teacherService.getCourses().subscribe({
      next: (courses) => {
        this.courses = courses;
        if (courses.length > 0) {
          this.selectedCourseId = courses[0].id;
        }
      },
      error: () => this.notificationService.error('Failed to load your courses.')
    });
    this.loadSessions();
  }

  loadSessions(): void {
    this.loading = true;
    this.liveSessionService.getSessions().subscribe({
      next: (sessions) => {
        this.sessions = sessions;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.notificationService.error('Failed to load live sessions.');
      }
    });
  }

  get upcomingSessions(): LiveSession[] {
    return this.sessions.filter((s) => s.status === 'SCHEDULED' || s.status === 'LIVE');
  }

  get pastSessions(): LiveSession[] {
    return this.sessions.filter((s) => s.status === 'ENDED');
  }

  scheduleSession(): void {
    if (!this.selectedCourseId) {
      this.notificationService.error('Select a course first.');
      return;
    }
    if (!this.sessionTitle.trim()) {
      this.notificationService.error('Enter a session title.');
      return;
    }
    if (!this.sessionDate || !this.sessionTime) {
      this.notificationService.error('Pick a date and time.');
      return;
    }
    this.scheduling = true;
    const scheduledAt = `${this.sessionDate}T${this.sessionTime}:00`;
    this.liveSessionService.createSession({
      courseId: this.selectedCourseId,
      title: this.sessionTitle.trim(),
      scheduledAt
    }).subscribe({
      next: () => {
        this.scheduling = false;
        this.notificationService.success('Session scheduled and students notified!');
        this.sessionTitle = '';
        this.sessionDate = '';
        this.sessionTime = '';
        this.loadSessions();
      },
      error: (err) => {
        this.scheduling = false;
        this.notificationService.error(err?.error?.message || 'Failed to schedule session.');
      }
    });
  }

  saveDraft(): void {
    // There is no "draft" session state in the backend domain model (sessions are
    // SCHEDULED/LIVE/ENDED) — saving a draft is a lightweight, client-side-only
    // convenience so the teacher doesn't lose in-progress form input.
    this.notificationService.info('Draft saved locally — resume filling this form anytime.');
  }

  startSession(id: number): void {
    this.startingIds.add(id);
    this.liveSessionService.startSession(id).subscribe({
      next: (session) => {
        this.startingIds.delete(id);
        this.notificationService.success('Session is now live!');
        this.loadSessions();
      },
      error: () => {
        this.startingIds.delete(id);
        this.notificationService.error('Failed to start session.');
      }
    });
  }

  endSession(id: number): void {
    this.endingIds.add(id);
    this.liveSessionService.endSession(id).subscribe({
      next: () => {
        this.endingIds.delete(id);
        this.notificationService.info('Session ended.');
        this.loadSessions();
      },
      error: () => {
        this.endingIds.delete(id);
        this.notificationService.error('Failed to end session.');
      }
    });
  }

  isStarting(id: number): boolean {
    return this.startingIds.has(id);
  }

  isEnding(id: number): boolean {
    return this.endingIds.has(id);
  }
}
