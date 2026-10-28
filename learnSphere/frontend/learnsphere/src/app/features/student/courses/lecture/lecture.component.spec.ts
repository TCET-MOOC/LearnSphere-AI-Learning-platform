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
import {
  LucideAngularModule,
  Video,
  BookOpen,
  GraduationCap,
  Award,
  HelpCircle,
  Bookmark,
  CheckCircle,
  Check,
  MessageSquare,
  FileText,
  Layers,
  Download,
  Code,
  Clock,
  AlertTriangle,
  RefreshCw,
  ExternalLink,
  X,
  Sparkles,
  Globe,
  MoreVertical,
  Subtitles,
  Search
} from 'lucide-angular';

describe('LectureComponent (DS-11 / AUD-09)', () => {
  let component: LectureComponent;
  let fixture: ComponentFixture<LectureComponent>;

  const mockLecture: any = {
    id: 1,
    courseId: 10,
    title: 'Foundations of Deep Learning',
    number: 1,
    videoUrl: 'http://example.com/1.mp4',
    duration: 3600,
    isDownloadable: false,
    status: 'PUBLISHED'
  };

  const mockCourse = {
    id: 10,
    title: 'Deep Learning Specialization',
    department: 'Computer Engineering',
    teacherName: 'Dr. Jane Smith'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        LectureComponent,
        LucideAngularModule.pick({
          Video,
          BookOpen,
          GraduationCap,
          Award,
          HelpCircle,
          Bookmark,
          CheckCircle,
          Check,
          MessageSquare,
          FileText,
          Layers,
          Download,
          Code,
          Clock,
          AlertTriangle,
          RefreshCw,
          ExternalLink,
          X,
          Sparkles,
          Globe,
          MoreVertical,
          Subtitles,
          Search
        })
      ],
      providers: [
        provideRouter([
          { path: 'student/courses', component: class DummyComponent {} },
          { path: 'student/courses/:courseId/player', component: class DummyComponent {} },
          { path: 'student/courses/:courseId/lectures/:lectureId', component: class DummyComponent {} }
        ]),
        {
          provide: StudentService,
          useValue: {
            getCourseLectures: vi.fn().mockReturnValue(of([mockLecture])),
            getPublicCourse: vi.fn().mockReturnValue(of(mockCourse)),
            getCourseProgress: vi.fn().mockReturnValue(of({ completedLectureIds: [], progressPercent: 0 })),
            getPlaybackDescriptor: vi.fn().mockReturnValue(of({ playbackUrl: 'http://example.com/1.mp4' })),
            getNotes: vi.fn().mockReturnValue(of([]))
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

  describe('Duration Formatting (W3-12 / AUD-09)', () => {
    it('calculates 0m for empty lectures list', () => {
      component.lectures = [];
      expect(component.totalCourseDuration).toBe('0m');
    });

    it('calculates real total duration sum instead of fake 15-minute estimate (AUD-09)', () => {
      component.lectures = [
        mockLecture,
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
      ] as any[];

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
          duration: 600,
          isDownloadable: false,
          status: 'PUBLISHED'
        }
      ] as any[];

      expect(component.totalCourseDuration).toBe('10m');
    });
  });

  describe('Player Workspace Typography & Density (DS-11 / W3-15)', () => {
    it('renders 2-column asymmetric workspace grid when current lecture is active', () => {
      component.loading = false;
      component.currentLecture = mockLecture;
      component.course = mockCourse as any;
      component.lectures = [mockLecture];
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.workspace-grid')).toBeTruthy();
      expect(compiled.querySelector('.canvas-column')).toBeTruthy();
      expect(compiled.querySelector('.sidebar-column')).toBeTruthy();
    });

    it('renders metadata header with lecture title, faculty details, and quieted action buttons', () => {
      component.loading = false;
      component.currentLecture = mockLecture;
      component.course = mockCourse as any;
      component.lectures = [mockLecture];
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const title = compiled.querySelector('.lecture-title');
      expect(title?.textContent?.trim()).toBe('Foundations of Deep Learning');

      const faculty = compiled.querySelector('.faculty-name');
      expect(faculty?.textContent?.trim()).toBe('Dr. Jane Smith');

      const actions = compiled.querySelectorAll('.action-btn');
      expect(actions.length).toBeGreaterThanOrEqual(2);
    });

    it('switches bottom tabs between discussion, notes, assignments, and resources', () => {
      component.loading = false;
      component.currentLecture = mockLecture;
      component.course = mockCourse as any;
      component.lectures = [mockLecture];
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(component.activeBottomTab).toBe('discussion');

      const tabBtns = compiled.querySelectorAll<HTMLButtonElement>('.bottom-tabbed-card .tab-btn');
      expect(tabBtns.length).toBe(4);

      tabBtns[1].click();
      fixture.detectChanges();
      expect(component.activeBottomTab).toBe('notes');
      expect(compiled.querySelector('.notes-tab-pane')).toBeTruthy();

      tabBtns[2].click();
      fixture.detectChanges();
      expect(component.activeBottomTab).toBe('assignments');
      expect(compiled.querySelector('.assignments-tab-pane')).toBeTruthy();

      tabBtns[3].click();
      fixture.detectChanges();
      expect(component.activeBottomTab).toBe('resources');
      expect(compiled.querySelector('.resources-tab-pane')).toBeTruthy();
    });

    it('switches sidebar between playlist and interactive transcript tabs', () => {
      component.loading = false;
      component.currentLecture = mockLecture;
      component.course = mockCourse as any;
      component.lectures = [mockLecture];
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(component.activeSidebarTab).toBe('playlist');

      const switcherTabs = compiled.querySelectorAll<HTMLButtonElement>('.sidebar-switcher .switcher-tab');
      expect(switcherTabs.length).toBe(2);

      switcherTabs[1].click();
      fixture.detectChanges();
      expect(component.activeSidebarTab).toBe('transcript');
      expect(compiled.querySelector('.transcript-slot')).toBeTruthy();
      expect(compiled.querySelector('.playlist-card.is-split')).toBeTruthy();
    });

    it('renders empty workspace card when no lecture is selected', () => {
      component.loading = false;
      component.currentLecture = null;
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.empty-workspace-card')).toBeTruthy();
      expect(compiled.querySelector('.workspace-grid')).toBeNull();
    });
  });
});
