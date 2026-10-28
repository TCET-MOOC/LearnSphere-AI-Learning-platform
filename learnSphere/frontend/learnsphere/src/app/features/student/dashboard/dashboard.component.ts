import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../../core/auth/auth.service';
import { LucideAngularModule } from 'lucide-angular';

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
  completed?: boolean;
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
    LucideAngularModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {

  get studentName(): string {
    return this.authService.currentUser?.fullName || 'Student';
  }
  currentDate = new Date();

  loading = true;
  errorMessage: string | null = null;

  kpiCards: KpiCard[] = [
    { icon: 'book-open', value: 0, label: 'Enrolled Courses', trend: 'up', trendValue: 'Active courses' },
    { icon: 'check-circle', value: '100%', label: 'Completion Rate', trend: 'up', trendValue: 'Across courses' },
    { icon: 'trophy', value: '#1', label: 'Leaderboard Rank', trend: 'up', trendValue: 'Top quartile' },
    { icon: 'award', value: 0, label: 'Certificates Earned', trend: 'up', trendValue: 'Verified' },
  ];

  continueLearning: CourseProgress[] = [];

  pendingAssessments: PendingAssessment[] = [];

  notifications: Notification[] = [
    { id: 'n1', icon: 'play-circle', message: 'Interactive transcript and live video sync available.', time: '1h ago', read: false },
    { id: 'n2', icon: 'trending-up', message: 'You completed your latest lecture module.', time: '2h ago', read: false },
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

  loadDashboardData(): void {
    this.loading = true;
    this.errorMessage = null;

    forkJoin({
      enrolled: this.studentService.getEnrolledCourses().pipe(
        catchError(() => {
          this.errorMessage = 'Unable to load your courses. Please check connection and try again.';
          return of([]);
        })
      ),
      certs: this.certificateService.getCertificates().pipe(
        catchError(() => of([]))
      ),
      explore: this.studentService.getExploreCourses({ status: 'LIVE' }).pipe(
        catchError(() => of([]))
      ),
      assessments: this.assessmentService.getAssessments().pipe(
        catchError(() => of([]))
      )
    }).subscribe({
      next: ({ enrolled, certs, explore, assessments }) => {
        this.loading = false;

        // KPI cards
        this.kpiCards[0].value = enrolled.length;

        let totalProgress = 0;
        enrolled.forEach(c => totalProgress += (c.progressPercent || 0));
        const avgProgress = enrolled.length > 0 ? Math.round(totalProgress / enrolled.length) : 100;
        this.kpiCards[1].value = `${avgProgress}%`;

        this.kpiCards[3].value = certs.length;

        // Continue Learning
        this.continueLearning = enrolled.map(c => ({
          id: c.id,
          title: c.title,
          teacher: c.teacherName ? `Prof. ${c.teacherName}` : 'Faculty Board',
          department: c.department || 'General',
          thumbnail: c.title ? c.title.substring(0, 2).toUpperCase() : 'LS',
          progress: c.progressPercent != null ? Math.round(c.progressPercent) : 0,
          lecturesWatched: c.completedLecturesCount || 0,
          totalLectures: c.lectureCount || 0,
          lastWatched: 'Recently Active',
          completed: !!(c.completed || (c.progressPercent != null && c.progressPercent >= 100))
        }));

        // Pending Assessments
        if (assessments && assessments.length > 0) {
          this.pendingAssessments = assessments.slice(0, 3).map((a, idx) => ({
            id: a.id,
            title: a.title,
            course: a.courseTitle || 'Curriculum Assessment',
            questions: a.questionCount || 10,
            timeLimit: a.durationMinutes || 30,
            dueDate: 'Scheduled',
            urgency: idx === 0 ? 'URGENT' : 'UPCOMING'
          }));
        } else {
          this.pendingAssessments = [];
        }

        // Recommended Courses
        this.recommendedCourses = (explore || []).slice(0, 3).map(c => ({
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
      error: () => {
        this.loading = false;
        this.errorMessage = 'Unable to load dashboard data. Please check connection and try again.';
      }
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