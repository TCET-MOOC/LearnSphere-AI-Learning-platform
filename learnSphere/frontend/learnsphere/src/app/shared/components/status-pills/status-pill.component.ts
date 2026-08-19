import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * StatusPillComponent is a badge/pill component that maps a status input string 
 * to a standardized visual state (color, background, and readable text label).
 * Used throughout discussions, dashboards, and tables.
 */
@Component({
  selector: 'app-status-pill',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="pill" [ngClass]="getPillClass()">
      {{ getLabel() }}
    </span>
  `,
  styles: [`
    @import '../../../../styles/variables';

    .pill {
      display: inline-flex;
      align-items: center;
      padding: 3px 8px;
      border-radius: 20px;
      font-size: 11px;
      font-weight: 600;
      white-space: nowrap;
      line-height: 1.4;
      text-transform: capitalize;

      &--green {
        background-color: var(--status-green-bg);
        color: var(--status-green-text);
      }
      &--amber {
        background-color: var(--status-amber-bg);
        color: var(--status-amber-text);
      }
      &--red {
        background-color: var(--status-red-bg);
        color: var(--status-red-text);
      }
      &--purple {
        background-color: var(--status-purple-bg);
        color: var(--status-purple-text);
      }
      &--grey {
        background-color: var(--status-grey-bg);
        color: var(--status-grey-text);
      }
      &--teal {
        background-color: var(--status-green-bg);
        color: var(--status-green-text);
      }
      &--blue {
        background-color: var(--status-blue-bg);
        color: var(--status-blue-text);
      }
    }
  `]
})
export class StatusPillComponent {
  /**
   * The raw status input string (e.g. 'LIVE', 'DRAFT', 'REMEDIAL', 'URGENT').
   */
  @Input() status: string = '';

  /**
   * Maps the status to a CSS class for color coding.
   */
  getPillClass(): string {
    if (!this.status) return 'pill--grey';
    const s = this.status.toUpperCase();
    if (s === 'LIVE' || s === 'ACTIVE' || s === 'POSITIVE' || s === 'VERIFIED') return 'pill--green';
    if (s === 'DRAFT' || s === 'UPCOMING' || s === 'WARNING' || s === 'SOON') return 'pill--amber';
    if (s === 'URGENT' || s === 'FLAGGED' || s === 'HIGH_RISK' || s === 'REJECTED') return 'pill--red';
    if (s === 'IN_PROGRESS' || s === 'YOUR_ITEM' || s === 'REMEDIAL' || s === 'TEACHER') return 'pill--purple';
    if (s === 'TEAL' || s === 'NEW') return 'pill--teal';
    if (s === 'BLUE' || s === 'ENROLL FREE' || s === 'MANAGED') return 'pill--blue';
    return 'pill--grey';
  }

  /**
   * Translates internal status values to user-friendly display text.
   */
  getLabel(): string {
    if (!this.status) return '';
    const s = this.status.toLowerCase();
    if (s === 'live' || s === 'active') return s;
    if (s === 'positive') return 'positive';
    if (s === 'draft') return 'draft';
    if (s === 'upcoming' || s === 'soon') return 'upcoming';
    if (s === 'warning') return 'warning';
    if (s === 'urgent') return 'urgent';
    if (s === 'flagged') return 'flagged';
    if (s === 'high_risk') return 'high risk';
    if (s === 'in_progress') return 'in progress';
    if (s === 'your_item') return 'your item';
    if (s === 'remedial') return 'remedial';
    return this.status;
  }
}
