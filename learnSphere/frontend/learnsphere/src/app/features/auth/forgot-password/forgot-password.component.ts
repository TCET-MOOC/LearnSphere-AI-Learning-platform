import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '@core/auth/auth.service';
import { ErrorHandlerService } from '@core/services/error-handler.service';
import { NotificationService } from '@core/services/notification.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss']
})
export class ForgotPasswordComponent {
  step: 'request' | 'reset' | 'done' = 'request';
  submitting = false;
  errorMessage = '';
  infoMessage = '';

  requestForm: FormGroup;
  resetForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private errorHandler: ErrorHandlerService,
    private notificationService: NotificationService
  ) {
    this.requestForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
    this.resetForm = this.fb.group({
      token: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  submitRequest(): void {
    if (this.requestForm.invalid || this.submitting) {
      return;
    }
    this.submitting = true;
    this.errorMessage = '';

    this.authService.forgotPassword(this.requestForm.value).subscribe({
      next: (res: any) => {
        this.submitting = false;
        this.step = 'reset';
        if (res.resetToken) {
          // Dev-mode convenience: no SMTP is configured, so the backend returns the
          // reset token directly instead of emailing it. Pre-fill it for the user.
          this.resetForm.patchValue({ token: res.resetToken });
          this.infoMessage = 'No email service is configured for this project, so your reset code has been filled in automatically below.';
        } else {
          this.infoMessage = 'If an account exists for that email, a reset code has been generated. Enter it below.';
        }
      },
      error: (err: HttpErrorResponse) => {
        this.submitting = false;
        this.errorMessage = this.errorHandler.extractMessage(err, 'Could not process your request.');
      }
    });
  }

  submitReset(): void {
    if (this.resetForm.invalid || this.submitting) {
      return;
    }
    this.submitting = true;
    this.errorMessage = '';

    this.authService.resetPassword(this.resetForm.value).subscribe({
      next: () => {
        this.submitting = false;
        this.step = 'done';
        this.notificationService.success('Your password has been reset. Please sign in.');
      },
      error: (err: HttpErrorResponse) => {
        this.submitting = false;
        this.errorMessage = this.errorHandler.extractMessage(err, 'That reset code is invalid or has expired.');
      }
    });
  }

  goToLogin(): void {
    this.router.navigateByUrl('/login');
  }
}
