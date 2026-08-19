import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, Download, Share2, X, Check, Copy, ExternalLink, Award } from 'lucide-angular';
import { NotificationService } from '@core/services/notification.service';

export interface CertificateModalData {
  id?: number;
  courseId?: number;
  courseName: string;
  date: string;
  teacher: string;
  certId: string;
  studentName: string;
  collegeName: string;
  type?: 'STANDARD' | 'REMEDIAL';
}

@Component({
  selector: 'app-certificate-modal',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './certificate-modal.component.html',
  styleUrls: ['./certificate-modal.component.scss']
})
export class CertificateModalComponent {
  @Input() certificate: CertificateModalData | null = null;
  @Output() close = new EventEmitter<void>();

  linkCopied = false;
  showShareSubmodal = false;

  constructor(private notificationService: NotificationService) {}

  getQrCodeUrl(cert: CertificateModalData): string {
    const code = cert.certId || 'LS-2026-CERT';
    const verifyUrl = `${window.location.origin}/verify-certificate/${code}`;
    return `https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=${encodeURIComponent(verifyUrl)}&bgcolor=FAF7F0&color=1A1423`;
  }

  printCertificate(): void {
    window.print();
  }

  getShareUrl(cert: CertificateModalData): string {
    const code = cert.certId || 'LS-2026-CERT';
    return `${window.location.origin}/verify-certificate/${code}`;
  }

  copyShareLink(cert: CertificateModalData): void {
    const url = this.getShareUrl(cert);
    navigator.clipboard.writeText(url).then(() => {
      this.linkCopied = true;
      this.notificationService.success('Verification URL copied to clipboard!');
      setTimeout(() => this.linkCopied = false, 3000);
    });
  }

  shareOnLinkedIn(cert: CertificateModalData): void {
    const orgName = 'LearnSphere AI Academy';
    const certName = encodeURIComponent(`${cert.courseName} — Certificate of Achievement`);
    const certUrl = encodeURIComponent(this.getShareUrl(cert));
    const certId = encodeURIComponent(cert.certId);
    const liUrl = `https://www.linkedin.com/profile/add?startTask=CERTIFICATION_NAME&name=${certName}&organizationName=${encodeURIComponent(orgName)}&certUrl=${certUrl}&certId=${certId}`;
    window.open(liUrl, '_blank', 'noopener,noreferrer');
  }

  onBackdropClick(): void {
    this.close.emit();
  }
}
