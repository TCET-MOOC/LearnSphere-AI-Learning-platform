import { Pipe, PipeTransform } from '@angular/core';

/**
 * DurationPipe converts a positive integer representing total seconds 
 * into formatted time text (e.g., 125 -> "2:05", 3665 -> "1:01:05").
 */
@Pipe({
  name: 'duration',
  standalone: true
})
export class DurationPipe implements PipeTransform {
  /**
   * Transforms input seconds to formatted mm:ss or hh:mm:ss string representation.
   */
  transform(value: number | null | undefined): string {
    if (value === null || value === undefined || isNaN(value)) return '00:00';
    
    const hrs = Math.floor(value / 3600);
    const mins = Math.floor((value % 3600) / 60);
    const secs = Math.floor(value % 60);

    // Padding helper to ensure double digits (e.g. "05" instead of "5")
    const pad = (num: number) => num.toString().padStart(2, '0');

    if (hrs > 0) {
      return `${hrs}:${pad(mins)}:${pad(secs)}`;
    }
    return `${mins}:${pad(secs)}`;
  }
}
