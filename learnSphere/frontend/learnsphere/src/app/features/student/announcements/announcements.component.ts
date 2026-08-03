import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({ selector: 'app-student-announcements', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './announcements.component.html', styleUrls: ['./announcements.component.scss'] })
export class AnnouncementsComponent {
  query = '';
  activeFilter = 'All';
  readonly items = [
  {
    "id": 0,
    "title": "End-semester examination schedule published",
    "body": "The examination cell has published the timetable. Review your dates and reporting times.",
    "meta": "Academic office · 20 min ago",
    "category": "Academic",
    "pinned": true
  },
  {
    "id": 1,
    "title": "New course: Introduction to Machine Learning",
    "body": "Enrolment is open for Dr. Amit Kulkarni’s new elective. Seats are limited.",
    "meta": "Course catalog · 2 hours ago",
    "category": "Courses",
    "pinned": false
  },
  {
    "id": 2,
    "title": "Campus maintenance window",
    "body": "LearnSphere may be unavailable on Sunday between 1:00 AM and 3:00 AM.",
    "meta": "Platform team · Yesterday",
    "category": "Platform",
    "pinned": false
  }
];
  get filters(): string[] { return ['All', ...Array.from(new Set(this.items.map(item => item.category)))]; }
  get filteredItems() { const q = this.query.toLowerCase(); return this.items.filter(item => (this.activeFilter === 'All' || item.category === this.activeFilter) && (!q || (item.title + item.body).toLowerCase().includes(q))); }
}

