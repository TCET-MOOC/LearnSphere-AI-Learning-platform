import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.scss']
})
export class TopbarComponent {
  isMenuOpen = false;

  userName = 'Admin User';
  userEmail = 'admin@learnsphere.in';
  userInitials = 'AD';

  constructor(private router: Router) {}

  // The topbar is shared by Student, Teacher, and Admin — derive the
  // current role from the URL so the message icon links to the right route.
  getMessagesRoute(): string {
    const url = this.router.url;
    if (url.startsWith('/student')) return '/student/messages';
    if (url.startsWith('/teacher')) return '/teacher/messages';
    if (url.startsWith('/admin')) return '/admin/messages';
    return '/login';
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