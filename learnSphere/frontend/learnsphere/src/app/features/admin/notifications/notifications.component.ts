import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NotificationStateService } from '../../../core/services/notification-state.service';

@Component({ selector: 'app-admin-notifications', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './notifications.component.html', styleUrls: ['./notifications.component.scss'] })
export class NotificationsComponent {
  query = '';
  activeFilter = 'All';
  readonly items = [
  {
    "id": 0,
    "title": "High-risk content flagged",
    "body": "A discussion post requires immediate moderation review.",
    "meta": "5 min ago",
    "category": "Urgent"
  },
  {
    "id": 1,
    "title": "New college application",
    "body": "Pune Institute of Technology submitted verification documents.",
    "meta": "40 min ago",
    "category": "Colleges"
  },
  {
    "id": 2,
    "title": "7 courses awaiting review",
    "body": "Teacher course submissions are ready for approval.",
    "meta": "2 hours ago",
    "category": "Courses"
  },
  {
    "id": 3,
    "title": "Revenue report available",
    "body": "The weekly revenue and payout summary is ready.",
    "meta": "Yesterday",
    "category": "Reports"
  }
];
  readonly readItems = new Set<number>();
  constructor(private notificationState: NotificationStateService) {}
  get unreadCount(): number { return this.items.length - this.readItems.size; }
  get filters(): string[] { return ['All', 'Unread', ...Array.from(new Set(this.items.map(item => item.category)))]; }
  get filteredItems() { const q = this.query.toLowerCase(); return this.items.filter(item => (this.activeFilter === 'All' || (this.activeFilter === 'Unread' ? !this.readItems.has(item.id) : item.category === this.activeFilter)) && (!q || (item.title + item.body).toLowerCase().includes(q))); }
  markRead(id: number): void { if (!this.readItems.has(id)) { this.readItems.add(id); this.notificationState.markOneRead('admin'); } }
  markAllRead(): void { this.items.forEach(item => this.readItems.add(item.id)); this.notificationState.markAllRead('admin'); }
}

