import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * ProgressBarComponent is a horizontal completion indicator.
 * Displays progress out of 100 and can render optional textual details (e.g. "17/25 lectures").
 */
@Component({
  selector: 'app-progress-bar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="progress-bar-container">
      <div class="progress-bar-track">
        <div class="progress-bar-fill" 
             [style.width.%]="value" 
             [ngClass]="color === 'green' ? 'progress-bar-fill--green' : 'progress-bar-fill--purple'">
        </div>
      </div>
      <span class="progress-bar-label" *ngIf="label">{{ label }}</span>
    </div>
  `,
  styles: [`
    @import '../../../../styles/variables';

    .progress-bar-container {
      display: flex;
      align-items: center;
      gap: $spacing-sm;
      width: 100%;
    }

    .progress-bar-track {
      flex: 1;
      height: 6px;
      background-color: var(--color-border-tertiary);
      border-radius: $border-radius-sm;
      overflow: hidden;
    }

    .progress-bar-fill {
      height: 100%;
      border-radius: $border-radius-sm;
      transition: width 0.3s ease;

      &--purple {
        background-color: $brand-purple;
      }

      &--green {
        background-color: $status-green-text;
      }
    }

    .progress-bar-label {
      font-size: 11px;
      color: var(--color-text-secondary);
      white-space: nowrap;
    }
  `]
})
export class ProgressBarComponent {
  /**
   * The progress value (0 to 100).
   */
  @Input() value: number = 0;

  /**
   * Optional string descriptor (e.g., "Lec 8/20").
   */
  @Input() label?: string;

  /**
   * Determines the filled color theme. Defaults to 'purple', 'green' maps to green.
   */
  @Input() color: 'purple' | 'green' = 'purple';
}
