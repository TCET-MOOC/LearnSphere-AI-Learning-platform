import { Component, ElementRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { environment } from '../../../../environments/environment';

export interface VideoTimeUpdate {
  currentTime: number;
  duration: number;
}

/**
 * VideoPlayerComponent is a thin, reusable wrapper around the native HTML5
 * <video> element. Accepts a (possibly relative, backend-served) src, exposes
 * play/pause/seek controls, and emits playback position updates so parent
 * pages (e.g. the lecture player) can report watch progress to the backend.
 */
@Component({
  selector: 'app-video-player',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './video-player.component.html',
  styleUrls: ['./video-player.component.scss']
})
export class VideoPlayerComponent implements OnChanges {
  /** Video source URL — absolute, or a relative backend path such as /api/uploads/xyz.mp4. */
  @Input() src: string | null | undefined;

  /** Optional poster/thumbnail image shown before playback starts. */
  @Input() poster: string | null | undefined;

  /** Emits on every native timeupdate event with current position + total duration. */
  @Output() timeUpdate = new EventEmitter<VideoTimeUpdate>();

  /** Emits once when playback reaches the end. */
  @Output() ended = new EventEmitter<void>();

  @ViewChild('videoEl') videoEl!: ElementRef<HTMLVideoElement>;

  isPlaying = false;
  currentTime = 0;
  duration = 0;

  get resolvedSrc(): string | null {
    if (!this.src) return null;
    if (/^https?:\/\//i.test(this.src)) return this.src;
    const origin = environment.apiUrl.replace(/\/api\/?$/, '');
    return this.src.startsWith('/') ? `${origin}${this.src}` : `${origin}/${this.src}`;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['src'] && this.videoEl) {
      this.isPlaying = false;
      this.currentTime = 0;
    }
  }

  togglePlay(): void {
    const video = this.videoEl?.nativeElement;
    if (!video) return;
    if (video.paused) {
      video.play();
    } else {
      video.pause();
    }
  }

  onPlay(): void {
    this.isPlaying = true;
  }

  onPause(): void {
    this.isPlaying = false;
  }

  onLoadedMetadata(): void {
    const video = this.videoEl?.nativeElement;
    if (video) {
      this.duration = video.duration || 0;
    }
  }

  onTimeUpdate(): void {
    const video = this.videoEl?.nativeElement;
    if (!video) return;
    this.currentTime = video.currentTime;
    this.duration = video.duration || this.duration;
    this.timeUpdate.emit({ currentTime: this.currentTime, duration: this.duration });
  }

  onEnded(): void {
    this.isPlaying = false;
    this.ended.emit();
  }

  onSeek(event: Event): void {
    const video = this.videoEl?.nativeElement;
    const input = event.target as HTMLInputElement;
    if (!video || !input) return;
    const value = Number(input.value);
    video.currentTime = value;
    this.currentTime = value;
  }

  /** Seeks to an absolute position in seconds (used by bookmarks/notes "jump to"). */
  seekTo(seconds: number): void {
    const video = this.videoEl?.nativeElement;
    if (!video) return;
    video.currentTime = seconds;
    this.currentTime = seconds;
    video.play();
  }

  formatTime(seconds: number): string {
    if (!isFinite(seconds) || seconds < 0) return '00:00';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }
}
