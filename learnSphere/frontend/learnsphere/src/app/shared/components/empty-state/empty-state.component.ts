import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

/**
 * EmptyStateComponent is shown when lists or tables return empty results.
 * Renders a visual indicator, helper text, and an optional call-to-action button.
 */
@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="empty-state">
      <div class="empty-state__icon">
        <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="color: #9CA3AF;"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
      </div>
      <h3 class="empty-state__title">{{ title }}</h3>
      <p class="empty-state__subtitle">{{ subtitle }}</p>
      <button *ngIf="ctaLabel && ctaRoute" [routerLink]="ctaRoute" class="btn btn--primary">
        {{ ctaLabel }}
      </button>
    </div>
  `,
  styles: [`
    @import '../../../../styles/variables';

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 40px 24px;
      text-align: center;
      background: var(--bg-surface);
      border: 1px solid var(--border-color);
      border-radius: var(--border-radius-lg);
      margin: 16px 0;
      width: 100%;

      &__icon {
        font-size: 40px;
        margin-bottom: 16px;
        animation: float 3s ease-in-out infinite;
      }

      &__title {
        font-size: 15px;
        font-weight: 600;
        color: var(--text-primary);
        margin-bottom: 4px;
      }

      &__subtitle {
        font-size: 13px;
        color: var(--text-muted);
        max-width: 320px;
        margin-bottom: 20px;
        line-height: 1.5;
      }
    }

    @keyframes float {
      0% { transform: translateY(0px); }
      50% { transform: translateY(-8px); }
      100% { transform: translateY(0px); }
    }
  `]
})
export class EmptyStateComponent {
  /**
   * Title text (e.g. "No notes yet").
   */
  @Input() title: string = 'No data available';

  /**
   * Subtitle explanatory description.
   */
  @Input() subtitle: string = 'There is currently no information to display here.';

  /**
   * Optional label text for the call-to-action button.
   */
  @Input() ctaLabel?: string;

  /**
   * Optional route target for the call-to-action button.
   */
  @Input() ctaRoute?: string;
}
