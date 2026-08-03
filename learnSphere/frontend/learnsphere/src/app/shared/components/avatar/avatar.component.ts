import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * AvatarComponent displays the user profile picture or initials with colored background hashing.
 * Optionally includes a green online status dot at the bottom right corner.
 */
@Component({
  selector: 'app-avatar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="avatar-container" [style.background]="getBackground()" [style.color]="getColor()">
      <ng-container *ngIf="user?.avatarUrl; else initialsTemplate">
        <img [src]="user?.avatarUrl" alt="avatar" class="avatar-img" />
      </ng-container>
      <ng-template #initialsTemplate>
        <span class="avatar-initials">{{ getInitials() }}</span>
      </ng-template>
      <div class="avatar-status-dot" *ngIf="isOnline"></div>
    </div>
  `,
  styles: [`
    @import '../../../../styles/variables';

    .avatar-container {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 11px;
      font-weight: 500;
      position: relative;
      user-select: none;
      flex-shrink: 0;
    }

    .avatar-img {
      width: 100%;
      height: 100%;
      border-radius: 50%;
      object-fit: cover;
    }

    .avatar-status-dot {
      position: absolute;
      bottom: 0;
      right: 0;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background-color: $status-green-text;
      border: 1.5px solid var(--color-background-primary);
    }
  `]
})
export class AvatarComponent {
  /**
   * The user object containing name and optional profile image URL.
   */
  @Input() user?: { name: string; avatarUrl?: string };

  /**
   * Shows an online status indicator dot when true.
   */
  @Input() isOnline: boolean = false;

  /**
   * Extracts double letter initials from the user's name.
   */
  getInitials(): string {
    if (!this.user || !this.user.name) return '??';
    const words = this.user.name.split(' ');
    if (words.length > 1) {
      return (words[0][0] + words[1][0]).toUpperCase();
    }
    return this.user.name.substring(0, 2).toUpperCase();
  }

  /**
   * Deterministic background color choice based on initials to maintain color consistency.
   */
  getBackground(): string {
    const initials = this.getInitials();
    const charCodeSum = initials.charCodeAt(0) + (initials.charCodeAt(1) || 0);
    const colors = ['#EEEDFE', '#EAF3DE', '#FAEEDA', '#FCEBEB', '#E1F5EE', '#E6F1FB'];
    return colors[charCodeSum % colors.length];
  }

  /**
   * Matching foreground text color based on background choice.
   */
  getColor(): string {
    const initials = this.getInitials();
    const charCodeSum = initials.charCodeAt(0) + (initials.charCodeAt(1) || 0);
    const colors = ['#3C3489', '#27500A', '#633806', '#791F1F', '#085041', '#0C447C'];
    return colors[charCodeSum % colors.length];
  }
}
