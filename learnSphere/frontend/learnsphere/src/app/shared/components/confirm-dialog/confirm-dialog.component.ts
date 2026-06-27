import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
}

/**
 * ConfirmDialogComponent is a Material dialog confirmation overlay.
 * Used before executing consequential or destructive actions (such as deletion, rejection, or blacklisting).
 */
@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule],
  template: `
    <div class="confirm-dialog">
      <h2 mat-dialog-title class="confirm-dialog__title">{{ data.title }}</h2>
      <div mat-dialog-content class="confirm-dialog__content">
        <p>{{ data.message }}</p>
      </div>
      <div mat-dialog-actions class="confirm-dialog__actions">
        <button class="btn btn--ghost" (click)="onCancel()">
          {{ data.cancelLabel || 'Cancel' }}
        </button>
        <button class="btn btn--danger" (click)="onConfirm()">
          {{ data.confirmLabel || 'Confirm' }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    @import '../../../../styles/variables';

    .confirm-dialog {
      padding: $spacing-md;
      background: var(--color-background-primary);
      border-radius: $border-radius-lg;
      max-width: 400px;

      &__title {
        font-size: 14px;
        font-weight: 500;
        color: var(--color-text-primary);
        margin: 0 0 $spacing-sm;
      }

      &__content {
        font-size: 12px;
        color: var(--color-text-secondary);
        margin-bottom: $spacing-lg;
        line-height: 1.5;
      }

      &__actions {
        display: flex;
        justify-content: flex-end;
        gap: $spacing-sm;
      }
    }
  `]
})
export class ConfirmDialogComponent {
  constructor(
    /**
     * MatDialogRef allows closing this specific dialog and passing values back to the opening parent component.
     */
    public dialogRef: MatDialogRef<ConfirmDialogComponent>,

    /**
     * Injects the dynamic configurations (title, message, labels) passed from the parent component.
     */
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData
  ) {}

  onCancel(): void {
    this.dialogRef.close(false);
  }

  onConfirm(): void {
    this.dialogRef.close(true);
  }
}
