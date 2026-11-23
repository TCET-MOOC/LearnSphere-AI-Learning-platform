import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UploadService } from '../../services/upload.service';
import { NotificationService } from '@core/services/notification.service';
import { TEACHER_UPLOAD_VIDEO_UPLOAD_UI } from '@core/content/teacher-upload-video-upload.ui';

/**
 * VideoUploadComponent is a self-contained file picker + progress bar for
 * uploading a lecture video to the backend, emitting the stored file's
 * public URL once the upload completes.
 */
@Component({
  selector: 'app-video-upload',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="upload-box">
      <h3 id="video-upload-heading">{{ ui.uploadVideo }}</h3>
      <p class="hint">{{ ui.mp4MovOrWebm }}</p>
      <input type="file" accept="video/*" aria-labelledby="video-upload-heading" (change)="onSelect($event)" [disabled]="uploading" />
      <div class="progress-track" *ngIf="uploading">
        <span [style.width.%]="percent"></span>
      </div>
      <p class="status" *ngIf="uploading">{{ ui.uploading }} {{ percent }}%</p>
      <p class="status status--done" *ngIf="lastUrl && !uploading">{{ ui.uploaded }} {{ lastUrl }}</p>
    </div>
  `,
  styles: [`
    .upload-box {
      background: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: var(--border-radius-lg);
      padding: 18px;
      box-shadow: var(--card-shadow);
    }
    h3 { margin: 0 0 4px; font-size: 14px; font-weight: 600; color: var(--text-primary); }
    .hint { margin: 0 0 10px; font-size: 12px; color: var(--text-muted); }
    input[type=file] { font-size: 12px; color: var(--text-primary); }
    .progress-track { height: 6px; border-radius: var(--border-radius-sm); background: var(--bg-hover); overflow: hidden; margin-top: 10px; }
    .progress-track span { display: block; height: 100%; background: var(--brand-primary); }
    .status { margin: 8px 0 0; font-size: 12px; color: var(--text-secondary); word-break: break-all; }
    .status--done { color: var(--status-green-text); font-weight: 600; }
  `]
})
export class VideoUploadComponent {
  /** Template copy (Wave 7 gate): `core/content/teacher-upload-video-upload.ui.ts`. */
  protected readonly ui = TEACHER_UPLOAD_VIDEO_UPLOAD_UI;
  @Output() uploaded = new EventEmitter<string>();

  uploading = false;
  percent = 0;
  lastUrl: string | null = null;

  constructor(private uploadService: UploadService, private notificationService: NotificationService) {}

  onSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.uploading = true;
    this.percent = 0;
    this.uploadService.uploadVideo(file).subscribe({
      next: (evt) => {
        this.percent = evt.percent;
        if (evt.type === 'done' && evt.url) {
          this.uploading = false;
          this.lastUrl = evt.url;
          this.uploaded.emit(evt.url);
          this.notificationService.success('Video uploaded.');
        }
      },
      error: () => {
        this.uploading = false;
        this.notificationService.error('Video upload failed.');
      }
    });
    input.value = '';
  }
}
