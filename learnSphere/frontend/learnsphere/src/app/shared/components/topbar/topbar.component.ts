import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { NotificationStateService } from '../../../core/services/notification-state.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.scss']
})
export class TopbarComponent {
  isMenuOpen = false;

  constructor(private router: Router, private notificationState: NotificationStateService) {}

  get role(): 'student' | 'teacher' | 'admin' {
    if (this.router.url.startsWith('/teacher')) return 'teacher';
    if (this.router.url.startsWith('/admin')) return 'admin';
    return 'student';
  }

  get userName(): string {
    return { student: 'Raj Shah', teacher: 'Prof. Ajay Sharma', admin: 'Admin User' }[this.role];
  }

  get userEmail(): string {
    return { student: 'raj.shah@mhcollege.edu.in', teacher: 'a.sharma@mhcollege.edu.in', admin: 'admin@learnsphere.in' }[this.role];
  }

  get userInitials(): string {
    return { student: 'RS', teacher: 'AS', admin: 'AD' }[this.role];
  }

  get unreadNotifications(): number {
    return this.notificationState.count(this.role);
  }

  // The topbar is shared by Student, Teacher, and Admin — derive the
  // current role from the URL so the message icon links to the right route.
  getMessagesRoute(): string {
    const url = this.router.url;
    if (url.startsWith('/student')) return '/student/messages';
    if (url.startsWith('/teacher')) return '/teacher/messages';
    if (url.startsWith('/admin')) return '/admin/messages';
    return '/login';
  }

  getRoleRoute(page: 'announcements' | 'notifications' | 'profile' | 'settings'): string {
    return `/${this.role}/${page}`;
  }

  toggleMenu() {
    this.isMenuOpen = !this.isMenuOpen;
  }

  closeMenu() {
    this.isMenuOpen = false;
  }

  signOut() {
    this.closeMenu();
    this.router.navigate(['/login']);
  }
}
