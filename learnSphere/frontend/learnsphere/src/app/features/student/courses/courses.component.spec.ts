import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { CoursesComponent } from './courses.component';
import { StudentService } from '../services/student.service';
import { Course } from '@core/models/course.model';

describe('CoursesComponent Polish & Enrolled Filters (W3-12 / AUD-09)', () => {
  let component: CoursesComponent;
  let fixture: ComponentFixture<CoursesComponent>;
  let studentServiceSpy: {
    getEnrolledCourses: ReturnType<typeof vi.fn>;
    getResumeTarget: ReturnType<typeof vi.fn>;
  };
  let router: Router;

  const mockEnrolledCourses: Course[] = [
    {
      id: 101,
      title: 'Database Management Systems',
      description: 'Relational algebra and query planning',
      department: 'Computer Science',
      teacherId: 1,
      teacherName: 'Prof. Ada Lovelace',
      thumbnail: '',
      price: 0,
      status: 'LIVE',
      progressPercent: 45,
      completed: false,
      lectureCount: 10
    },
    {
      id: 102,
      title: 'Advanced Computer Networks',
      description: 'Routing protocols and network topology',
      department: 'Computer Science',
      teacherId: 2,
      teacherName: 'Dr. Radia Perlman',
      thumbnail: '',
      price: 0,
      status: 'LIVE',
      progressPercent: 100,
      completed: true,
      lectureCount: 8
    },
    {
      id: 103,
      title: 'Operating Systems Internals',
      description: 'Kernel scheduling and virtual memory',
      department: 'Information Technology',
      teacherId: 3,
      teacherName: 'Prof. Andrew Tanenbaum',
      thumbnail: '',
      price: 0,
      status: 'LIVE',
      progressPercent: 0,
      completed: false,
      lectureCount: 15
    }
  ];

  beforeEach(async () => {
    studentServiceSpy = {
      getEnrolledCourses: vi.fn().mockReturnValue(of(mockEnrolledCourses)),
      getResumeTarget: vi.fn().mockReturnValue(of({ courseId: 101, lectureId: 2, contentId: 20 }))
    };

    await TestBed.configureTestingModule({
      imports: [CoursesComponent],
      providers: [
        provideRouter([]),
        { provide: StudentService, useValue: studentServiceSpy }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(CoursesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders student enrolled courses shell and loads enrolled courses', () => {
    expect(component).toBeTruthy();
    expect(studentServiceSpy.getEnrolledCourses).toHaveBeenCalledTimes(1);
    expect(component.enrolledCourses.length).toBe(3);
    expect(component.filteredCourses.length).toBe(3);
  });

  it('provides student-relevant filter tabs: All Courses, In Progress, and Completed without LIVE/ARCHIVED (AUD-09)', () => {
    const filterKeys = component.filters.map(f => f.key);
    expect(filterKeys).toEqual(['ALL', 'IN_PROGRESS', 'COMPLETED']);
    expect(filterKeys).not.toContain('LIVE');
    expect(filterKeys).not.toContain('ARCHIVED');
  });

  it('filters by IN_PROGRESS correctly', () => {
    component.setFilter('IN_PROGRESS');
    fixture.detectChanges();

    const inProgress = component.filteredCourses;
    expect(inProgress.length).toBe(2);
    expect(inProgress.map(c => c.id)).toEqual([101, 103]);
    expect(inProgress.every(c => !c.completed && (c.progressPercent == null || c.progressPercent < 100))).toBe(true);
  });

  it('filters by COMPLETED correctly', () => {
    component.setFilter('COMPLETED');
    fixture.detectChanges();

    const completed = component.filteredCourses;
    expect(completed.length).toBe(1);
    expect(completed[0].id).toBe(102);
    expect(completed[0].completed).toBe(true);
  });

  it('filters courses by search query matching title or teacher', () => {
    component.searchQuery = 'Tanenbaum';
    fixture.detectChanges();

    const results = component.filteredCourses;
    expect(results.length).toBe(1);
    expect(results[0].id).toBe(103);
  });

  it('renders "In Progress" badge rather than raw "LIVE" status on active course cards', () => {
    const cardBadges = fixture.nativeElement.querySelectorAll('.badge-group .pill');
    const badgeTexts = Array.from(cardBadges).map((el: any) => el.textContent.trim());
    expect(badgeTexts).toContain('In Progress');
    expect(badgeTexts).toContain('Completed');
    expect(badgeTexts).not.toContain('LIVE');
  });
});
