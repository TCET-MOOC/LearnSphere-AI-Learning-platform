import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { CertificateService } from '../services/certificate.service';
import { AssessmentService } from '../services/assessment.service';
import { StudentService } from '../services/student.service';
import { AuthService } from '@core/auth/auth.service';
import { Assessment, Certificate } from '@core/models/assessment.model';
import { Course } from '@core/models/course.model';
import { NotificationService } from '@core/services/notification.service';
import { LucideAngularModule } from 'lucide-angular';

export interface EarnedCertView {
  courseId: number;
  courseName: string;
  date: string;
  teacher: string;
  icon: string;
  certId: string;
  studentName: string;
  collegeName: string;
  type: 'STANDARD' | 'REMEDIAL';
}

export interface InProgressCertView {
  courseId: number;
  courseName: string;
  requirement: string;
}

export interface RemedialCertView {
  courseId: number;
  courseName: string;
  requirement: string;
  statusText: string;
  testId: number | null;
}

@Component({
  selector: 'app-certificates',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './certificates.component.html',
  styleUrls: ['./certificates.component.scss']
})
export class CertificatesComponent implements OnInit {
  loading = true;

  earnedCerts: EarnedCertView[] = [];
  inProgressCerts: InProgressCertView[] = [];
  remedialCerts: RemedialCertView[] = [];

  // Certificate Modal State
  selectedCertForPreview: EarnedCertView | null = null;
  selectedCertForShare: EarnedCertView | null = null;
  linkCopied = false;

  constructor(
    private certificateService: CertificateService,
    private assessmentService: AssessmentService,
    private studentService: StudentService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    forkJoin({
      certificates: this.certificateService.getCertificates(),
      courses: this.studentService.getEnrolledCourses()
    }).subscribe({
      next: ({ certificates, courses }) => {
        this.buildEarned(certificates, courses);
        this.buildInProgress(certificates, courses);
        this.loadRemedial(certificates, courses);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.notificationService.error('Could not load certificates.');
        this.cdr.markForCheck();
      }
    });
  }

  private buildEarned(certificates: Certificate[], courses: Course[]): void {
    const user = this.authService.currentUser;
    const studentName = user?.fullName || 'LearnSphere Scholar';
    const collegeName = user?.collegeName || 'Thakur College of Engineering & Technology';

    this.earnedCerts = certificates.map((cert) => {
      const course = courses.find((c) => c.id === cert.courseId);
      const generatedId = cert.verificationCode || `LS-${new Date(cert.issuedAt).getFullYear()}-${(cert.id * 7919 + 104729) % 900000 + 100000}`;
      return {
        courseId: cert.courseId,
        courseName: cert.courseTitle || course?.title || 'Course',
        date: new Date(cert.issuedAt).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }),
        teacher: cert.instructorName ? `Prof. ${cert.instructorName}` : (course?.teacherName ? `Prof. ${course.teacherName}` : 'Faculty Board'),
        icon: cert.type === 'REMEDIAL' ? 'graduation-cap' : 'trophy',
        certId: generatedId,
        studentName: cert.studentName || studentName,
        collegeName,
        type: cert.type || 'STANDARD'
      };
    });
  }

  private buildInProgress(certificates: Certificate[], courses: Course[]): void {
    const standardCourseIds = new Set(
      certificates.filter((c) => c.type === 'STANDARD').map((c) => c.courseId)
    );
    this.inProgressCerts = courses
      .filter((c) => !standardCourseIds.has(c.id))
      .map((c) => ({
        courseId: c.id,
        courseName: c.title,
        requirement: c.lectureCount
          ? `Complete all ${c.lectureCount} lecture${c.lectureCount === 1 ? '' : 's'} to unlock your certificate.`
          : 'Complete all lectures to unlock your certificate.'
      }));
  }

  private loadRemedial(certificates: Certificate[], courses: Course[]): void {
    const remedialCourseIds = new Set(
      certificates.filter((c) => c.type === 'REMEDIAL').map((c) => c.courseId)
    );
    const candidates = courses.filter((c) => !remedialCourseIds.has(c.id));

    if (candidates.length === 0) {
      this.remedialCerts = [];
      this.loading = false;
      this.cdr.markForCheck();
      return;
    }

    forkJoin(
      candidates.map((course) =>
        this.assessmentService.getAssessments(course.id).pipe(catchError(() => of([] as Assessment[])))
      )
    ).subscribe({
      next: (lists) => {
        this.remedialCerts = candidates
          .map((course, i) => ({ course, remedialTest: lists[i].find((a) => a.isRemedial) || null }))
          .filter((entry) => entry.remedialTest)
          .map((entry) => ({
            courseId: entry.course.id,
            courseName: entry.course.title,
            requirement: `Score 40%+ on "${entry.remedialTest!.title}" to unlock this certificate.`,
            statusText: 'Not attempted yet',
            testId: entry.remedialTest!.id
          }));
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  attemptRemedial(cert: RemedialCertView): void {
    if (!cert.testId) return;
    this.router.navigate(['/student/assessments', cert.testId]);
  }

  download(cert: EarnedCertView): void {
    this.selectedCertForPreview = cert;
  }

  share(cert: EarnedCertView): void {
    this.selectedCertForShare = cert;
    this.linkCopied = false;
  }

  closePreview(): void {
    this.selectedCertForPreview = null;
  }

  closeShare(): void {
    this.selectedCertForShare = null;
    this.linkCopied = false;
  }

  printCertificate(): void {
    window.print();
  }

  getShareUrl(cert: EarnedCertView): string {
    return `${window.location.origin}/verify-certificate/${cert.certId}`;
  }

  getQrCodeUrl(cert: EarnedCertView): string {
    const shareUrl = this.getShareUrl(cert);
    return `https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=${encodeURIComponent(shareUrl)}`;
  }

  copyShareLink(cert: EarnedCertView): void {
    const url = this.getShareUrl(cert);
    if (navigator.clipboard) {
      navigator.clipboard.writeText(url).then(() => {
        this.linkCopied = true;
        this.notificationService.success('Certificate verification link copied!');
        this.cdr.markForCheck();
      });
    }
  }

  shareOnLinkedIn(cert: EarnedCertView): void {
    const title = encodeURIComponent(`${cert.courseName} - LearnSphere Certification`);
    const certUrl = encodeURIComponent(this.getShareUrl(cert));
    const linkedInUrl = `https://www.linkedin.com/profile/add?startTask=CERTIFICATION_NAME&name=${title}&organizationName=LearnSphere%20AI&certUrl=${certUrl}&certId=${cert.certId}`;
    window.open(linkedInUrl, '_blank');
  }
}
