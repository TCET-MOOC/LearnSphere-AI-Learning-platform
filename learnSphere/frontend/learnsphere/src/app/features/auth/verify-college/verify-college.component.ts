import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '@core/auth/auth.service';
import { CollegeService } from '@core/services/college.service';
import { ErrorHandlerService } from '@core/services/error-handler.service';
import { NotificationService } from '@core/services/notification.service';
import { College, UserRole } from '@core/models/user.model';
import { VERIFY_COLLEGE_COPY } from '@core/content/verify-college.content';

@Component({
  selector: 'app-verify-college',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './verify-college.component.html',
  styleUrls: ['./verify-college.component.scss']
})
export class VerifyCollegeComponent implements OnInit {
  protected readonly copy = VERIFY_COLLEGE_COPY;
  colleges: College[] = [];
  isTeacher = false;
  submitting = false;
  submitted = false;
  errorMessage = '';

  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private collegeService: CollegeService,
    private errorHandler: ErrorHandlerService,
    private notificationService: NotificationService
  ) {
    this.form = this.fb.group({
      collegeId: [null, Validators.required],
      claimedDepartment: [''],
      idDocumentUrl: ['']
    });
  }

  ngOnInit(): void {
    this.isTeacher = this.authService.currentUser?.role === UserRole.TEACHER;
    if (this.isTeacher) {
      this.form.get('claimedDepartment')?.addValidators(Validators.required);
      this.form.get('idDocumentUrl')?.addValidators(Validators.required);
    }
    this.collegeService.getColleges().subscribe({
      next: (colleges) => (this.colleges = colleges),
      error: () => this.notificationService.error(VERIFY_COLLEGE_COPY.collegesFailed)
    });
  }

  onSubmit(): void {
    if (this.form.invalid || this.submitting) {
      return;
    }
    this.submitting = true;
    this.errorMessage = '';

    this.authService.verifyCollege(this.form.value).subscribe({
      next: () => {
        this.submitting = false;
        this.submitted = true;
        if (this.isTeacher) {
          this.notificationService.success(VERIFY_COLLEGE_COPY.teacherSubmitted);
        } else {
          this.notificationService.success(VERIFY_COLLEGE_COPY.studentLinked);
          this.router.navigateByUrl(this.authService.dashboardPathForRole(this.authService.currentUser?.role));
        }
      },
      error: (err: HttpErrorResponse) => {
        this.submitting = false;
        this.errorMessage = this.errorHandler.extractMessage(err, VERIFY_COLLEGE_COPY.failed);
      }
    });
  }

  skipForNow(): void {
    this.router.navigateByUrl(this.authService.dashboardPathForRole(this.authService.currentUser?.role));
  }
}
