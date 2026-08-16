import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { VideoUploadComponent } from './components/video-upload.component';
import { ResourceUploadComponent } from './components/resource-upload.component';
import { NotificationService } from '@core/services/notification.service';

interface UploadedFile {
  url: string;
  kind: 'video' | 'resource';
  uploadedAt: Date;
}

/**
 * UploadComponent is the teacher's content-upload hub: upload lecture videos
 * or course resources and get back a backend-hosted URL that can be pasted
 * into a lecture/course while managing it.
 */
@Component({
  selector: 'app-teacher-upload',
  standalone: true,
  imports: [CommonModule, RouterModule, VideoUploadComponent, ResourceUploadComponent],
  templateUrl: './upload.component.html',
  styleUrls: ['./upload.component.scss']
})
export class UploadComponent {
  recentUploads: UploadedFile[] = [];

  constructor(private notificationService: NotificationService) {}

  onVideoUploaded(url: string): void {
    this.recentUploads.unshift({ url, kind: 'video', uploadedAt: new Date() });
  }

  onResourceUploaded(url: string): void {
    this.recentUploads.unshift({ url, kind: 'resource', uploadedAt: new Date() });
  }

  copy(url: string): void {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(url).then(() => this.notificationService.success('URL copied.'));
    }
  }
}
