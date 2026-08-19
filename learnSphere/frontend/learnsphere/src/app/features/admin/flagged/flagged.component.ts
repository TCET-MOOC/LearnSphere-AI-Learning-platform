import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ModerationService, FlaggedContentItem } from '../services/moderation.service';
import { NotificationService } from '@core/services/notification.service';
import { timeAgo } from '@core/utils/time.util';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-flagged',
  standalone: true,
  imports: [
    CommonModule,
    LucideAngularModule
  ],
  templateUrl: './flagged.component.html',
  styleUrls: ['./flagged.component.scss']
})
export class FlaggedComponent implements OnInit {
  activeTab = 'all';
  allItems: FlaggedContentItem[] = [];
  items: FlaggedContentItem[] = [];
  loading = false;
  actingId: number | null = null;

  tabs: { id: string; label: string }[] = [
    { id: 'all', label: 'All' },
    { id: 'high-risk', label: 'High risk' },
    { id: 'bullying', label: 'Bullying' },
    { id: 'spam', label: 'Spam' },
    { id: 'suspicious', label: 'Suspicious' }
  ];

  constructor(
    private moderationService: ModerationService,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.moderationService.getFlagged().subscribe({
      next: (items) => {
        this.allItems = items;
        this.buildTabCounts();
        this.applyTab();
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.notificationService.error('Failed to load flagged content.');
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private buildTabCounts(): void {
    const count = (pred: (i: FlaggedContentItem) => boolean) => this.allItems.filter(pred).length;
    this.tabs = [
      { id: 'all', label: `All (${this.allItems.length})` },
      { id: 'high-risk', label: `High risk (${count((i) => i.reason === 'HIGH_RISK')})` },
      { id: 'bullying', label: `Bullying (${count((i) => i.reason === 'BULLYING')})` },
      { id: 'spam', label: `Spam (${count((i) => i.reason === 'SPAM')})` },
      { id: 'suspicious', label: `Suspicious (${count((i) => i.reason === 'SUSPICIOUS')})` }
    ];
  }

  setTab(id: string): void {
    this.activeTab = id;
    this.applyTab();
    this.cdr.markForCheck();
  }

  private applyTab(): void {
    this.items =
      this.activeTab === 'all'
        ? this.allItems
        : this.allItems.filter((i) => i.reason.toLowerCase().replace('_', '-') === this.activeTab);
  }

  get openCount(): number {
    return this.allItems.length;
  }

  get highRiskCount(): number {
    return this.allItems.filter((i) => i.severity === 'high').length;
  }

  get bullyingCount(): number {
    return this.allItems.filter((i) => i.reason === 'BULLYING').length;
  }

  get spamCount(): number {
    return this.allItems.filter((i) => i.reason === 'SPAM').length;
  }

  timeAgo(iso: string): string {
    return timeAgo(iso);
  }

  resolve(item: FlaggedContentItem): void {
    this.actingId = item.id;
    this.cdr.markForCheck();
    this.moderationService.resolve(item.id).subscribe({
      next: () => {
        this.actingId = null;
        this.notificationService.success('Marked as resolved.');
        this.load();
      },
      error: () => {
        this.actingId = null;
        this.notificationService.error('Failed to resolve item.');
        this.cdr.markForCheck();
      }
    });
  }

  dismiss(item: FlaggedContentItem): void {
    this.actingId = item.id;
    this.cdr.markForCheck();
    this.moderationService.dismiss(item.id).subscribe({
      next: () => {
        this.actingId = null;
        this.notificationService.success('Dismissed false flag.');
        this.load();
      },
      error: () => {
        this.actingId = null;
        this.notificationService.error('Failed to dismiss item.');
        this.cdr.markForCheck();
      }
    });
  }
}
