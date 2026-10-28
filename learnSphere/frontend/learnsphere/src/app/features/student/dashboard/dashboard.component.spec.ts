import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError, NEVER } from 'rxjs';
import { DashboardComponent } from './dashboard.component';
import { AuthService } from '../../../core/auth/auth.service';
import { StudentService } from '../services/student.service';
import { CertificateService } from '../services/certificate.service';
import { AssessmentService } from '../services/assessment.service';
import {
  LucideAngularModule,
  Hand,
  Search,
  BookOpen,
  CheckCircle,
  Trophy,
  Award,
  PlayCircle,
  TrendingUp,
  Star,
  Users,
  FileText,
  Clock,
  Calendar,
  AlertTriangle
} from 'lucide-angular';
import { Course } from '@core/models/course.model';

describe('DashboardComponent (DS-10 / Student Surfaces)', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let authServiceSpy: { currentUser: { fullName: string; email: string; id: number } | null };
  let studentServiceSpy: {
    getEnrolledCourses: ReturnType<typeof vi.fn>;
    getExploreCourses: ReturnType<typeof vi.fn>;
  };
  let certificateServiceSpy: {
    getCertificates: ReturnType<typeof vi.fn>;
  };
  let assessmentServiceSpy: {
    getAssessments: ReturnType<typeof vi.fn>;
  };
  let router: Router;

  const mockEnrolledCourses: Course[] = [
    {
      id: 101,
      title: 'Distributed Systems Architecture',
      description: 'Consensus protocols and fault tolerance',
      department: 'Computer Science',
      teacherId: 1,
      teacherName: 'Leslie Lamport',
      thumbnail: '',
      price: 0,
      status: 'LIVE',
      progressPercent: 65,
      completed: false,
      lectureCount: 12,
      completedLecturesCount: 8
    },
    {
      id: 102,
      title: 'Database Engineering',
      description: 'Storage engines and transactional isolation',
      department: 'Information Technology',
      teacherId: 2,
      teacherName: 'Michael Stonebraker',
      thumbnail: '',
      price: 0,
      status: 'LIVE',
      progressPercent: 100,
      completed: true,
      lectureCount: 10,
      completedLecturesCount: 10
    }
  ];

  beforeEach(async () => {
    authServiceSpy = {
      currentUser: { fullName: 'Alex Rivera', email: 'alex@example.com', id: 42 }
    };

    studentServiceSpy = {
      getEnrolledCourses: vi.fn().mockReturnValue(of(mockEnrolledCourses)),
      getExploreCourses: vi.fn().mockReturnValue(of([
        {
          id: 201,
          title: 'Deep Learning & Neural Networks',
          teacherName: 'Yann LeCun',
          department: 'Artificial Intelligence',
          price: 0,
          status: 'LIVE'
        }
      ]))
    };

    certificateServiceSpy = {
      getCertificates: vi.fn().mockReturnValue(of([
        { id: 1, courseTitle: 'Database Engineering', code: 'CERT-101' }
      ]))
    };

    assessmentServiceSpy = {
      getAssessments: vi.fn().mockReturnValue(of([
        {
          id: 501,
          title: 'Distributed Systems Midterm',
          courseTitle: 'Distributed Systems Architecture',
          questionCount: 15,
          durationMinutes: 45
        }
      ]))
    };

    await TestBed.configureTestingModule({
      imports: [
        DashboardComponent,
        LucideAngularModule.pick({
          Hand,
          Search,
          BookOpen,
          CheckCircle,
          Trophy,
          Award,
          PlayCircle,
          TrendingUp,
          Star,
          Users,
          FileText,
          Clock,
          Calendar,
          AlertTriangle
        })
      ],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: StudentService, useValue: studentServiceSpy },
        { provide: CertificateService, useValue: certificateServiceSpy },
        { provide: AssessmentService, useValue: assessmentServiceSpy }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
  });

  it('renders student dashboard shell with greeting, user name, and explore button', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.greeting__name')?.textContent).toContain('Alex Rivera');
    expect(compiled.querySelector('.btn-explore-inline')?.textContent).toContain('Explore Courses');
  });

  it('displays loading state while dashboard data is resolving', () => {
    studentServiceSpy.getEnrolledCourses.mockReturnValue(NEVER);
    component.loadDashboardData();
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.dashboard-state--loading')).toBeTruthy();
    expect(compiled.querySelector('.loading-spinner')).toBeTruthy();
  });

  it('displays error state with retry button when loading fails', () => {
    studentServiceSpy.getEnrolledCourses.mockReturnValue(throwError(() => new Error('Network error')));
    component.loadDashboardData();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.dashboard-state--error')).toBeTruthy();
    const retryBtn = compiled.querySelector('.dashboard-state--error button') as HTMLButtonElement;
    expect(retryBtn).toBeTruthy();
    expect(retryBtn.textContent).toContain('Retry');

    // Clicking retry triggers loadDashboardData
    const reloadSpy = vi.spyOn(component, 'loadDashboardData');
    retryBtn.click();
    expect(reloadSpy).toHaveBeenCalled();
  });

  it('displays empty learning state when student has no enrolled courses', () => {
    studentServiceSpy.getEnrolledCourses.mockReturnValue(of([]));
    component.loadDashboardData();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.empty-state')).toBeTruthy();
    expect(compiled.querySelector('.empty-state__title')?.textContent).toContain('No courses in progress');
    expect(compiled.querySelector('.empty-state button')?.textContent).toContain('Explore Catalogue');
  });

  it('renders populated KPI cards, course cards, and assessments with card hierarchy', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    // KPI cards
    const kpiCards = compiled.querySelectorAll('.kpi-card');
    expect(kpiCards.length).toBe(4);
    expect(kpiCards[0].querySelector('.kpi-card__value')?.textContent?.trim()).toBe('2');

    // Continue learning course items
    const courseItems = compiled.querySelectorAll('.course-item');
    expect(courseItems.length).toBe(2);
    expect(courseItems[0].querySelector('.course-item__title')?.textContent).toContain('Distributed Systems Architecture');
    expect(courseItems[0].querySelector('.course-item__pct')?.textContent).toContain('65%');

    // Pending assessments
    const assessmentItems = compiled.querySelectorAll('.assessment-item');
    expect(assessmentItems.length).toBe(1);
    expect(assessmentItems[0].querySelector('.assessment-item__title')?.textContent).toContain('Distributed Systems Midterm');
  });

  it('navigates when action buttons are clicked', () => {
    fixture.detectChanges();
    component.navigateToCourse(101);
    expect(router.navigate).toHaveBeenCalledWith(['/student/courses', 101]);

    component.navigateToAllCourses();
    expect(router.navigate).toHaveBeenCalledWith(['/student/courses']);

    component.navigateToAssessment(501);
    expect(router.navigate).toHaveBeenCalledWith(['/student/assessments', 501]);
  });
});
