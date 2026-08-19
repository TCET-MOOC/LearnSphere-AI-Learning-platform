import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CertificateService } from '../../student/services/certificate.service';
import { CertificateVerificationResult } from '@core/models/assessment.model';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-verify-certificate',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, LucideAngularModule],
  templateUrl: './verify-certificate.component.html',
  styleUrls: ['./verify-certificate.component.scss']
})
export class VerifyCertificateComponent implements OnInit {
  searchCode: string = '';
  loading = false;
  hasSearched = false;
  result: CertificateVerificationResult | null = null;
  errorMessage: string = '';

  constructor(
    private route: ActivatedRoute,
    private certificateService: CertificateService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const codeParam = this.route.snapshot.paramMap.get('code');
    if (codeParam) {
      this.searchCode = codeParam.trim();
      this.performVerification(this.searchCode);
    }
  }

  onSearch(): void {
    if (!this.searchCode || !this.searchCode.trim()) return;
    this.performVerification(this.searchCode.trim());
  }

  performVerification(code: string): void {
    this.loading = true;
    this.hasSearched = true;
    this.errorMessage = '';
    this.result = null;

    this.certificateService.verifyCertificate(code).subscribe({
      next: (res: CertificateVerificationResult) => {
        this.loading = false;
        this.result = res;
        this.cdr.markForCheck();
      },
      error: (_err: unknown) => {
        this.loading = false;
        this.result = {
          valid: false,
          verificationCode: code,
          message: 'Unable to verify credential. Please ensure the code is correct.'
        };
        this.cdr.markForCheck();
      }
    });
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  printCertificate(): void {
    window.print();
  }

  getQrCodeUrl(code: string): string {
    const url = `${window.location.origin}/verify-certificate/${code}`;
    return `https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=${encodeURIComponent(url)}`;
  }
}
