import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-teacher-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './teacher-sidebar.component.html',
  styleUrls: ['../sidebar/sidebar.component.scss'] // Reusing the exact same SCSS as the student sidebar!
})
export class TeacherSidebarComponent {
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