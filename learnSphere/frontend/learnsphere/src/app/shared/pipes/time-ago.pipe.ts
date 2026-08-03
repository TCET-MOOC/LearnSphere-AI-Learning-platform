import { Pipe, PipeTransform } from '@angular/core';

/**
 * @Pipe decorator defines a custom formatting pipe.
 * standalone: true makes the pipe direct-importable in other components without requiring a module declaration.
 */
@Pipe({
  name: 'timeAgo',
  standalone: true
})
export class TimeAgoPipe implements PipeTransform {
  /**
   * Transforms an ISO string date or Date object into a relative string representation (e.g., "5m ago").
   */
  transform(value: string | Date | null | undefined): string {
    if (!value) return '';
    const date = typeof value === 'string' ? new Date(value) : value;
    const now = new Date();
    const seconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (seconds < 60) return 'Just now';
    
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}m ago`;
    
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours}h ago`;
    
    const days = Math.floor(hours / 24);
    if (days === 1) return 'Yesterday';
    if (days < 7) return `${days}d ago`;
    
    // Return standard formatted date string if older than 7 days
    return date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
  }
}
