import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({ selector: 'app-teacher-announcements', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './announcements.component.html', styleUrls: ['./announcements.component.scss'] })
export class AnnouncementsComponent {
  query = '';
  activeFilter = 'All';
  readonly items = [
  {
    "id": 0,
    "title": "Faculty development workshop",
    "body": "Registrations are open for the AI-assisted teaching workshop this Friday.",
    "meta": "Academic office · 35 min ago",
    "category": "Faculty",
    "pinned": true
  },
  {
    "id": 1,
    "title": "Course review completed",
    "body": "Engineering Mathematics III passed its quarterly content review.",
    "meta": "Quality team · 3 hours ago",
    "category": "Courses",
    "pinned": false
  },
  {
    "id": 2,
    "title": "June payout schedule",
    "body": "Teacher royalties will be processed on 30 June. Verify your payout details.",
    "meta": "Finance · Yesterday",
    "category": "Payouts",
    "pinned": false
  }
];
  get filters(): string[] { return ['All', ...Array.from(new Set(this.items.map(item => item.category)))]; }
  get filteredItems() { const q = this.query.toLowerCase(); return this.items.filter(item => (this.activeFilter === 'All' || item.category === this.activeFilter) && (!q || (item.title + item.body).toLowerCase().includes(q))); }
}

