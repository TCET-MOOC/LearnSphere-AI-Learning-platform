import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../../core/auth/auth.service';
import { LucideAngularModule } from 'lucide-angular';
import { Course } from '@core/models/course.model';
import { TEACHER_HOME_MOBILE_COPY } from '@core/content/teacher-home-mobile.content';
import { TeacherService } from '../services/teacher.service';
import { LiveSessionService } from '../services/live-session.service';
import { MessagingService } from '../services/messaging.service';
import { AwaitingItem, NextClass, awaitingReply, courseStatusLabel, nextClass } from './teacher-home-mobile';
import { firstName } from '../../student/dashboard/home-mobile';

/** Phones get the approved 360 px teacher Home; its requests are only made there. */
const PHONE_QUERY = '(max-width: 767.98px)';

@Component({
  selector: 'app-teacher-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  constructor(private authService: AuthService) {}

  // Approved 360 px teacher Home (real data only)
  private readonly teacherService = inject(TeacherService);
  private readonly liveSessions = inject(LiveSessionService);
  private readonly messaging = inject(MessagingService);
  isPhone = false;
  phoneLoaded = false;
  phoneNextClass: NextClass | null = null;
  phoneAwaiting: AwaitingItem[] = [];
  phoneAwaitingTotal = 0;
  phoneCourses: { id: number; title: string; status: string }[] = [];

  /** Getter, not a field: in the combined unit run the content module can initialise after this one. */
  get tCopy() {
    return TEACHER_HOME_MOBILE_COPY;
  }

  get phoneGreeting(): string {
    const hour = new Date().getHours();
    const salutation = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
    const name = firstName(this.authService.currentUser?.fullName);
    return name ? `${salutation}, ${name}` : salutation;
  }

  ngOnInit(): void {
    this.isPhone = typeof window !== 'undefined' && typeof window.matchMedia === 'function' && window.matchMedia(PHONE_QUERY).matches;
    if (!this.isPhone) return;
    this.liveSessions.getSessions().pipe(catchError(() => of([]))).subscribe((sessions) => {
      this.phoneNextClass = nextClass(sessions || []);
    });
    this.messaging.getConversations().pipe(catchError(() => of([]))).subscribe((conversations) => {
      const result = awaitingReply(conversations || []);
      this.phoneAwaiting = result.items;
      this.phoneAwaitingTotal = result.total;
    });
    this.teacherService.getCourses().pipe(catchError(() => of([] as Course[]))).subscribe((courses) => {
      this.phoneCourses = (courses || []).slice(0, 4).map((c) => ({ id: c.id, title: c.title, status: courseStatusLabel(c.status) }));
      this.phoneLoaded = true;
    });
  }

  get teacherName(): string {
    return this.authService.currentUser?.fullName || 'Teacher';
  }

  kpiCards = [
    { label: 'Total students', value: '486', sub: '+12 this week', subColor: '#0F6E56' },
    { label: 'Active courses', value: '5', sub: '2 draft / 1 pending', subColor: '#6B6880' },
    { label: 'Avg. rating', value: '4.7 ★', sub: 'Top 10% platform', subColor: '#0F6E56' },
    { label: 'Royalty this month', value: '₹18,400', sub: 'Payout on 30 Jun', subColor: '#534AB7' }
  ];

  myCourses = [
    { title: 'Engineering Mathematics III', stats: '312 students · ★★★★★ 4.9', status: 'Live', pillClass: 'p-gr', bg: '#EEEDFE', icon: 'pencil' },
    { title: 'Discrete Mathematics', stats: '174 students · ★★★★ 4.5', status: 'Live', pillClass: 'p-gr', bg: '#E6F1FB', icon: 'calculator' },
    { title: 'Statistics for Engineers', stats: '0 students · 8 lectures added', status: 'Draft', pillClass: 'p-am', bg: '#FAEEDA', icon: 'bar-chart-2' },
    { title: 'Numerical Methods', stats: 'Submitted · Admin review', status: 'Pending', pillClass: 'p-re', bg: '#FCEBEB', icon: 'book-open' }
  ];

  recentMessages = [
    { initials: 'RS', name: 'Raj Shah', text: 'Can you clarify Laplace lec 8?', time: '10m', bg: '#EEEDFE', color: '#534AB7' },
    { initials: 'PK', name: 'Priya Kulkarni', text: 'Thank you for the feedback!', time: '1h', bg: '#EAF3DE', color: '#27500A' },
    { initials: 'AM', name: 'Arjun Mishra', text: 'When is the next live session?', time: '1d', bg: '#FAEEDA', color: '#633806' }
  ];
}