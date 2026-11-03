import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '@core/auth/auth.service';
import { ErrorHandlerService } from '@core/services/error-handler.service';
import { NotificationService } from '@core/services/notification.service';
import { passwordValidator } from '@core/validators/password.validator';
import { REGISTER_COPY } from '@core/content/register.content';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  protected readonly copy = REGISTER_COPY;
  registerForm: FormGroup;
  selectedRole: 'STUDENT' | 'TEACHER' | 'ADMIN' = 'STUDENT'; // Default role
  submitting = false;
  errorMessage = '';

  roles = [
    { value: 'STUDENT', label: REGISTER_COPY.roles.STUDENT },
    { value: 'TEACHER', label: REGISTER_COPY.roles.TEACHER },
    { value: 'ADMIN', label: REGISTER_COPY.roles.ADMIN }
  ];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private errorHandler: ErrorHandlerService,
    private notificationService: NotificationService
  ) {
    this.registerForm = this.fb.group({
      fullName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, passwordValidator()]]
    });
  }

  get passwordControl() {
    return this.registerForm.get('password');
  }

  // Method to update the role when a user clicks a role button
  setRole(roleStr: string) {
    this.selectedRole = roleStr as 'STUDENT' | 'TEACHER' | 'ADMIN';
  }

  onSubmit(): void {
    if (this.registerForm.invalid || this.submitting) {
      return;
    }

    this.submitting = true;
    this.errorMessage = '';

    const payload = {
      role: this.selectedRole,
      ...this.registerForm.value
    };

    this.authService.register(payload).subscribe({
      next: (res) => {
        this.submitting = false;
        this.notificationService.success(REGISTER_COPY.created);
        if (this.selectedRole === 'TEACHER') {
          this.router.navigateByUrl('/verify-college');
        } else {
          this.router.navigateByUrl(this.authService.dashboardPathForRole(res.user.role));
        }
      },
      error: (err: HttpErrorResponse) => {
        this.submitting = false;
        this.errorMessage = this.errorHandler.extractMessage(err, REGISTER_COPY.failed);
      }
    });
  }
}
