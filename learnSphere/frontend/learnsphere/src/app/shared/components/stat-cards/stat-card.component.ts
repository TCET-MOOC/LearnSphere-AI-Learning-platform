import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * StatCardComponent is a reusable KPI component used across student, teacher, and admin dashboards.
 * It displays an icon (emoji mapping), a prominent numeric value, a label, and trend arrows/texts.
 */
@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stat-card.component.html',
  styleUrls: ['./stat-card.component.scss']
})
export class StatCardComponent {
  /**
   * The icon identifier which determines the emoji displayed.
   */
  @Input() icon: string = 'book-open';

  /**
   * Prominent display value (e.g. 7, "84%", "₹18,400").
   */
  @Input() value: string | number = '';

  /**
   * Label text below the value explaining what the metric represents.
   */
  @Input() label: string = '';

  /**
   * Optional trend direction: 'up' or 'down'.
   */
  @Input() trend?: 'up' | 'down';

  /**
   * Optional details accompanying the trend (e.g. "+12 this week", "2 in progress").
   */
  @Input() trendValue?: string;

  /**
   * Maps an icon string name to a corresponding visual emoji symbol.
   */
  getEmoji(): string {
    switch (this.icon) {
      case 'users': return '👥';
      case 'book-open': return '📖';
      case 'shield-exclamation': return '⚠️';
      case 'graduation-cap': return '🎓';
      case 'award': return '🏆';
      case 'dollar-sign': return '💸';
      case 'clock': return '⏱️';
      case 'folder': return '📁';
      default: return '📊';
    }
  }
}
