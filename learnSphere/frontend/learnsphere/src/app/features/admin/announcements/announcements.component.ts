import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
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

@Component({ 
  selector: 'app-admin-announcements', 
  standalone: true, 
  imports: [
    CommonModule, 
    FormsModule, 
    ReactiveFormsModule,
    LucideAngularModule
  ], 
  templateUrl: './announcements.component.html', 
  styleUrls: ['./announcements.component.scss'] 
})
export class AnnouncementsComponent implements OnInit {
  query = '';
  activeFilter = 'All';
  items: AnnouncementItem[] = [];
  loading = false;
  showCreateModal = false;
  creating = false;
  announcementForm!: FormGroup;

  constructor(
    private announcementService: AnnouncementService, 
    private notify: NotificationService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.load();
  }

  initForm(): void {
    this.announcementForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(4)]],
      body: ['', [Validators.required, Validators.minLength(10)]],
      category: ['General', Validators.required],
      pinned: [false]
    });
  }

  load(): void {
    this.loading = true;
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
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.notify.error('Could not load announcements.');
      }
    });
  }

  get filters(): string[] { 
    return ['All', ...Array.from(new Set(this.items.map(item => item.category)))]; 
  }

  get pinnedCount(): number {
    return this.items.filter(i => i.pinned).length;
  }

  get filteredItems(): AnnouncementItem[] { 
    const q = this.query.toLowerCase(); 
    return this.items.filter(item => 
      (this.activeFilter === 'All' || item.category === this.activeFilter) && 
      (!q || (item.title + ' ' + item.body).toLowerCase().includes(q))
    ); 
  }

  openCreateModal(): void {
    this.announcementForm.reset({
      category: 'General',
      pinned: false
    });
    this.showCreateModal = true;
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
  }

  submitAnnouncement(): void {
    if (this.announcementForm.invalid) return;

    this.creating = true;
    const val = this.announcementForm.value;
    this.announcementService.createAnnouncement({
      title: val.title,
      body: val.body,
      category: val.category,
      pinned: val.pinned,
      audience: 'ALL'
    }).subscribe({
      next: () => {
        this.creating = false;
        this.showCreateModal = false;
        this.notify.success('Broadcast announcement published successfully.');
        this.load();
      },
      error: () => {
        this.creating = false;
        this.notify.error('Failed to publish announcement.');
      }
    });
  }
}
