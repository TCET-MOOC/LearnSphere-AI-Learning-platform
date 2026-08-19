import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { StudentService } from '../services/student.service';
import { NotificationService } from '@core/services/notification.service';
import { StatCardComponent } from '@shared/components/stat-cards/stat-card.component';
import { StatusPillComponent } from '@shared/components/status-pills/status-pill.component';
import { LucideAngularModule } from 'lucide-angular';

export interface StudentLiveSession {
  id: number;
  courseId: number;
  courseTitle: string;
  title: string;
  scheduledAt: string;
  status: 'SCHEDULED' | 'LIVE' | 'ENDED';
  joinUrl?: string;
}

@Component({
  selector: 'app-student-live',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    StatCardComponent,
    StatusPillComponent,
    LucideAngularModule
  ],
  templateUrl: './live.component.html',
  styleUrls: ['./live.component.scss']
})
export class StudentLiveComponent implements OnInit {
  loading = true;
  sessions: StudentLiveSession[] = [];
  activeFilter: 'ALL' | 'LIVE' | 'UPCOMING' | 'PAST' = 'ALL';

  constructor(
    private studentService: StudentService,
    private notificationService: NotificationService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadSessions();
  }

  loadSessions(): void {
    this.loading = true;
    this.studentService.getLiveSessions().subscribe({
      next: (sessions) => {
        this.sessions = sessions || [];
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.notificationService.error('Failed to load live sessions.');
        this.cdr.markForCheck();
      }
    });
  }

  get liveNowSessions(): StudentLiveSession[] {
    return this.sessions.filter(s => s.status === 'LIVE');
  }

  get upcomingSessions(): StudentLiveSession[] {
    return this.sessions.filter(s => s.status === 'SCHEDULED');
  }

  get pastSessions(): StudentLiveSession[] {
    return this.sessions.filter(s => s.status === 'ENDED');
  }

  get filteredSessions(): StudentLiveSession[] {
    if (this.activeFilter === 'LIVE') return this.liveNowSessions;
    if (this.activeFilter === 'UPCOMING') return this.upcomingSessions;
    if (this.activeFilter === 'PAST') return this.pastSessions;
    return this.sessions;
  }

  joinSession(session: StudentLiveSession): void {
    if (session.status === 'LIVE') {
      this.router.navigate(['/student/live', session.id]);
    } else if (session.status === 'SCHEDULED') {
      this.notificationService.info('This session is scheduled. It will unlock when the instructor starts broadcasting.');
    } else {
      this.router.navigate(['/student/live', session.id]);
    }
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return 'TBA';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString(undefined, { 
        weekday: 'short', 
        month: 'short', 
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return dateStr;
    }
  }
}
