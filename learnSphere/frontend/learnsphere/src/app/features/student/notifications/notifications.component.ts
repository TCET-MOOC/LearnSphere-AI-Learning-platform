import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NotificationStateService } from '../../../core/services/notification-state.service';

@Component({ selector: 'app-student-notifications', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './notifications.component.html', styleUrls: ['./notifications.component.scss'] })
export class NotificationsComponent {
  query = '';
  activeFilter = 'All';
  readonly items = [
  {
    "id": 0,
    "title": "Lecture 18 is now available",
    "body": "Dr. Priya Nair uploaded Dynamic Programming in Data Structures & Algorithms.",
    "meta": "10 min ago",
    "category": "Course"
  },
  {
    "id": 1,
    "title": "Assessment due tomorrow",
    "body": "Engineering Mathematics III closes tomorrow at 5:00 PM.",
    "meta": "1 hour ago",
    "category": "Assessment"
  },
  {
    "id": 2,
    "title": "You moved up 4 places",
    "body": "You are now #12 on your college leaderboard.",
    "meta": "3 hours ago",
    "category": "Progress"
  },
  {
    "id": 3,
    "title": "Your question has a reply",
    "body": "Prof. Sharma replied in the Mathematics discussion.",
    "meta": "Yesterday",
    "category": "Discussion"
  }
];
  readonly readItems = new Set<number>();
  constructor(private notificationState: NotificationStateService) {}
  get unreadCount(): number { return this.items.length - this.readItems.size; }
  get filters(): string[] { return ['All', 'Unread', ...Array.from(new Set(this.items.map(item => item.category)))]; }
  get filteredItems() { const q = this.query.toLowerCase(); return this.items.filter(item => (this.activeFilter === 'All' || (this.activeFilter === 'Unread' ? !this.readItems.has(item.id) : item.category === this.activeFilter)) && (!q || (item.title + item.body).toLowerCase().includes(q))); }
  markRead(id: number): void { if (!this.readItems.has(id)) { this.readItems.add(id); this.notificationState.markOneRead('student'); } }
  markAllRead(): void { this.items.forEach(item => this.readItems.add(item.id)); this.notificationState.markAllRead('student'); }
}

