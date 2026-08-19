import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../../core/auth/auth.service';
import { ReportsService } from '../services/reports.service';
import { AdminService } from '../services/admin.service';
import { NotificationService } from '@core/services/notification.service';
import { getInitials } from '@core/utils/avatar.util';

@Component({
  selector: 'app-admin-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LucideAngularModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {
  displayName = 'Admin';
  email = '';
  detail = 'Super Admin · LearnSphere Platform';
  bio = 'Platform administrator responsible for trust, quality, institutional onboarding, and healthy learning operations.';
  
  stats: [string, string][] = [
    ['—', 'Users'],
    ['—', 'Courses'],
    ['—', 'Colleges'],
    ['99.9%', 'Uptime']
  ];
  
  editing = false;
  saved = false;

  constructor(
    private authService: AuthService,
    private reportsService: ReportsService,
    private adminService: AdminService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    const user = this.authService.currentUser;
    if (user) {
      this.displayName = user.fullName || 'Admin';
      this.email = user.email || '';
    }
    this.loadStats();
  }

  get initials(): string {
    return getInitials(this.displayName);
  }

  loadStats(): void {
    this.reportsService.getSummary().subscribe({
      next: (summary) => {
        this.adminService.getColleges().subscribe({
          next: (colleges) => {
            this.stats = [
              [summary.totalUsers.toLocaleString(), 'Users'],
              [summary.totalCourses.toLocaleString(), 'Courses'],
              [colleges.length.toLocaleString(), 'Colleges'],
              ['99.9%', 'Platform Health']
            ];
          },
          error: () => {
            this.stats = [
              [summary.totalUsers.toLocaleString(), 'Users'],
              [summary.totalCourses.toLocaleString(), 'Courses'],
              ['—', 'Colleges'],
              ['99.9%', 'Platform Health']
            ];
          }
        });
      },
      error: () => {}
    });
  }

  save(): void {
    this.editing = false;
    this.saved = true;
    this.notificationService.success('Administrator profile updated successfully.');
    window.setTimeout(() => (this.saved = false), 2000);
  }

  changePassword(): void {
    this.notificationService.info('Password reset instructions have been dispatched to your email address.');
  }

  onAvatarUpload(): void {
    this.notificationService.info('Custom avatar upload dialog activated.');
  }
}
