import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { CertificateService } from '../services/certificate.service';
import { AssessmentService } from '../services/assessment.service';
import { StudentService } from '../services/student.service';
import { Assessment, Certificate } from '@core/models/assessment.model';
import { Course } from '@core/models/course.model';
import { NotificationService } from '@core/services/notification.service';

interface EarnedCertView {
  courseId: number;
  courseName: string;
  date: string;
  teacher: string;
  icon: string;
}

interface InProgressCertView {
  courseId: number;
  courseName: string;
  requirement: string;
}

interface RemedialCertView {
  courseId: number;
  courseName: string;
  requirement: string;
  statusText: string;
  testId: number | null;
}

/**
 * CertificatesComponent shows the student's earned certificates plus the
 * courses that are still "in progress" (no STANDARD certificate yet) or
 * eligible for a remedial certificate (course has a remedial test, no
 * REMEDIAL certificate yet). All three buckets are derived from real API
 * data — certificates already issued, enrolled courses, and each course's
 * assessments — rather than hardcoded arrays.
 */
@Component({
  selector: 'app-certificates',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './certificates.component.html',
  styleUrls: ['./certificates.component.scss']
})
export class CertificatesComponent implements OnInit {
  loading = true;

  earnedCerts: EarnedCertView[] = [];
  inProgressCerts: InProgressCertView[] = [];
  remedialCerts: RemedialCertView[] = [];

  constructor(
    private certificateService: CertificateService,
    private assessmentService: AssessmentService,
    private studentService: StudentService,
    private notificationService: NotificationService,
    private router: Router
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
      },
      error: () => {
        this.loading = false;
        this.notificationService.error('Could not load certificates.');
      }
    });
  }

  private buildEarned(certificates: Certificate[], courses: Course[]): void {
    this.earnedCerts = certificates.map((cert) => {
      const course = courses.find((c) => c.id === cert.courseId);
      return {
        courseId: cert.courseId,
        courseName: cert.courseTitle || course?.title || 'Course',
        date: new Date(cert.issuedAt).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }),
        teacher: course?.teacherName ? `Prof. ${course.teacherName}` : 'Course instructor',
        icon: cert.type === 'REMEDIAL' ? '🎓' : '🏆'
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
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  attemptRemedial(cert: RemedialCertView): void {
    if (!cert.testId) return;
    this.router.navigate(['/student/assessments', cert.testId]);
  }

  download(cert: EarnedCertView): void {
    this.notificationService.info(`Certificate download for "${cert.courseName}" is coming soon.`);
  }

  share(cert: EarnedCertView): void {
    this.notificationService.info(`Sharing for "${cert.courseName}" is coming soon.`);
  }
}
