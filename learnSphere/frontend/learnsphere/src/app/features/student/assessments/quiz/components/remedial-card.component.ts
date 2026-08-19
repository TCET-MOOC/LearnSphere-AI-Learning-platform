import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, GraduationCap } from 'lucide-angular';

/**
 * RemedialCardComponent is a small banner shown while taking (or reviewing)
 * a test flagged isRemedial=true, reminding the student that passing this
 * test (40%+) unlocks a remedial certificate for the course.
 */
@Component({
  selector: 'app-remedial-card',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <div class="remedial-banner" *ngIf="visible">
      <span class="icon" style="display:flex; align-items:center;">
        <lucide-icon name="graduation-cap" style="width:20px; height:20px; color:#7a4a05;"></lucide-icon>
      </span>
      <div class="copy">
        <strong>This is a remedial test.</strong>
        <span>Score at least 40% to unlock a remedial certificate for {{ courseTitle || 'this course' }}.</span>
      </div>
    </div>
  `,
  styles: [`
    .remedial-banner {
      display: flex;
      align-items: center;
      gap: 12px;
      background: #fdf3eb;
      border: 1px solid #f2ddc2;
      border-radius: 10px;
      padding: 12px 16px;
      margin-bottom: 16px;
    }
    .icon { font-size: 20px; }
    .copy { display: flex; flex-direction: column; gap: 2px; }
    .copy strong { font-size: 12.5px; color: #7a4a05; }
    .copy span { font-size: 11.5px; color: #8a6a3c; }
  `]
})
export class RemedialCardComponent {
  @Input() visible = true;
  @Input() courseTitle: string | null | undefined;
}
