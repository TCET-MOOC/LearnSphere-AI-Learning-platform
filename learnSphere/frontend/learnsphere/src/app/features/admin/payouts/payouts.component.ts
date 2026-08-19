import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { LucideAngularModule } from 'lucide-angular';
import { PayoutService, PendingPayout } from '../services/payout.service';
import { NotificationService } from '@core/services/notification.service';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
import { getInitials } from '@core/utils/avatar.util';

@Component({
  selector: 'app-payouts',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    LucideAngularModule
  ],
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
    private notificationService: NotificationService,
    private dialog: MatDialog
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
    return getInitials(name);
  }

  processAll(): void {
    if (this.pendingPayouts.length === 0 || this.processingAll) {
      return;
    }

    const count = this.pendingPayouts.length;
    const total = this.totalPending;

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Disburse All Pending Payouts',
        message: `Are you sure you want to process ₹${total.toLocaleString()} across all ${count} eligible faculty member(s)?`,
        confirmLabel: 'Process All Payouts',
        cancelLabel: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.processingAll = true;
        this.payoutService.processAll().subscribe({
          next: (res) => {
            this.processingAll = false;
            this.notificationService.success(`Successfully disbursed payouts to ${res.processed} teacher(s).`);
            this.loadPending();
          },
          error: () => {
            this.processingAll = false;
            this.notificationService.error('Failed to process payouts.');
          }
        });
      }
    });
  }

  payTeacher(payout: PendingPayout): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Disburse Teacher Royalty',
        message: `Are you sure you want to transfer ₹${(payout.pendingAmount || 0).toLocaleString()} to ${payout.teacherName}?`,
        confirmLabel: 'Pay Now',
        cancelLabel: 'Cancel'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed) => {
      if (confirmed) {
        this.payingIds.add(payout.teacherId);
        this.payoutService.payTeacher(payout.teacherId).subscribe({
          next: () => {
            this.payingIds.delete(payout.teacherId);
            this.notificationService.success(`Payout of ₹${(payout.pendingAmount || 0).toLocaleString()} to ${payout.teacherName} completed.`);
            this.loadPending();
          },
          error: () => {
            this.payingIds.delete(payout.teacherId);
            this.notificationService.error(`Failed to process payout for ${payout.teacherName}.`);
          }
        });
      }
    });
  }

  isPaying(teacherId: number): boolean {
    return this.payingIds.has(teacherId);
  }
}
