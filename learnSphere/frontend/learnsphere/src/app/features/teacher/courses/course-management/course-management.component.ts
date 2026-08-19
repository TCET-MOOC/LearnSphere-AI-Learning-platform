import { Component, OnInit, ChangeDetectorRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TeacherService } from '../../services/teacher.service';
import { ApiService } from '@core/services/api.service';
import { Course } from '@core/models/course.model';
import { CourseEditorComponent } from './components/course-editor.component';
import { LectureListComponent } from './components/lecture-list.component';
import { QuizBuilderComponent } from '../../upload/components/quiz-builder.component';
import { NotificationService } from '@core/services/notification.service';
import { LucideAngularModule } from 'lucide-angular';

interface ImportedLecture {
  videoId: string;
  title: string;
  description: string;
  videoUrl: string;
  thumbnailUrl: string;
  durationSeconds: number;
  orderIndex: number;
}

@Component({
  selector: 'app-course-management',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule,
    RouterModule, 
    CourseEditorComponent, 
    LectureListComponent, 
    QuizBuilderComponent,
    LucideAngularModule
  ],
  templateUrl: './course-management.component.html',
  styleUrls: ['./course-management.component.scss']
})
export class CourseManagementComponent implements OnInit {
  @ViewChild(LectureListComponent) lectureListComponent?: LectureListComponent;

  courseId: number | null = null;
  course: Course | null = null;
  loading = false;
  saving = false;

  // YouTube Playlist Importer Modal State
  showImportModal = false;
  playlistUrl = '';
  importingPlaylist = false;
  savingImported = false;
  importedLectures: ImportedLecture[] = [];
  playlistTitle = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private teacherService: TeacherService,
    private apiService: ApiService,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.courseId = Number(idParam);
      this.load();
    }
  }

  get isEditMode(): boolean {
    return !!this.courseId;
  }

  load(): void {
    if (!this.courseId) return;
    this.loading = true;
    this.cdr.markForCheck();
    this.teacherService.getCourse(this.courseId).subscribe({
      next: (course) => {
        this.course = course;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.cdr.markForCheck();
        this.notificationService.error('Could not load course.');
        this.router.navigate(['/teacher/courses']);
      }
    });
  }

  onSave(payload: Partial<Course>): void {
    this.saving = true;
    this.cdr.markForCheck();
    if (this.isEditMode && this.courseId) {
      this.teacherService.updateCourse(this.courseId, payload).subscribe({
        next: (course) => {
          this.course = course;
          this.saving = false;
          this.cdr.markForCheck();
          this.notificationService.success('Course updated.');
        },
        error: () => {
          this.saving = false;
          this.cdr.markForCheck();
          this.notificationService.error('Could not update course.');
        }
      });
    } else {
      this.teacherService.createCourse(payload).subscribe({
        next: (course) => {
          this.saving = false;
          this.cdr.markForCheck();
          this.notificationService.success('Course created — now add some lectures.');
          this.router.navigate(['/teacher/courses', course.id, 'manage']);
        },
        error: () => {
          this.saving = false;
          this.cdr.markForCheck();
          this.notificationService.error('Could not create course.');
        }
      });
    }
  }

  onDeleteCourse(): void {
    if (!this.courseId) return;
    if (!confirm('Are you sure you want to permanently delete this course? All associated lectures, quizzes, and enrollments will be deleted.')) return;

    this.loading = true;
    this.cdr.markForCheck();
    this.teacherService.deleteCourse(this.courseId).subscribe({
      next: () => {
        this.notificationService.success('Course deleted successfully.');
        this.router.navigate(['/teacher/courses']);
      },
      error: (err) => {
        this.loading = false;
        this.cdr.markForCheck();
        this.notificationService.error(err?.error?.message || 'Could not delete course.');
      }
    });
  }

  openImportModal(): void {
    this.showImportModal = true;
    this.playlistUrl = '';
    this.importedLectures = [];
    this.cdr.markForCheck();
  }

  closeImportModal(): void {
    this.showImportModal = false;
    this.cdr.markForCheck();
  }

  fetchPlaylist(): void {
    if (!this.playlistUrl.trim()) {
      this.notificationService.info('Please enter a valid YouTube Video or Playlist URL.');
      return;
    }

    this.importingPlaylist = true;
    this.cdr.markForCheck();
    this.apiService.post<any>('/teacher/youtube/import-playlist', { playlistUrl: this.playlistUrl.trim() }).subscribe({
      next: (res) => {
        this.importingPlaylist = false;
        this.playlistTitle = res.playlistTitle;
        this.importedLectures = res.lectures || [];
        const count = this.importedLectures.length;
        if (count === 1) {
          this.notificationService.success(`Found YouTube video: "${this.playlistTitle}"!`);
        } else {
          this.notificationService.success(`Found ${count} lectures in playlist!`);
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.importingPlaylist = false;
        const msg = err?.error?.message || (typeof err?.error === 'string' ? err.error : 'Could not import YouTube video/playlist. Please check the URL.');
        this.notificationService.error(msg);
        this.cdr.markForCheck();
      }
    });
  }

  applyImportedLectures(): void {
    if (!this.courseId || this.importedLectures.length === 0) return;

    this.savingImported = true;
    this.cdr.markForCheck();

    // Map extracted lectures to backend entity structure
    const payload = this.importedLectures.map((item, idx) => ({
      title: item.title,
      number: idx + 1,
      videoUrl: item.videoUrl,
      duration: item.durationSeconds || 0,
      status: 'PUBLISHED',
      isDownloadable: false
    }));

    this.apiService.post<any[]>(`/teacher/courses/${this.courseId}/bulk-lectures`, payload).subscribe({
      next: (savedLectures) => {
        this.savingImported = false;
        this.notificationService.success(`Successfully imported and saved ${savedLectures.length} lectures into course curriculum!`);
        this.showImportModal = false;
        this.lectureListComponent?.load();
        this.load();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.savingImported = false;
        const msg = err?.error?.message || (typeof err?.error === 'string' ? err.error : 'Failed to save imported lectures to course. Please try again.');
        this.notificationService.error(msg);
        this.cdr.markForCheck();
      }
    });
  }
}
