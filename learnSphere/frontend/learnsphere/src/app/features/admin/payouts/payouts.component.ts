import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PayoutService, PendingPayout } from '../services/payout.service';
import { NotificationService } from '@core/services/notification.service';

@Component({
  selector: 'app-payouts',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payouts.component.html',
  styleUrls: ['./payouts.component.scss']
})
export class PayoutsComponent implements OnInit {
  pendingPayouts: PendingPayout[] = [];
  loading = true;
  processingAll = false;
  payingIds = new Set<number>();

  constructor(
    private payoutService: PayoutService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadPending();
  }

  loadPending(): void {
    this.loading = true;
    this.payoutService.getPendingPayouts().subscribe({
      next: (rows) => {
        this.pendingPayouts = rows;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.notificationService.error('Failed to load pending payouts.');
      }
    });
  }

  get totalPending(): number {
    return this.pendingPayouts.reduce((sum, p) => sum + (p.pendingAmount || 0), 0);
  }

  initials(name: string): string {
    return name
      .split(' ')
      .map((p) => p.charAt(0))
      .join('')
      .substring(0, 2)
      .toUpperCase();
  }

  processAll(): void {
    if (this.pendingPayouts.length === 0 || this.processingAll) {
      return;
    }
    this.processingAll = true;
    this.payoutService.processAll().subscribe({
      next: (res) => {
        this.processingAll = false;
        this.notificationService.success(`Processed payouts for ${res.processed} teacher(s).`);
        this.loadPending();
      },
      error: () => {
        this.processingAll = false;
        this.notificationService.error('Failed to process payouts.');
      }
    });
  }

  payTeacher(teacherId: number): void {
    this.payingIds.add(teacherId);
    this.payoutService.payTeacher(teacherId).subscribe({
      next: () => {
        this.payingIds.delete(teacherId);
        this.notificationService.success('Payout processed.');
        this.loadPending();
      },
      error: () => {
        this.payingIds.delete(teacherId);
        this.notificationService.error('Failed to process this payout.');
      }
    });
  }

  isPaying(teacherId: number): boolean {
    return this.payingIds.has(teacherId);
  }
}
