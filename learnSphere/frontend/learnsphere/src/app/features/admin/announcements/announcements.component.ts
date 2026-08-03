import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({ selector: 'app-admin-announcements', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './announcements.component.html', styleUrls: ['./announcements.component.scss'] })
export class AnnouncementsComponent {
  query = '';
  activeFilter = 'All';
  readonly items = [
  {
    "id": 0,
    "title": "Quarterly platform review",
    "body": "The Q2 operations review is scheduled for Monday at 11:00 AM.",
    "meta": "Operations · 15 min ago",
    "category": "Internal",
    "pinned": true
  },
  {
    "id": 1,
    "title": "Updated moderation policy",
    "body": "New AI-assisted moderation thresholds take effect on 1 July.",
    "meta": "Trust & safety · 2 hours ago",
    "category": "Policy",
    "pinned": false
  },
  {
    "id": 2,
    "title": "Three colleges approved",
    "body": "Institution verification is complete for three new partner colleges.",
    "meta": "Partnerships · Yesterday",
    "category": "Colleges",
    "pinned": false
  }
];
  get filters(): string[] { return ['All', ...Array.from(new Set(this.items.map(item => item.category)))]; }
  get filteredItems() { const q = this.query.toLowerCase(); return this.items.filter(item => (this.activeFilter === 'All' || item.category === this.activeFilter) && (!q || (item.title + item.body).toLowerCase().includes(q))); }
}

