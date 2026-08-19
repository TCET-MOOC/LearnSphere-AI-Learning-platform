import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { NotificationStateService } from '../../../core/services/notification-state.service';
import { NotificationApiService } from '@core/services/notification-api.service';
import { NotificationService } from '@core/services/notification.service';
import { timeAgo } from '@core/utils/time.util';

interface NotificationItem {
  id: number;
  title: string;
  body: string;
  meta: string;
  category: string;
}

@Component({ 
  selector: 'app-admin-notifications', 
  standalone: true, 
  imports: [CommonModule, FormsModule, LucideAngularModule], 
  templateUrl: './notifications.component.html', 
  styleUrls: ['./notifications.component.scss'] 
})
export class NotificationsComponent implements OnInit {
  query = '';
  activeFilter = 'All';
  items: NotificationItem[] = [];
  readonly readItems = new Set<number>();

  constructor(
    private notificationState: NotificationStateService,
    private notificationApi: NotificationApiService,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.notificationApi.getNotifications().subscribe({
      next: (dtos) => {
        this.items = dtos.map((dto) => ({
          id: dto.id,
          title: dto.title,
          body: dto.body,
          meta: timeAgo(dto.createdAt),
          category: dto.category || 'General'
        }));
        this.readItems.clear();
        dtos.filter((dto) => dto.read).forEach((dto) => this.readItems.add(dto.id));
        this.notificationState.setCount('admin', this.unreadCount);
      },
      error: () => this.notify.error('Could not load notifications.')
    });
  }

  get unreadCount(): number { return this.items.length - this.readItems.size; }
  get filters(): string[] { return ['All', 'Unread', ...Array.from(new Set(this.items.map(item => item.category)))]; }
  get filteredItems() { 
    const q = this.query.toLowerCase(); 
    return this.items.filter(item => 
      (this.activeFilter === 'All' || (this.activeFilter === 'Unread' ? !this.readItems.has(item.id) : item.category === this.activeFilter)) && 
      (!q || (item.title + ' ' + item.body).toLowerCase().includes(q))
    ); 
  }

  markRead(id: number): void {
    if (this.readItems.has(id)) return;
    this.readItems.add(id);
    this.notificationState.markOneRead('admin');
    this.notificationApi.markRead(id).subscribe({ error: () => this.notify.error('Could not mark notification read.') });
  }

  markAllRead(): void {
    this.items.forEach(item => this.readItems.add(item.id));
    this.notificationState.markAllRead('admin');
    this.notificationApi.markAllRead().subscribe({ error: () => this.notify.error('Could not mark notifications read.') });
  }
}
