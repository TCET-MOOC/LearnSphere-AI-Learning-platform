import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, Users, BookOpen, ShieldAlert, GraduationCap, Award, DollarSign, Clock, Folder, BarChart } from 'lucide-angular';

/**
 * StatCardComponent is a reusable KPI component used across student, teacher, and admin dashboards.
 * It displays an icon, a prominent numeric value, a label, and trend arrows/texts.
 */
@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [
    CommonModule,
    LucideAngularModule
  ],
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
   * Returns a valid lucide-angular icon name.
   */
  getIconName(): string {
    switch (this.icon) {
      case 'shield-exclamation': return 'shield-alert';
      default: return this.icon || 'bar-chart';
    }
  }
}
