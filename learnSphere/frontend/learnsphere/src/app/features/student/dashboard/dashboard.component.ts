import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { MatIconModule } from '@angular/material/icon';

import { StudentService } from '../services/student.service';
import { CertificateService } from '../services/certificate.service';
import { AssessmentService } from '../services/assessment.service';
import { Course } from '@core/models/course.model';

interface KpiCard {
  icon: string;
  value: string | number;
  label: string;
  trend?: 'up' | 'down';
  trendValue?: string;
}

interface CourseProgress {
  id: string | number;
  title: string;
  teacher: string;
  department: string;
  thumbnail: string;
  progress: number;
  lecturesWatched: number;
  totalLectures: number;
  lastWatched: string;
  color: string;
}

interface PendingAssessment {
  id: string | number;
  title: string;
  course: string;
  questions: number;
  timeLimit: number;
  dueDate: string;
  urgency: 'URGENT' | 'UPCOMING' | 'REMEDIAL';
}

interface Notification {
  id: string;
  icon: string;
  message: string;
  time: string;
  read: boolean;
}

interface RecommendedCourse {
  id: string | number;
  title: string;
  teacher: string;
  department: string;
  thumbnail: string;
  students: number;
  rating: number;
  tag: string;
}

@Component({
  selector: 'app-student-dashboard',
  standalone: true,
  imports: [
    CommonModule, 
    DatePipe,
    MatIconModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {

  get studentName(): string {
    return this.authService.currentUser?.fullName || 'Student';
  }
  currentDate = new Date();

  kpiCards: KpiCard[] = [
    { icon: 'menu_book', value: 0, label: 'Enrolled Courses', trend: 'up', trendValue: 'Active courses' },
    { icon: 'event_available', value: '100%', label: 'Completion Rate', trend: 'up', trendValue: 'Across courses' },
    { icon: 'emoji_events', value: '#1', label: 'Leaderboard Rank', trend: 'up', trendValue: 'Top quartile' },
    { icon: 'workspace_premium', value: 0, label: 'Certificates Earned', trend: 'up', trendValue: 'Verified' },
  ];

  continueLearning: CourseProgress[] = [];

  pendingAssessments: PendingAssessment[] = [];

  notifications: Notification[] = [
    { id: 'n1', icon: 'smart_display', message: 'Interactive transcript and live video sync available.', time: '1h ago', read: false },
    { id: 'n2', icon: 'trending_up', message: 'You completed your latest lecture module.', time: '2h ago', read: false },
  ];

  recommendedCourses: RecommendedCourse[] = [];

  constructor(
    private router: Router, 
    private authService: AuthService,
    private studentService: StudentService,
    private certificateService: CertificateService,
    private assessmentService: AssessmentService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  private loadDashboardData(): void {
    const colors = ['#534AB7', '#1D9E75', '#BA7517', '#3B82F6', '#EC4899'];

    this.studentService.getEnrolledCourses().subscribe({
      next: (courses: Course[]) => {
        this.kpiCards[0].value = courses.length;

        let totalProgress = 0;
        courses.forEach(c => totalProgress += (c.progressPercent || 0));
        const avgProgress = courses.length > 0 ? Math.round(totalProgress / courses.length) : 100;
        this.kpiCards[1].value = `${avgProgress}%`;

        this.continueLearning = courses.map((c, index) => ({
          id: c.id,
          title: c.title,
          teacher: c.teacherName ? `Prof. ${c.teacherName}` : 'Faculty Board',
          department: c.department || 'General',
          thumbnail: c.title ? c.title.substring(0, 2).toUpperCase() : 'LS',
          progress: c.progressPercent != null ? Math.round(c.progressPercent) : 0,
          lecturesWatched: c.completedLecturesCount || 0,
          totalLectures: c.lectureCount || 0,
          lastWatched: 'Recently Active',
          color: colors[index % colors.length]
        }));
      },
      error: () => {}
    });

    this.certificateService.getCertificates().subscribe({
      next: (certs) => {
        this.kpiCards[3].value = certs.length;
      },
      error: () => {}
    });

    this.studentService.getExploreCourses({ status: 'LIVE' }).subscribe({
      next: (courses: Course[]) => {
        this.recommendedCourses = courses.slice(0, 3).map(c => ({
          id: c.id,
          title: c.title,
          teacher: c.teacherName ? `Prof. ${c.teacherName}` : 'Faculty Board',
          department: c.department || 'Computer Science',
          thumbnail: c.title ? c.title.substring(0, 2).toUpperCase() : 'LS',
          students: 120 + (c.id * 17) % 200,
          rating: 4.8,
          tag: 'Popular'
        }));
      },
      error: () => {}
    });
  }

  getGreeting(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 17) return 'Good afternoon';
    return 'Good evening';
  }

  getUrgencyClass(urgency: string): string {
    const map: Record<string, string> = {
      URGENT: 'pill--red',
      UPCOMING: 'pill--amber',
      REMEDIAL: 'pill--purple'
    };
    return map[urgency] || 'pill--grey';
  }

  getUrgencyLabel(urgency: string): string {
    return urgency.charAt(0) + urgency.slice(1).toLowerCase();
  }

  navigateToCourse(courseId: string | number): void {
    this.router.navigate(['/student/courses', courseId]);
  }

  navigateToAssessment(assessmentId: string | number): void {
    this.router.navigate(['/student/assessments', assessmentId]);
  }

  navigateToAllCourses(): void {
    this.router.navigate(['/student/courses']);
  }

  navigateToAllAssessments(): void {
    this.router.navigate(['/student/assessments']);
  }
}