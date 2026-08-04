import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TeacherService } from '../../../services/teacher.service';
import { UploadService } from '../../../services/upload.service';
import { Lecture } from '@core/models/course.model';
import { NotificationService } from '@core/services/notification.service';
import { DurationPipe } from '@shared/pipes/duration.pipe';

/**
 * LectureListComponent manages the lectures belonging to a single course:
 * lists them in order, and provides an add/edit form (including a video file
 * upload that populates the videoUrl field via UploadService) plus delete.
 */
@Component({
  selector: 'app-lecture-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DurationPipe],
  templateUrl: './lecture-list.component.html',
  styleUrls: ['./lecture-list.component.scss']
})
export class LectureListComponent implements OnChanges {
  @Input() courseId!: number;

  lectures: Lecture[] = [];
  loading = true;

  showForm = false;
  editingLecture: Lecture | null = null;
  form: FormGroup;
  saving = false;

  uploadingVideo = false;
  uploadPercent = 0;

  constructor(
    private teacherService: TeacherService,
    private uploadService: UploadService,
    private notificationService: NotificationService,
    private fb: FormBuilder
  ) {
    this.form = this.fb.group({
      title: ['', Validators.required],
      number: [1, [Validators.required, Validators.min(1)]],
      videoUrl: [''],
      duration: [0, [Validators.required, Validators.min(0)]],
      isDownloadable: [false],
      status: ['DRAFT']
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['courseId'] && this.courseId) {
      this.load();
    }
  }

  load(): void {
    this.loading = true;
    this.teacherService.getLectures(this.courseId).subscribe({
      next: (lectures) => {
        this.lectures = lectures.sort((a, b) => a.number - b.number);
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  openAddForm(): void {
    this.editingLecture = null;
    this.form.reset({
      title: '',
      number: this.lectures.length + 1,
      videoUrl: '',
      duration: 0,
      isDownloadable: false,
      status: 'DRAFT'
    });
    this.showForm = true;
  }

  openEditForm(lecture: Lecture): void {
    this.editingLecture = lecture;
    this.form.reset({
      title: lecture.title,
      number: lecture.number,
      videoUrl: lecture.videoUrl,
      duration: lecture.duration,
      isDownloadable: lecture.isDownloadable,
      status: lecture.status
    });
    this.showForm = true;
  }

  cancelForm(): void {
    this.showForm = false;
    this.editingLecture = null;
  }

  onVideoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.uploadingVideo = true;
    this.uploadPercent = 0;
    this.uploadService.uploadVideo(file).subscribe({
      next: (evt) => {
        this.uploadPercent = evt.percent;
        if (evt.type === 'done' && evt.url) {
          this.form.patchValue({ videoUrl: evt.url });
          this.uploadingVideo = false;
          this.notificationService.success('Video uploaded.');
        }
      },
      error: () => {
        this.uploadingVideo = false;
        this.notificationService.error('Video upload failed.');
      }
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    this.saving = true;
    const payload: Partial<Lecture> = this.form.value;

    const request$ = this.editingLecture
      ? this.teacherService.updateLecture(this.editingLecture.id, payload)
      : this.teacherService.addLecture(this.courseId, payload);

    request$.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.notificationService.success(this.editingLecture ? 'Lecture updated.' : 'Lecture added.');
        this.load();
      },
      error: () => {
        this.saving = false;
        this.notificationService.error('Could not save lecture.');
      }
    });
  }

  remove(lecture: Lecture): void {
    if (!confirm(`Delete "${lecture.title}"?`)) return;
    this.teacherService.deleteLecture(lecture.id).subscribe({
      next: () => {
        this.lectures = this.lectures.filter(l => l.id !== lecture.id);
        this.notificationService.success('Lecture deleted.');
      },
      error: () => this.notificationService.error('Could not delete lecture.')
    });
  }
}
