import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { Certificate, CertificateType, CertificateVerificationResult } from '@core/models/assessment.model';

/**
 * CertificateService owns the certificate-issuance & public verification API integrations:
 * listing certificates already earned by the current student,
 * requesting a new one once eligible, and public verification.
 */
@Injectable({
  providedIn: 'root'
})
export class CertificateService {

  constructor(private apiService: ApiService) {}

  /** Certificates already issued to the current student. */
  getCertificates(): Observable<Certificate[]> {
    return this.apiService.get<Certificate[]>('/certificates');
  }

  /**
   * Attempts to issue a certificate for a course. The backend enforces
   * eligibility (lecture completion for STANDARD, a passed remedial test
   * for REMEDIAL) and returns 400 with a reason if not yet eligible.
   */
  issueCertificate(courseId: number, type: CertificateType): Observable<Certificate> {
    return this.apiService.post<Certificate>('/certificates/issue', { courseId, type });
  }

  /** Public verification of a certificate by verification code. */
  verifyCertificate(code: string): Observable<CertificateVerificationResult> {
    return this.apiService.get<CertificateVerificationResult>(`/certificates/verify/${encodeURIComponent(code)}`);
  }
}
