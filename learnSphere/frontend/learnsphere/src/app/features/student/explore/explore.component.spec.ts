import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { ExploreComponent } from './explore.component';
import { StudentService } from '../services/student.service';
import { NotificationService } from '@core/services/notification.service';
import { Course } from '@core/models/course.model';
import { LucideAngularModule, AlertTriangle, Search } from 'lucide-angular';

describe('ExploreComponent Standalone Course Catalogue (W3-10 / AUD-07)', () => {
  let component: ExploreComponent;
  let fixture: ComponentFixture<ExploreComponent>;
  let studentServiceSpy: {
    getExploreCourses: ReturnType<typeof vi.fn>;
    getEnrolledCourses: ReturnType<typeof vi.fn>;
    enrollInCourse: ReturnType<typeof vi.fn>;
  };
  let notificationServiceSpy: {
    success: ReturnType<typeof vi.fn>;
    error: ReturnType<typeof vi.fn>;
  };
  let router: Router;

  const mockExploreCourses: Course[] = [
    {
      id: 1,
      title: 'Distributed Systems Architecture',
      description: 'Consensus, replication, and fault tolerance',
      teacherId: 10,
      teacherName: 'Dr. Sarah Connor',
      department: 'Computer Science',
      thumbnail: '',
      status: 'LIVE',
      price: 0,
      lectureCount: 12,
    },
    {
      id: 2,
      title: 'Cloud Native Microservices',
      description: 'Production containerization and CI/CD',
      teacherId: 11,
      teacherName: 'Prof. Alan Turing',
      department: 'Informatics',
      thumbnail: '',
      status: 'LIVE',
      price: 1999,
      lectureCount: 20,
    }
  ];

  const mockEnrolledCourses: Course[] = [
    {
      id: 1,
      title: 'Distributed Systems Architecture',
      description: 'Consensus, replication, and fault tolerance',
      teacherId: 10,
      teacherName: 'Dr. Sarah Connor',
      department: 'Computer Science',
      thumbnail: '',
      status: 'LIVE',
      price: 0,
      progressPercent: 30,
      completed: false
    }
  ];

  beforeEach(async () => {
    studentServiceSpy = {
      getExploreCourses: vi.fn().mockReturnValue(of(mockExploreCourses)),
      getEnrolledCourses: vi.fn().mockReturnValue(of(mockEnrolledCourses)),
      enrollInCourse: vi.fn().mockReturnValue(of({}))
    };

    notificationServiceSpy = {
      success: vi.fn(),
      error: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [
        ExploreComponent,
        LucideAngularModule.pick({ AlertTriangle, Search })
      ],
      providers: [
        provideRouter([]),
        { provide: StudentService, useValue: studentServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(ExploreComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders catalogue shell with title, eyebrow and search input', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.eyebrow')?.textContent).toContain('Academic Course Catalogue');
    expect(compiled.querySelector('h1')?.textContent).toContain('Explore Courses');
    expect(compiled.querySelector('input[type="text"]')).toBeTruthy();
  });

  it('loads public catalogue and distinguishes enrolled courses with pill', () => {
    expect(studentServiceSpy.getExploreCourses).toHaveBeenCalled();
    expect(studentServiceSpy.getEnrolledCourses).toHaveBeenCalled();
    expect(component.courses.length).toBe(2);

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelectorAll('.explore-card').length).toBe(2);

    // Course 1 is enrolled
    expect(component.isEnrolled(1)).toBe(true);
    // Course 2 is not enrolled
    expect(component.isEnrolled(2)).toBe(false);

    expect(compiled.querySelector('.pill--enrolled')?.textContent).toContain('Enrolled');
  });

  it('filters courses by pricing tab', () => {
    component.setFilter('FREE');
    expect(component.filteredCourses.length).toBe(1);
    expect(component.filteredCourses[0].id).toBe(1);

    component.setFilter('PAID');
    expect(component.filteredCourses.length).toBe(1);
    expect(component.filteredCourses[0].id).toBe(2);

    component.setFilter('ALL');
    expect(component.filteredCourses.length).toBe(2);
  });

  it('navigates to course on already enrolled course action', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    const event = new MouseEvent('click');

    component.onAction(mockExploreCourses[0], event);
    expect(navigateSpy).toHaveBeenCalledWith(['/student/courses', 1]);
  });

  it('enrolls student in free course and updates enrolled set', () => {
    const event = new MouseEvent('click');
    const freeUnenrolled: Course = { ...mockExploreCourses[0], id: 99 };

    component.onAction(freeUnenrolled, event);
    expect(studentServiceSpy.enrollInCourse).toHaveBeenCalledWith(99);
    expect(notificationServiceSpy.success).toHaveBeenCalledWith('Enrolled! You now have full access.');
    expect(component.isEnrolled(99)).toBe(true);
  });
});
