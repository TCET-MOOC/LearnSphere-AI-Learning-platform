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
      <div class="empty-state__icon">🔍</div>
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
      padding: $spacing-xl * 2 $spacing-xl;
      text-align: center;
      background: var(--color-background-primary);
      border: 0.5px solid var(--color-border-tertiary);
      border-radius: $border-radius-lg;
      margin: $spacing-md 0;
      width: 100%;

      &__icon {
        font-size: 40px;
        margin-bottom: $spacing-md;
        animation: float 3s ease-in-out infinite;
      }

      &__title {
        font-size: 14px;
        font-weight: 500;
        color: var(--color-text-primary);
        margin-bottom: $spacing-xs;
      }

      &__subtitle {
        font-size: 12px;
        color: var(--color-text-secondary);
        max-width: 280px;
        margin-bottom: $spacing-lg;
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
