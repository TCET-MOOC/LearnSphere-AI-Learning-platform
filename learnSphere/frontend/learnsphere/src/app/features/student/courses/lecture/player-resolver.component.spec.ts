import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { PlayerResolverComponent } from './player-resolver.component';
import { StudentService } from '../../services/student.service';

describe('PlayerResolverComponent (W3-11 / AUD-08)', () => {
  let component: PlayerResolverComponent;
  let fixture: ComponentFixture<PlayerResolverComponent>;
  let mockRouter: { navigate: ReturnType<typeof vi.fn> };
  let mockStudentService: {
    getResumeTarget: ReturnType<typeof vi.fn>;
    getCourseLectures: ReturnType<typeof vi.fn>;
  };

  function createTestBed(paramMap: Record<string, string>) {
    mockRouter = {
      navigate: vi.fn()
    };

    mockStudentService = {
      getResumeTarget: vi.fn(),
      getCourseLectures: vi.fn()
    };

    const mockActivatedRoute = {
      snapshot: {
        paramMap: {
          get: (key: string) => paramMap[key] || null
        }
      }
    };

    TestBed.configureTestingModule({
      imports: [PlayerResolverComponent],
      providers: [
        { provide: Router, useValue: mockRouter },
        { provide: StudentService, useValue: mockStudentService },
        { provide: ActivatedRoute, useValue: mockActivatedRoute }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PlayerResolverComponent);
    component = fixture.componentInstance;
  }

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  it('navigates to /student/courses if courseId param is missing or invalid', () => {
    createTestBed({});
    component.ngOnInit();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/student/courses'], { replaceUrl: true });
  });

  it('navigates to the incomplete lecture resolved by resume target', () => {
    createTestBed({ courseId: '42' });
    mockStudentService.getResumeTarget.mockReturnValue(of({
      courseId: 42,
      contentId: 101,
      lectureId: 7,
      title: 'Distributed Consensus',
      completed: false
    }));

    component.ngOnInit();

    expect(mockStudentService.getResumeTarget).toHaveBeenCalledWith(42);
    expect(mockRouter.navigate).toHaveBeenCalledWith(
      ['/student/courses', 42, 'lectures', 7],
      { replaceUrl: true }
    );
  });

  it('falls back to the first lecture when resume target is not available or course is completed', () => {
    createTestBed({ courseId: '42' });
    mockStudentService.getResumeTarget.mockReturnValue(throwError(() => new Error('Course completed')));
    mockStudentService.getCourseLectures.mockReturnValue(of([
      { id: 1, courseId: 42, title: 'Introduction', number: 1, videoUrl: '', duration: 100, isDownloadable: true, status: 'PUBLISHED' }
    ]));

    component.ngOnInit();

    expect(mockStudentService.getCourseLectures).toHaveBeenCalledWith(42);
    expect(mockRouter.navigate).toHaveBeenCalledWith(
      ['/student/courses', 42, 'lectures', 1],
      { replaceUrl: true }
    );
  });

  it('navigates to /student/courses/:courseId when course has no lectures or user is unenrolled', () => {
    createTestBed({ courseId: '42' });
    mockStudentService.getResumeTarget.mockReturnValue(throwError(() => new Error('404 Not enrolled')));
    mockStudentService.getCourseLectures.mockReturnValue(throwError(() => new Error('404 Not enrolled')));

    component.ngOnInit();

    expect(mockRouter.navigate).toHaveBeenCalledWith(
      ['/student/courses', 42],
      { replaceUrl: true }
    );
  });
});
