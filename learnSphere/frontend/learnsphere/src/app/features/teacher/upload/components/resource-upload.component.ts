import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UploadService } from '../../services/upload.service';
import { NotificationService } from '@core/services/notification.service';

/**
 * ResourceUploadComponent is a self-contained file picker + progress bar for
 * uploading supplementary course resources (PDF, slides, images) to the
 * backend, emitting the stored file's public URL once upload completes.
 */
@Component({
  selector: 'app-resource-upload',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="upload-box">
      <h3>Upload resource</h3>
      <p class="hint">PDF, DOC, PPT, or image files — handouts, slides, and reading material.</p>
      <input
        type="file"
        accept=".pdf,.doc,.docx,.ppt,.pptx,.txt,image/*"
        (change)="onSelect($event)"
        [disabled]="uploading" />
      <div class="progress-track" *ngIf="uploading">
        <span [style.width.%]="percent"></span>
      </div>
      <p class="status" *ngIf="uploading">Uploading… {{ percent }}%</p>
      <p class="status status--done" *ngIf="lastUrl && !uploading">Uploaded: {{ lastUrl }}</p>
    </div>
  `,
  styles: [`
    .upload-box { background: #fff; border: 1px solid #e8e7ef; border-radius: 12px; padding: 16px; }
    h3 { margin: 0 0 4px; font-size: 13px; }
    .hint { margin: 0 0 10px; font-size: 11px; color: #6b6880; }
    input[type=file] { font-size: 12px; }
    .progress-track { height: 5px; border-radius: 4px; background: #eeedf3; overflow: hidden; margin-top: 10px; }
    .progress-track span { display: block; height: 100%; background: #534ab7; }
    .status { margin: 8px 0 0; font-size: 11px; color: #6b6880; word-break: break-all; }
    .status--done { color: #1d9e75; }
  `]
})
export class ResourceUploadComponent {
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
    this.uploadService.uploadResource(file).subscribe({
      next: (evt) => {
        this.percent = evt.percent;
        if (evt.type === 'done' && evt.url) {
          this.uploading = false;
          this.lastUrl = evt.url;
          this.uploaded.emit(evt.url);
          this.notificationService.success('Resource uploaded.');
        }
      },
      error: () => {
        this.uploading = false;
        this.notificationService.error('Resource upload failed.');
      }
    });
    input.value = '';
  }
}
