import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { StudentService } from '../../services/student.service';
import { Course, Lecture } from '@core/models/course.model';
import { NotificationService } from '@core/services/notification.service';
import { CertificateModalComponent, CertificateModalData } from '@shared/components/certificate-modal/certificate-modal.component';
import { CertificateService } from '../../services/certificate.service';
import { AuthService } from '@core/auth/auth.service';
import { LucideAngularModule, Award, Download, CheckCircle, BookOpen } from 'lucide-angular';
import { PaymentsService } from '../../services/payments.service';

declare var Razorpay: any;

@Component({
  selector: 'app-course-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule, CertificateModalComponent],
  templateUrl: './course-detail.component.html',
  styleUrls: ['./course-detail.component.scss']
})
export class CourseDetailComponent implements OnInit {
  courseId!: number;
  course: Course | null = null;
  lectures: Lecture[] = [];
  isEnrolled = false;
  enrolling = false;
  loading = true;
  confirmingPayment = false;

  // Progress & Certificate State
  progressPercent = 0;
  isCompleted = false;
  hasEarnedCertificate = false;
  showCertModal = false;
  certModalData: CertificateModalData | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private studentService: StudentService,
    private certificateService: CertificateService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private paymentsService: PaymentsService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = Number(params.get('id'));
      if (!id) {
        this.router.navigate(['/student/courses']);
        return;
      }
      this.courseId = id;
      this.load();
    });
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.studentService.getPublicCourse(this.courseId).subscribe({
      next: (course) => {
        this.course = course;
        this.loading = false;
        this.checkEnrollment();
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.notificationService.error('Course not found.');
        this.cdr.markForCheck();
        this.router.navigate(['/student/courses']);
      }
    });
  }

  checkEnrollment(): void {
    this.studentService.getCourseDetail(this.courseId).subscribe({
      next: (courseDetail) => {
        this.isEnrolled = true;
        this.progressPercent = courseDetail.progressPercent != null ? Math.round(courseDetail.progressPercent) : 0;
        this.isCompleted = !!(courseDetail.completed || this.progressPercent >= 100);
        this.loadLectures();
        this.checkCertificate();
        this.cdr.markForCheck();
      },
      error: () => {
        this.isEnrolled = false;
        this.loadLectures();
        this.cdr.markForCheck();
      }
    });
  }

  checkCertificate(): void {
    this.certificateService.getCertificates().subscribe({
      next: (certs) => {
        const match = certs.find(c => c.courseId === this.courseId);
        if (match) {
          this.hasEarnedCertificate = true;
          this.populateCertData(match);
          this.cdr.markForCheck();
        }
      },
      error: () => {}
    });
  }

  openCertificateModal(): void {
    if (this.hasEarnedCertificate && this.certModalData) {
      this.showCertModal = true;
      this.cdr.markForCheck();
      return;
    }

    this.certificateService.issueCertificate(this.courseId, 'STANDARD').subscribe({
      next: (cert) => {
        this.hasEarnedCertificate = true;
        this.populateCertData(cert);
        this.showCertModal = true;
        this.notificationService.success('🏆 Certificate of Achievement unlocked & verified!');
        this.cdr.markForCheck();
      },
      error: (err) => {
        const msg = err?.error?.message || 'Could not generate certificate. Please complete all lectures.';
        this.notificationService.error(msg);
      }
    });
  }

  private populateCertData(cert: any): void {
    const user = this.authService.currentUser;
    this.certModalData = {
      id: cert.id,
      courseId: this.courseId,
      courseName: this.course?.title || cert.courseTitle || 'Masterclass',
      date: cert.issuedAt ? new Date(cert.issuedAt).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }) : new Date().toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }),
      teacher: cert.instructorName ? `Prof. ${cert.instructorName}` : (this.course?.teacherName ? `Prof. ${this.course.teacherName}` : 'Faculty Board'),
      certId: cert.verificationCode || `LS-2026-${cert.id || 'CERT'}`,
      studentName: cert.studentName || user?.fullName || 'Student',
      collegeName: user?.collegeName || 'Thakur College of Engineering & Technology',
      type: (cert.type as any) || 'STANDARD'
    };
  }

  loadLectures(): void {
    this.studentService.getCourseLectures(this.courseId).subscribe({
      next: (lectures) => {
        this.lectures = lectures;
        this.cdr.markForCheck();
      },
      error: () => {
        this.lectures = [];
        this.cdr.markForCheck();
      }
    });
  }

  get isFree(): boolean {
    return !this.course || !this.course.price || this.course.price === 0;
  }

  enroll(): void {
    if (!this.isFree) {
      this.initiatePayment();
      return;
    }
    this.enrolling = true;
    this.studentService.enrollInCourse(this.courseId).subscribe({
      next: () => {
        this.enrolling = false;
        this.notificationService.success('Enrolled! Enjoy the course.');
        this.isEnrolled = true;
        this.loadLectures();
      },
      error: (err) => {
        this.enrolling = false;
        this.notificationService.error(err?.error?.message || 'Could not enroll in this course.');
      }
    });
  }

  goToLecture(lecture: Lecture): void {
    this.router.navigate(['/student/courses', this.courseId, 'lectures', lecture.id]);
  }

  private loadRazorpayScript(): Promise<boolean> {
    return new Promise(resolve => {
      if (typeof Razorpay !== 'undefined') {
        resolve(true);
        return;
      }
      const script = document.createElement('script');
      script.src = 'https://checkout.razorpay.com/v1/checkout.js';
      script.onload = () => resolve(true);
      script.onerror = () => resolve(false);
      document.body.appendChild(script);
    });
  }

  async initiatePayment(): Promise<void> {
    const user = this.authService.currentUser;
    if (!user) return;

    this.enrolling = true;
    this.cdr.markForCheck();

    const isLoaded = await this.loadRazorpayScript();
    if (!isLoaded) {
      this.notificationService.error('Failed to load payment gateway');
      this.enrolling = false;
      this.cdr.markForCheck();
      return;
    }

    this.paymentsService.createOrder({ userId: user.id, courseId: this.courseId }).subscribe({
      next: (orderInfo) => {
        // Standard Live/Test Razorpay Gateway Modal
        const options = {
          key: orderInfo.key_id,
          amount: orderInfo.amount,
          currency: orderInfo.currency || 'INR',
          name: 'LearnSphere',
          description: `Enroll in ${this.course?.title}`,
          order_id: orderInfo.razorpay_order_id,
          handler: (response: any) => {
            this.confirmingPayment = true;
            this.enrolling = false;
            this.cdr.markForCheck();
            this.notificationService.info('Verifying payment with gateway...');

            this.paymentsService.verifyPayment({
              razorpay_order_id: response.razorpay_order_id || orderInfo.razorpay_order_id,
              razorpay_payment_id: response.razorpay_payment_id,
              razorpay_signature: response.razorpay_signature
            }).subscribe({
              next: () => {
                this.confirmingPayment = false;
                this.isEnrolled = true;
                this.notificationService.success('🎉 Payment verified successfully! You are now enrolled.');
                this.checkEnrollment();
                this.cdr.markForCheck();
              },
              error: () => {
                // Fallback check
                this.handlePaymentSuccess(user.id);
              }
            });
          },
          prefill: {
            name: user.fullName,
            email: user.email
          },
          theme: {
            color: '#6366f1'
          },
          modal: {
            ondismiss: () => {
              this.enrolling = false;
              this.notificationService.info('Payment was cancelled or closed.');
              this.cdr.markForCheck();
            }
          }
        };

        const rzp = new Razorpay(options);
        rzp.on('payment.failed', (response: any) => {
          this.enrolling = false;
          this.cdr.markForCheck();
          console.error('Razorpay Payment Failed:', response.error);
          const reason = response.error?.description || response.error?.reason || 'Payment failed';
          this.notificationService.error(`Payment failed: ${reason}`);
        });
        rzp.open();
      },
      error: (err) => {
        this.enrolling = false;
        const msg = err?.error?.message || (typeof err?.error === 'string' ? err.error : 'Failed to initiate payment. Please verify payments service is running on port 8081.');
        this.notificationService.error(msg);
        this.cdr.markForCheck();
      }
    });
  }

  private handlePaymentSuccess(userId: number): void {
    this.confirmingPayment = true;
    this.enrolling = false;
    this.cdr.markForCheck();
    
    // Poll for status
    const pollInterval = setInterval(() => {
      this.paymentsService.getUserOrders(userId).subscribe({
        next: (orders) => {
          // find the specific order for this course that is PAID
          const order = orders.find(o => o.courseId === this.courseId && o.status === 'PAID');
          if (order) {
            clearInterval(pollInterval);
            this.confirmingPayment = false;
            this.notificationService.success('Payment successful! You are now enrolled.');
            this.isEnrolled = true;
            this.checkEnrollment(); // reload state
          }
        }
      });
    }, 2000);

    // Stop polling after 30 seconds
    setTimeout(() => {
      clearInterval(pollInterval);
      if (this.confirmingPayment) {
        this.confirmingPayment = false;
        this.cdr.markForCheck();
        this.notificationService.info('Payment confirmation is taking longer than expected. Please refresh the page in a few minutes.');
      }
    }, 30000);
  }
}
