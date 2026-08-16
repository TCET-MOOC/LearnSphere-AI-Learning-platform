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

@Component({
  selector: 'app-verify-college',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './verify-college.component.html',
  styleUrls: ['./verify-college.component.scss']
})
export class VerifyCollegeComponent implements OnInit {
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
      error: () => this.notificationService.error('Could not load the list of colleges.')
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
          this.notificationService.success('Affiliation request submitted for admin review.');
        } else {
          this.notificationService.success('College linked to your account.');
          this.router.navigateByUrl(this.authService.dashboardPathForRole(this.authService.currentUser?.role));
        }
      },
      error: (err: HttpErrorResponse) => {
        this.submitting = false;
        this.errorMessage = this.errorHandler.extractMessage(err, 'Could not submit your college details.');
      }
    });
  }

  skipForNow(): void {
    this.router.navigateByUrl(this.authService.dashboardPathForRole(this.authService.currentUser?.role));
  }
}
