import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '@core/auth/auth.service';
import { ErrorHandlerService } from '@core/services/error-handler.service';
import { NotificationService } from '@core/services/notification.service';
import { passwordValidator } from '@core/validators/password.validator';
import { safeReturnUrl } from '@core/auth/session';
import { SESSION_COPY } from '@core/content/session.content';
import { LOGIN_COPY } from '@core/content/login.content';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  protected readonly copy = LOGIN_COPY;
  loginForm: FormGroup;
  submitting = false;
  errorMessage = '';
  /** Shown after the app signed the user out because the session could not be renewed (W7-04). */
  readonly sessionNotice: string | null;
  private readonly returnUrl: string | null;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private errorHandler: ErrorHandlerService,
    private notificationService: NotificationService,
    route: ActivatedRoute
  ) {
    const query = route.snapshot.queryParamMap;
    this.returnUrl = safeReturnUrl(query.get('redirectTo'));
    this.sessionNotice = query.get('reason') === 'expired' ? SESSION_COPY.expiredNotice : null;
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, passwordValidator()]]
    });
  }

  get passwordControl() {
    return this.loginForm.get('password');
  }

  onSubmit(): void {
    if (this.loginForm.invalid || this.submitting) {
      return;
    }

    this.submitting = true;
    this.errorMessage = '';

    this.authService.login(this.loginForm.value).subscribe({
      next: (res) => {
        this.submitting = false;
        this.notificationService.success(`Welcome back, ${res.user.fullName.split(' ')[0]}!`);
        // W7-04: back to the page the user was sent away from, else their dashboard.
        this.router.navigateByUrl(this.returnUrl ?? this.authService.dashboardPathForRole(res.user.role));
      },
      error: (err: HttpErrorResponse) => {
        this.submitting = false;
        this.errorMessage = this.errorHandler.extractMessage(err, LOGIN_COPY.failed);
      }
    });
  }
}
