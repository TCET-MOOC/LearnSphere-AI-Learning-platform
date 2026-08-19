import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SidebarService } from '../../../core/services/sidebar.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-teacher-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './teacher-sidebar.component.html',
  styleUrls: ['../sidebar/sidebar.component.scss'] // Reusing the exact same SCSS as the student sidebar!
})
export class TeacherSidebarComponent {
  isCollapsed$: Observable<boolean>;
  isMobileOpen$: Observable<boolean>;

  constructor(private sidebarService: SidebarService) {
    this.isCollapsed$ = this.sidebarService.isCollapsed$;
    this.isMobileOpen$ = this.sidebarService.isMobileOpen$;
  }

  toggleSidebar(): void {
    this.sidebarService.toggleSidebar();
  }

  closeMobile(): void {
    this.sidebarService.closeMobile();
  }

  navItems = [
    { label: 'Dashboard', icon: 'dashboard', route: '/teacher/dashboard' },
    { label: 'My courses', icon: 'courses', route: '/teacher/courses' },
    { label: 'Upload content', icon: 'upload', route: '/teacher/upload' },
    { label: 'Student standings', icon: 'standings', route: '/teacher/students' },
    { label: 'Messages', icon: 'messages', route: '/teacher/messages' },
    { label: 'Discussion', icon: 'discussion', route: '/teacher/discuss' },
    { label: 'Live sessions', icon: 'live', route: '/teacher/live' },
    { label: 'Trending', icon: 'trending', route: '/teacher/trending' },
    { label: 'Royalties', icon: 'royalties', route: '/teacher/royalties' }
  ];
}