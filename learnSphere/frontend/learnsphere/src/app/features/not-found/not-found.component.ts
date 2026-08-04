import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="not-found">
      <h1>404</h1>
      <p>This page doesn't exist.</p>
      <a [routerLink]="homeLink()">Back to safety</a>
    </div>
  `,
  styles: [`
    .not-found {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 12px;
      font-family: 'Inter', sans-serif;
      color: #1A1830;
    }
    h1 { font-size: 64px; margin: 0; color: #534AB7; }
    p { color: #6B6880; margin: 0; }
    a { color: #534AB7; font-weight: 600; text-decoration: none; margin-top: 8px; }
  `]
})
export class NotFoundComponent {
  constructor(private authService: AuthService) {}

  homeLink(): string {
    if (!this.authService.isAuthenticated) {
      return '/';
    }
    return this.authService.dashboardPathForRole(this.authService.currentUser?.role);
  }
}
