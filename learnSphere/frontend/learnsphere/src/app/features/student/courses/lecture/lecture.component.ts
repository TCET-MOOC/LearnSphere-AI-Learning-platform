import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { StudentService } from '../../services/student.service';
import { BookmarkService } from '../../services/bookmark.service';
import { Course, Lecture, LectureProgress } from '@core/models/course.model';
import { VideoPlayerComponent, VideoTimeUpdate } from '@shared/components/video-player/video-player.component';
import { LectureGridComponent } from './components/lecture-grid.component';
import { NotesDrawerComponent } from './components/notes-drawer.component';
import { NotificationService } from '@core/services/notification.service';

@Component({
  selector: 'app-student-lecture',
  standalone: true,
  imports: [CommonModule, RouterModule, VideoPlayerComponent, LectureGridComponent, NotesDrawerComponent],
  templateUrl: './lecture.component.html',
  styleUrls: ['./lecture.component.scss']
})
export class LectureComponent implements OnInit {
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

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private studentService: StudentService,
    private bookmarkService: BookmarkService,
    private notificationService: NotificationService
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
          return;
        }
        this.studentService.getCourseLectures(candidate.id).subscribe({
          next: (lectures) => {
            if (lectures.length === 0) {
              this.courseId = candidate.id;
              this.course = candidate;
              this.lectures = [];
              this.loading = false;
              return;
            }
            this.loadCourseContext(candidate.id, lectures[0].id);
          },
          error: () => (this.loading = false)
        });
      },
      error: () => (this.loading = false)
    });
  }

  private loadCourseContext(courseId: number, lectureId: number): void {
    this.loading = true;
    this.courseId = courseId;
    this.studentService.getPublicCourse(courseId).subscribe({
      next: (course) => (this.course = course),
      error: () => (this.course = null)
    });
    this.studentService.getCourseLectures(courseId).subscribe({
      next: (lectures) => {
        this.lectures = lectures;
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
    this.selectLecture(lectureId);
  }

  selectLecture(lecture: Lecture | number): void {
    const lectureId = typeof lecture === 'number' ? lecture : lecture.id;
    if (!this.courseId) return;

    // Navigate so the URL reflects the lecture being watched (deep-linkable, matches notes navigation).
    if (this.currentLecture?.id !== lectureId) {
      this.router.navigate(['/student/courses', this.courseId, 'lecture', lectureId], { replaceUrl: true });
    }

    this.autoCompletedForLecture = null;
    this.playbackPosition = 0;
    this.loadingLecture = true;
    this.studentService.getLecture(this.courseId, lectureId).subscribe({
      next: (fullLecture) => {
        this.currentLecture = fullLecture;
        this.loadingLecture = false;
      },
      error: () => {
        this.loadingLecture = false;
      }
    });
    this.studentService.getWatchProgress(lectureId).subscribe({
      next: (progress) => {
        this.currentProgress = progress;
        if ((progress.progressPercent || 0) >= 100) {
          this.completedIds.add(lectureId);
        }
      },
      error: () => (this.currentProgress = null)
    });
  }

  onLectureGridSelect(lecture: Lecture): void {
    this.selectLecture(lecture);
  }

  onTimeUpdate(event: VideoTimeUpdate): void {
    this.playbackPosition = event.currentTime;
    if (
      this.currentLecture &&
      this.autoCompletedForLecture !== this.currentLecture.id &&
      event.duration > 0 &&
      event.currentTime / event.duration >= 0.95
    ) {
      this.autoCompletedForLecture = this.currentLecture.id;
      this.markComplete();
    }
  }

  markComplete(): void {
    if (!this.currentLecture) return;
    const lectureId = this.currentLecture.id;
    this.studentService.markLectureComplete(lectureId).subscribe({
      next: (progress) => {
        this.currentProgress = progress;
        this.completedIds.add(lectureId);
        this.notificationService.success('Lecture marked as complete.');
      },
      error: () => this.notificationService.error('Could not update progress.')
    });
  }

  get watchedPercent(): number {
    return this.currentProgress?.progressPercent || 0;
  }

  addBookmark(): void {
    if (!this.currentLecture) return;
    const defaultLabel = `Bookmark at ${Math.floor(this.playbackPosition / 60)}:${Math.floor(this.playbackPosition % 60).toString().padStart(2, '0')}`;
    const label = window.prompt('Label this bookmark:', defaultLabel);
    if (label === null) return;
    this.bookmarkService.createBookmark({
      lectureId: this.currentLecture.id,
      timestampSeconds: Math.floor(this.playbackPosition),
      label: label || defaultLabel
    }).subscribe({
      next: () => this.notificationService.success('Bookmark saved.'),
      error: () => this.notificationService.error('Could not save bookmark.')
    });
  }
}
