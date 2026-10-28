import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { LectureComponent } from './lecture.component';
import { StudentService } from '../../services/student.service';
import { AssignmentService } from '@core/services/assignment.service';
import { BookmarkService } from '../../services/bookmark.service';
import { AssessmentService } from '../../services/assessment.service';
import { CertificateService } from '../../services/certificate.service';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { LectureContextService } from '@core/services/lecture-context.service';

describe('LectureComponent Duration Polish (W3-12 / AUD-09)', () => {
  let component: LectureComponent;
  let fixture: ComponentFixture<LectureComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LectureComponent],
      providers: [
        provideRouter([]),
        {
          provide: StudentService,
          useValue: {
            getCourseLectures: vi.fn().mockReturnValue(of([])),
            getPublicCourse: vi.fn().mockReturnValue(of({})),
            getCourseProgress: vi.fn().mockReturnValue(of({ completedLectureIds: [], progressPercent: 0 })),
            getPlaybackDescriptor: vi.fn().mockReturnValue(of({}))
          }
        },
        { provide: AssignmentService, useValue: { getAssignmentsByModule: vi.fn().mockReturnValue(of([])) } },
        { provide: BookmarkService, useValue: { getBookmarks: vi.fn().mockReturnValue(of([])) } },
        { provide: AssessmentService, useValue: { getTestAttempts: vi.fn().mockReturnValue(of([])) } },
        { provide: CertificateService, useValue: { issueCertificate: vi.fn().mockReturnValue(of({})) } },
        { provide: AuthService, useValue: { getUser: vi.fn().mockReturnValue({ id: 1, name: 'Student' }) } },
        { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn() } },
        { provide: LectureContextService, useValue: { setContext: vi.fn(), reset: vi.fn() } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LectureComponent);
    component = fixture.componentInstance;
  });

  it('calculates 0m for empty lectures list', () => {
    component.lectures = [];
    expect(component.totalCourseDuration).toBe('0m');
  });

  it('calculates real total duration sum instead of fake 15-minute estimate (AUD-09)', () => {
    // 2 lectures: 3600s (60 min) + 1800s (30 min) = 5400s (90 min) -> 1h 30m
    // Fake formula was 2 * 15 = 30m
    component.lectures = [
      {
        id: 1,
        courseId: 10,
        title: 'Introduction',
        number: 1,
        videoUrl: 'http://example.com/1.mp4',
        duration: 3600,
        isDownloadable: false,
        status: 'PUBLISHED'
      },
      {
        id: 2,
        courseId: 10,
        title: 'Deep Dive',
        number: 2,
        videoUrl: 'http://example.com/2.mp4',
        duration: 1800,
        isDownloadable: false,
        status: 'PUBLISHED'
      }
    ];

    expect(component.totalCourseDuration).toBe('1h 30m');
  });

  it('formats minutes properly when total duration is under an hour', () => {
    component.lectures = [
      {
        id: 1,
        courseId: 10,
        title: 'Short Lesson',
        number: 1,
        videoUrl: 'http://example.com/1.mp4',
        duration: 600, // 10 minutes
        isDownloadable: false,
        status: 'PUBLISHED'
      }
    ];

    expect(component.totalCourseDuration).toBe('10m');
  });
});
