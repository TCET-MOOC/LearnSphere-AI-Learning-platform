import { Component, OnInit, ChangeDetectorRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { StudentService } from '../../services/student.service';
import { BookmarkService } from '../../services/bookmark.service';
import { Course, Lecture, LectureProgress } from '@core/models/course.model';
import { VideoPlayerComponent, VideoTimeUpdate } from '@shared/components/video-player/video-player.component';
import { LectureGridComponent } from './components/lecture-grid.component';
import { NotesDrawerComponent } from './components/notes-drawer.component';
import { LectureDiscussionComponent } from './components/lecture-discussion.component';
import { InteractiveTranscriptComponent } from './components/interactive-transcript.component';
import { PostVideoQuizComponent } from './components/post-video-quiz.component';
import { CertificateModalComponent, CertificateModalData } from '@shared/components/certificate-modal/certificate-modal.component';
import { CertificateService } from '../../services/certificate.service';
import { AuthService } from '@core/auth/auth.service';
import { AssessmentService } from '../../services/assessment.service';
import { NotificationService } from '@core/services/notification.service';
import { 
  LucideAngularModule, 
  BookOpen, 
  FileText, 
  MessageSquare, 
  Bookmark, 
  CheckCircle, 
  Clock, 
  Video, 
  Share2, 
  Layers,
  HelpCircle,
  Award
} from 'lucide-angular';

@Component({
  selector: 'app-student-lecture',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule, 
    LucideAngularModule, 
    VideoPlayerComponent, 
    NotesDrawerComponent, 
    LectureDiscussionComponent,
    InteractiveTranscriptComponent,
    PostVideoQuizComponent,
    CertificateModalComponent
  ],
  templateUrl: './lecture.component.html',
  styleUrls: ['./lecture.component.scss']
})
export class LectureComponent implements OnInit {
  @ViewChild(VideoPlayerComponent) videoPlayer?: VideoPlayerComponent;

  loading = true;
  courseId: number | null = null;
  course: Course | null = null;
  lectures: Lecture[] = [];

  currentLecture: Lecture | null = null;
  currentProgress: LectureProgress | null = null;
  loadingLecture = false;

  completedIds = new Set<number>();
  private autoCompletedForLecture: number | null = null;
  playbackPosition = 0;
  videoDuration = 0;
  isBookmarked = false;

  // Post-Video Quiz State
  hasLectureQuiz = false;
  showPostQuizModal = false;

  // Certificate Modal State
  showCertModal = false;
  certModalData: CertificateModalData | null = null;
  hasEarnedCertificate = false;

  // Workspace Dynamic Tabs
  activeSidebarTab: 'playlist' | 'transcript' = 'playlist';
  activeBottomTab: 'discussion' | 'notes' | 'resources' = 'discussion';
  selectedTranscriptLanguage = 'en';

  onLanguageChange(lang: string): void {
    this.selectedTranscriptLanguage = lang;
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private studentService: StudentService,
    private bookmarkService: BookmarkService,
    private assessmentService: AssessmentService,
    private certificateService: CertificateService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const courseIdParam = params.get('courseId');
      const lectureIdParam = params.get('lectureId');
      if (courseIdParam && lectureIdParam) {
        this.loadCourseContext(Number(courseIdParam), Number(lectureIdParam));
      } else {
        this.loadDefaultContext();
      }
    });
  }

  private loadDefaultContext(): void {
    this.loading = true;
    this.studentService.getEnrolledCourses().subscribe({
      next: (courses) => {
        const candidate = courses.find(c => (c.lectureCount || 0) > 0) || courses[0];
        if (!candidate) {
          this.loading = false;
          this.cdr.markForCheck();
          return;
        }
        this.studentService.getCourseLectures(candidate.id).subscribe({
          next: (lectures) => {
            if (lectures.length === 0) {
              this.courseId = candidate.id;
              this.course = candidate;
              this.lectures = [];
              this.loading = false;
              this.cdr.markForCheck();
              return;
            }
            this.loadCourseContext(candidate.id, lectures[0].id);
          },
          error: () => {
            this.loading = false;
            this.cdr.markForCheck();
          }
        });
      },
      error: () => {
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private loadCourseContext(courseId: number, lectureId: number): void {
    this.loading = true;
    this.courseId = courseId;
    this.studentService.getPublicCourse(courseId).subscribe({
      next: (course) => {
        this.course = course;
        this.cdr.markForCheck();
      },
      error: () => {
        this.course = null;
        this.cdr.markForCheck();
      }
    });

    // Load full playlist progress so all completed checkmarks persist on page load/refresh
    this.studentService.getCourseProgress(courseId).subscribe({
      next: (progressList) => {
        this.completedIds.clear();
        progressList.forEach((p) => {
          if (p.completedAt || (p.progressPercent != null && p.progressPercent >= 100)) {
            this.completedIds.add(p.lectureId);
          }
        });
        this.cdr.markForCheck();
      },
      error: () => {}
    });

    // Check if certificate has already been issued for this course
    this.certificateService.getCertificates().subscribe({
      next: (certs) => {
        const match = certs.find(c => c.courseId === courseId);
        if (match) {
          this.hasEarnedCertificate = true;
          this.populateCertData(match);
          this.cdr.markForCheck();
        }
      },
      error: () => {}
    });

    this.studentService.getCourseLectures(courseId).subscribe({
      next: (lectures) => {
        this.lectures = lectures;
        const target = lectures.find(l => l.id === lectureId) || lectures[0] || null;
        this.currentLecture = target;
        this.loading = false;
        this.cdr.markForCheck();

        if (target) {
          this.loadLectureProgress(target.id);
          this.loadLectureBookmarks(target.id);
          this.checkLectureQuiz(target.id);
        }
      },
      error: () => {
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private checkLectureQuiz(lectureId: number): void {
    this.assessmentService.getAssessmentsByLecture(lectureId).subscribe({
      next: (tests) => {
        this.hasLectureQuiz = tests && tests.length > 0;
        this.cdr.markForCheck();
      },
      error: () => {
        this.hasLectureQuiz = false;
      }
    });
  }

  onVideoEnded(): void {
    if (this.hasLectureQuiz && this.currentLecture && !this.completedIds.has(this.currentLecture.id)) {
      this.showPostQuizModal = true;
      this.cdr.markForCheck();
    } else {
      this.markComplete(false);
    }
  }

  openQuizModal(): void {
    this.showPostQuizModal = true;
  }

  onQuizPassed(lectureId: number): void {
    this.notificationService.success('Quiz passed! Lecture progress verified.');
    this.markComplete(true);
    this.showPostQuizModal = false;
    this.cdr.markForCheck();
  }

  private loadLectureProgress(lectureId: number): void {
    this.studentService.getWatchProgress(lectureId).subscribe({
      next: (progress: LectureProgress) => {
        this.currentProgress = progress;
        if (progress.completedAt || progress.progressPercent >= 80) {
          this.completedIds.add(lectureId);
        } else {
          this.completedIds.delete(lectureId);
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.currentProgress = null;
        this.cdr.markForCheck();
      }
    });
  }

  private loadLectureBookmarks(lectureId: number): void {
    this.bookmarkService.getBookmarksByLecture(lectureId).subscribe({
      next: (bookmarks) => {
        this.isBookmarked = bookmarks && bookmarks.length > 0;
        this.cdr.markForCheck();
      },
      error: () => {
        this.isBookmarked = false;
      }
    });
  }

  onTimeUpdate(update: VideoTimeUpdate): void {
    this.playbackPosition = Math.floor(update.currentTime);
    this.videoDuration = Math.floor(update.duration);

    if (this.currentLecture && update.duration > 0) {
      const watchedRatio = update.currentTime / update.duration;
      if (watchedRatio >= 0.8 && this.autoCompletedForLecture !== this.currentLecture.id && !this.completedIds.has(this.currentLecture.id) && !this.hasLectureQuiz) {
        this.autoCompletedForLecture = this.currentLecture.id;
        this.markComplete(false);
      }
    }
  }

  onTranscriptSeek(seconds: number): void {
    if (this.videoPlayer) {
      this.videoPlayer.seekTo(seconds);
    }
  }

  onLectureGridSelect(lectureId: number): void {
    if (this.courseId) {
      this.router.navigate(['/student/courses', this.courseId, 'lectures', lectureId]);
    }
  }

  toggleBookmark(): void {
    if (!this.currentLecture) return;
    const lectureId = this.currentLecture.id;

    if (this.isBookmarked) {
      this.bookmarkService.deleteBookmarkByLecture(lectureId).subscribe({
        next: () => {
          this.isBookmarked = false;
          this.notificationService.success('Bookmark removed');
          this.cdr.markForCheck();
        },
        error: () => {
          this.notificationService.error('Failed to remove bookmark');
        }
      });
    } else {
      this.bookmarkService.createBookmark({
        lectureId: lectureId,
        timestampSeconds: this.playbackPosition,
        label: `Bookmark at ${this.formatTime(this.playbackPosition)}`
      }).subscribe({
        next: () => {
          this.isBookmarked = true;
          this.notificationService.success('Bookmark saved at ' + this.formatTime(this.playbackPosition));
          this.cdr.markForCheck();
        },
        error: () => {
          this.notificationService.error('Failed to save bookmark');
        }
      });
    }
  }

  toggleComplete(): void {
    if (!this.currentLecture) return;
    const lectureId = this.currentLecture.id;

    if (this.completedIds.has(lectureId)) {
      this.studentService.unmarkLectureComplete(lectureId).subscribe({
        next: (progress) => {
          this.completedIds.delete(lectureId);
          this.currentProgress = progress;
          this.notificationService.info('Lecture unmarked as complete');
          this.cdr.markForCheck();
        },
        error: () => {
          this.notificationService.error('Could not update progress');
        }
      });
    } else {
      this.markComplete(true);
    }
  }

  markComplete(manual = true): void {
    if (!this.currentLecture) return;
    const lectureId = this.currentLecture.id;
    this.studentService.markLectureComplete(lectureId).subscribe({
      next: (progress) => {
        this.completedIds.add(lectureId);
        this.currentProgress = progress;
        if (manual) {
          this.notificationService.success('Lecture marked as complete');
        }
        this.cdr.markForCheck();
      },
      error: () => {
        if (manual) {
          this.notificationService.error('Could not update progress');
        }
      }
    });
  }

  get watchedPercent(): number {
    if (!this.currentProgress || !this.currentLecture) return 0;
    return this.completedIds.has(this.currentLecture.id) ? 100 : 
      Math.min(100, Math.round(((this.currentProgress.secondsWatched || 0) / 1200) * 100));
  }

  get totalCourseDuration(): string {
    const totalMinutes = this.lectures.length * 18;
    const hrs = Math.floor(totalMinutes / 60);
    const mins = totalMinutes % 60;
    return hrs > 0 ? `${hrs}h ${mins}m` : `${mins}m`;
  }

  get courseCompletionPercent(): number {
    if (this.lectures.length === 0) return 0;
    return Math.round((this.completedIds.size / this.lectures.length) * 100);
  }

  openCertificateModal(): void {
    if (!this.courseId) return;
    if (this.hasEarnedCertificate && this.certModalData) {
      this.showCertModal = true;
      this.cdr.markForCheck();
      return;
    }

    this.certificateService.issueCertificate(this.courseId, 'STANDARD').subscribe({
      next: (cert) => {
        this.hasEarnedCertificate = true;
        this.populateCertData(cert);
        this.showCertModal = true;
        this.notificationService.success('🏆 Certificate of Achievement unlocked & verified!');
        this.cdr.markForCheck();
      },
      error: (err) => {
        const msg = err?.error?.message || 'Could not generate certificate. Please ensure all lectures are completed.';
        this.notificationService.error(msg);
      }
    });
  }

  private populateCertData(cert: any): void {
    const user = this.authService.currentUser;
    this.certModalData = {
      id: cert.id,
      courseId: this.courseId || cert.courseId,
      courseName: this.course?.title || cert.courseTitle || 'Masterclass',
      date: cert.issuedAt ? new Date(cert.issuedAt).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }) : new Date().toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }),
      teacher: cert.instructorName ? `Prof. ${cert.instructorName}` : (this.course?.teacherName ? `Prof. ${this.course.teacherName}` : 'Faculty Board'),
      certId: cert.verificationCode || `LS-2026-${cert.id || 'CERT'}`,
      studentName: cert.studentName || user?.fullName || 'Student',
      collegeName: user?.collegeName || 'Thakur College of Engineering & Technology',
      type: (cert.type as any) || 'STANDARD'
    };
  }

  formatTime(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }
}
