import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { StudentService } from './student.service';
import { ApiService } from '@core/services/api.service';
import { Course, CourseStatus } from '@core/models/course.model';

describe('StudentService (W3-09 Learning regression suite)', () => {
  let service: StudentService;
  let apiServiceSpy: { get: ReturnType<typeof vi.fn>; post: ReturnType<typeof vi.fn>; put: ReturnType<typeof vi.fn> };

  const mockCourses: Course[] = [
    {
      id: 101,
      title: 'Cloud Native Microservices',
      description: 'Distributed architectures',
      teacherId: 1,
      thumbnail: 'https://example.com/thumb.jpg',
      price: 0,
      status: 'LIVE' as CourseStatus,
      department: 'Computer Science',
      createdAt: '2026-01-01T00:00:00Z',
    },
  ];

  beforeEach(() => {
    apiServiceSpy = {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        StudentService,
        { provide: ApiService, useValue: apiServiceSpy },
      ],
    });

    service = TestBed.inject(StudentService);
  });

  it('should fetch enrolled courses from /student/courses', () => {
    apiServiceSpy.get.mockReturnValue(of(mockCourses));

    service.getEnrolledCourses().subscribe((courses) => {
      expect(courses).toEqual(mockCourses);
    });

    expect(apiServiceSpy.get).toHaveBeenCalledWith('/student/courses');
  });

  it('should fetch enrolled course detail from /student/courses/:id', () => {
    apiServiceSpy.get.mockReturnValue(of(mockCourses[0]));

    service.getCourseDetail(101).subscribe((course) => {
      expect(course.title).toBe('Cloud Native Microservices');
    });

    expect(apiServiceSpy.get).toHaveBeenCalledWith('/student/courses/101');
  });

  it('should fetch resume target from /student/courses/:id/resume', () => {
    const mockResume = {
      courseId: 101,
      contentId: 501,
      lectureId: 201,
      title: '1.2 Logical Clocks',
      contentType: 'VIDEO',
      position: 2,
      progressPercent: 45.0,
      secondsWatched: 540,
      completed: false,
    };
    apiServiceSpy.get.mockReturnValue(of(mockResume));

    service.getResumeTarget(101).subscribe((target) => {
      expect(target.contentId).toBe(501);
      expect(target.title).toBe('1.2 Logical Clocks');
    });

    expect(apiServiceSpy.get).toHaveBeenCalledWith('/student/courses/101/resume');
  });

  it('should fetch public course catalogue from /courses with query parameters', () => {
    apiServiceSpy.get.mockReturnValue(of(mockCourses));

    service.getExploreCourses({ status: 'LIVE', department: 'Computer Science', search: 'Cloud' }).subscribe((courses) => {
      expect(courses).toEqual(mockCourses);
    });

    expect(apiServiceSpy.get).toHaveBeenCalledWith('/courses', {
      params: {
        status: 'LIVE',
        department: 'Computer Science',
        search: 'Cloud',
      },
    });
  });

  it('should fetch course modules from /courses/:id/modules', () => {
    const mockModules = [{ id: 1, title: 'Module 1', position: 1 }];
    apiServiceSpy.get.mockReturnValue(of(mockModules));

    service.getCourseModules(101).subscribe((modules) => {
      expect(modules).toEqual(mockModules);
    });

    expect(apiServiceSpy.get).toHaveBeenCalledWith('/courses/101/modules');
  });

  it('should mark lecture complete via /student/lectures/:id/complete', () => {
    const mockProgress = { lectureId: 5, completed: true, progressPercent: 100.0 };
    apiServiceSpy.post.mockReturnValue(of(mockProgress));

    service.markLectureComplete(5).subscribe((res) => {
      expect(res).toEqual(mockProgress);
    });

    expect(apiServiceSpy.post).toHaveBeenCalledWith('/student/lectures/5/complete', {});
  });

  it('should unmark lecture complete via /student/lectures/:id/uncomplete', () => {
    const mockProgress = { lectureId: 5, completed: false, progressPercent: 0.0 };
    apiServiceSpy.post.mockReturnValue(of(mockProgress));

    service.unmarkLectureComplete(5).subscribe((res) => {
      expect(res).toEqual(mockProgress);
    });

    expect(apiServiceSpy.post).toHaveBeenCalledWith('/student/lectures/5/uncomplete', {});
  });

  it('should fetch course progress from /student/courses/:id/progress', () => {
    const mockProgressList = [{ lectureId: 5, completed: true, progressPercent: 100.0 }];
    apiServiceSpy.get.mockReturnValue(of(mockProgressList));

    service.getCourseProgress(101).subscribe((res) => {
      expect(res).toEqual(mockProgressList);
    });

    expect(apiServiceSpy.get).toHaveBeenCalledWith('/student/courses/101/progress');
  });

  it('should fetch individual lecture metadata from /student/courses/:courseId/lectures/:lectureId', () => {
    const mockLecture = { id: 5, courseId: 101, contentId: 205, title: 'Consensus', number: 1, videoUrl: '', duration: 120, isDownloadable: true, status: 'PUBLISHED' as const };
    apiServiceSpy.get.mockReturnValue(of(mockLecture));

    service.getLecture(101, 5).subscribe((res) => {
      expect(res).toEqual(mockLecture);
    });

    expect(apiServiceSpy.get).toHaveBeenCalledWith('/student/courses/101/lectures/5');
  });

  it('should fetch playback descriptor from /contents/:contentId/playback', () => {
    const mockDescriptor = {
      contentId: 205,
      title: 'Consensus',
      contentType: 'VIDEO',
      sourceType: 'UPLOAD' as const,
      streamUrl: '/api/contents/205/stream?exp=123&u=1&sig=abc'
    };
    apiServiceSpy.get.mockReturnValue(of(mockDescriptor));

    service.getPlaybackDescriptor(205).subscribe((res) => {
      expect(res).toEqual(mockDescriptor);
    });

    expect(apiServiceSpy.get).toHaveBeenCalledWith('/contents/205/playback');
  });
});
