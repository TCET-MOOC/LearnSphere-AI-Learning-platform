import { Component, OnInit, OnDestroy, ChangeDetectorRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
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
import { LectureContextService } from '@core/services/lecture-context.service';
import { FormsModule } from '@angular/forms';
import { AssignmentService } from '@core/services/assignment.service';
import { Assignment, AssignmentSubmission } from '@core/models/course.model';
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
    FormsModule,
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
export class LectureComponent implements OnInit, OnDestroy {
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
  initialSeekTime = 0;
  isBookmarked = false;

  // Anti-cheat cumulative watch tracking
  private cumulativeWatchedSeconds = 0;
  private lastReportedTime = -1;

  // Post-Video Quiz State
  hasLectureQuiz = false;
  showPostQuizModal = false;

  // Certificate Modal State
  showCertModal = false;
  certModalData: CertificateModalData | null = null;
  hasEarnedCertificate = false;

  // Workspace Dynamic Tabs
  activeSidebarTab: 'playlist' | 'transcript' = 'playlist';
  activeBottomTab: 'discussion' | 'notes' | 'resources' | 'assignments' = 'discussion';
  selectedTranscriptLanguage = 'en';

  // Assignment & Submissions State (Section 4 & 8)
  assignments: Assignment[] = [];
  selectedAssignment: Assignment | null = null;
  mySubmissions = new Map<number, AssignmentSubmission>();
  submissionText = '';
  submissionFileUrl = '';
  submittingAssignment = false;

  private routeSub?: Subscription;
  private querySub?: Subscription;

  onLanguageChange(lang: string): void {
    this.selectedTranscriptLanguage = lang;
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private studentService: StudentService,
    private assignmentService: AssignmentService,
    private bookmarkService: BookmarkService,
    private assessmentService: AssessmentService,
    private certificateService: CertificateService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private lectureContext: LectureContextService,
    private cdr: ChangeDetectorRef
  ) {}


  ngOnInit(): void {
    this.querySub = this.route.queryParams.subscribe(qParams => {
      if (qParams['t']) {
        const t = parseFloat(qParams['t']);
        if (!isNaN(t) && t > 0) {
          this.initialSeekTime = t;
          if (this.videoPlayer) {
            this.videoPlayer.seekTo(t);
          }
        }
      }
    });

    this.routeSub = this.route.paramMap.subscribe(params => {
      const courseIdParam = params.get('courseId');
      const lectureIdParam = params.get('lectureId');
      if (courseIdParam && lectureIdParam) {
        this.loadCourseContext(Number(courseIdParam), Number(lectureIdParam));
      } else if (courseIdParam) {
        this.router.navigate(['/student/courses', courseIdParam, 'player'], { replaceUrl: true });
      } else {
        this.router.navigate(['/student/courses'], { replaceUrl: true });
      }
    });
  }

  ngOnDestroy(): void {
    if (this.routeSub) this.routeSub.unsubscribe();
    if (this.querySub) this.querySub.unsubscribe();
    this.lectureContext.reset();
  }

  private loadCourseContext(courseId: number, lectureId: number): void {
    this.loading = true;
    this.courseId = courseId;
    this.playbackPosition = 0;
    this.videoDuration = 0;
    this.cumulativeWatchedSeconds = 0;
    this.lastReportedTime = -1;

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

          // Delegate playback to W2-12 descriptor if canonical content exists
          if (target.contentId) {
            this.studentService.getPlaybackDescriptor(target.contentId).subscribe({
              next: (desc) => {
                if (desc) {
                  const mediaUrl = desc.streamUrl || desc.embedUrl || target.videoUrl;
                  if (mediaUrl) {
                    target.videoUrl = mediaUrl;
                  }
                  this.cdr.markForCheck();
                }
              },
              error: () => {}
            });
          }
        }
        this.loadCourseAssignments(courseId);
      },
      error: () => {
        this.loading = false;
        // If un-enrolled or course not found, return to course details
        this.router.navigate(['/student/courses', courseId]);
        this.cdr.markForCheck();
      }
    });
  }

  loadCourseAssignments(courseId: number): void {
    this.assignmentService.getAssignmentsByCourse(courseId).subscribe({
      next: (list) => {
        this.assignments = list || [];
        if (this.assignments.length > 0 && !this.selectedAssignment) {
          this.selectAssignment(this.assignments[0]);
        }
        this.cdr.markForCheck();
      },
      error: () => { this.assignments = []; }
    });

    this.assignmentService.getMySubmissions().subscribe({
      next: (subs) => {
        this.mySubmissions.clear();
        (subs || []).forEach(s => {
          this.mySubmissions.set(s.assignmentId, s);
        });
        if (this.selectedAssignment) {
          this.selectAssignment(this.selectedAssignment);
        }
        this.cdr.markForCheck();
      },
      error: () => {}
    });
  }

  selectAssignment(assign: Assignment): void {
    this.selectedAssignment = assign;
    const sub = this.mySubmissions.get(assign.assignmentId || 0);
    if (sub) {
      this.submissionText = sub.submissionText || '';
      this.submissionFileUrl = sub.fileUrl || '';
    } else {
      this.submissionText = '';
      this.submissionFileUrl = '';
    }
  }

  submitAssignment(): void {
    if (!this.selectedAssignment || !this.selectedAssignment.assignmentId) return;
    if (!this.submissionText.trim() && !this.submissionFileUrl.trim()) {
      this.notificationService.error('Please provide submission text or a file link.');
      return;
    }
    this.submittingAssignment = true;
    this.assignmentService.submitAssignment(this.selectedAssignment.assignmentId, {
      submissionText: this.submissionText,
      fileUrl: this.submissionFileUrl
    }).subscribe({
      next: (sub) => {
        this.submittingAssignment = false;
        this.mySubmissions.set(sub.assignmentId, sub);
        this.notificationService.success('Assignment submitted successfully!');
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.submittingAssignment = false;
        this.notificationService.error(err?.error?.message || 'Failed to submit assignment.');
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
        if (progress.completedAt || (progress.progressPercent != null && progress.progressPercent >= 100)) {
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
    this.lectureContext.updateContext({
      currentTimestamp: update.currentTime,
      activeCueText: update.activeCueText ?? ''
    });

    this.playbackPosition = Math.floor(update.currentTime);
    this.videoDuration = Math.floor(update.duration);

    // Cumulative anti-cheat watch increment
    if (this.lastReportedTime >= 0) {
      const delta = update.currentTime - this.lastReportedTime;
      // Normal playback progress delta between 0.1s and 1.5s
      if (delta > 0 && delta <= 1.5) {
        this.cumulativeWatchedSeconds += delta;
      }
    }
    this.lastReportedTime = update.currentTime;

    if (this.currentLecture && update.duration > 10) {
      const requiredWatchTime = update.duration * 0.8;
      const isSufficientlyWatched = this.cumulativeWatchedSeconds >= requiredWatchTime || update.currentTime >= update.duration - 2;

      if (
        isSufficientlyWatched && 
        this.autoCompletedForLecture !== this.currentLecture.id && 
        !this.completedIds.has(this.currentLecture.id) && 
        !this.hasLectureQuiz
      ) {
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

  onNoteModalOpen(): void {
    if (this.videoPlayer) {
      this.videoPlayer.pause();
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
      if (this.hasLectureQuiz) {
        this.openQuizModal();
        return;
      }
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
    if (!this.currentLecture) return 0;
    if (this.completedIds.has(this.currentLecture.id)) return 100;
    if (this.videoDuration > 0) {
      return Math.min(99, Math.round((this.playbackPosition / this.videoDuration) * 100));
    }
    return 0;
  }

  get totalCourseDuration(): string {
    const totalMinutes = this.lectures.length * 15;
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

  formatTime(seconds: number | undefined | null): string {
    if (!seconds || seconds <= 0) return '00:00';
    const hrs = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = Math.floor(seconds % 60);

    if (hrs > 0) {
      return `${hrs}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }
}
