import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AnnouncementService } from '@core/services/announcement.service';
import { NotificationService } from '@core/services/notification.service';
import { timeAgo } from '@core/utils/time.util';

interface AnnouncementItem {
  id: number;
  title: string;
  body: string;
  meta: string;
  category: string;
  pinned: boolean;
}

@Component({ selector: 'app-teacher-announcements', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './announcements.component.html', styleUrls: ['./announcements.component.scss'] })
export class AnnouncementsComponent implements OnInit {
  query = '';
  activeFilter = 'All';
  items: AnnouncementItem[] = [];

  constructor(private announcementService: AnnouncementService, private notify: NotificationService) {}

  ngOnInit(): void {
    this.announcementService.getAnnouncements().subscribe({
      next: (dtos) => {
        this.items = dtos.map((dto) => ({
          id: dto.id,
          title: dto.title,
          body: dto.body,
          meta: `${dto.authorName || 'LearnSphere'} · ${timeAgo(dto.createdAt)}`,
          category: dto.category || 'General',
          pinned: dto.pinned
        }));
      },
      error: () => this.notify.error('Could not load announcements.')
    });
  }

  get filters(): string[] { return ['All', ...Array.from(new Set(this.items.map(item => item.category)))]; }
  get filteredItems() { const q = this.query.toLowerCase(); return this.items.filter(item => (this.activeFilter === 'All' || item.category === this.activeFilter) && (!q || (item.title + item.body).toLowerCase().includes(q))); }
}
