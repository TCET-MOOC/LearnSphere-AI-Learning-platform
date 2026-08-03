import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NotificationStateService } from '../../../core/services/notification-state.service';

@Component({ selector: 'app-teacher-notifications', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './notifications.component.html', styleUrls: ['./notifications.component.scss'] })
export class NotificationsComponent {
  query = '';
  activeFilter = 'All';
  readonly items = [
  {
    "id": 0,
    "title": "32 new enrolments",
    "body": "Students joined Engineering Mathematics III this week.",
    "meta": "20 min ago",
    "category": "Students"
  },
  {
    "id": 1,
    "title": "New discussion question",
    "body": "Raj Shah asked about inverse Laplace transforms.",
    "meta": "1 hour ago",
    "category": "Discussion"
  },
  {
    "id": 2,
    "title": "Quiz draft generated",
    "body": "AI extracted 18 questions from your latest lecture notes.",
    "meta": "4 hours ago",
    "category": "AI tools"
  },
  {
    "id": 3,
    "title": "Monthly payout processed",
    "body": "Your June payout of ₹18,420 is being processed.",
    "meta": "Yesterday",
    "category": "Payouts"
  }
];
  readonly readItems = new Set<number>();
  constructor(private notificationState: NotificationStateService) {}
  get unreadCount(): number { return this.items.length - this.readItems.size; }
  get filters(): string[] { return ['All', 'Unread', ...Array.from(new Set(this.items.map(item => item.category)))]; }
  get filteredItems() { const q = this.query.toLowerCase(); return this.items.filter(item => (this.activeFilter === 'All' || (this.activeFilter === 'Unread' ? !this.readItems.has(item.id) : item.category === this.activeFilter)) && (!q || (item.title + item.body).toLowerCase().includes(q))); }
  markRead(id: number): void { if (!this.readItems.has(id)) { this.readItems.add(id); this.notificationState.markOneRead('teacher'); } }
  markAllRead(): void { this.items.forEach(item => this.readItems.add(item.id)); this.notificationState.markAllRead('teacher'); }
}

