import { Injectable } from '@angular/core';
import { Observable, ReplaySubject, throwError } from 'rxjs';
import { take, timeout } from 'rxjs/operators';

declare global {
  interface Window {
    YT: any;
    onYouTubeIframeAPIReady: () => void;
  }
}

@Injectable({
  providedIn: 'root'
})
export class YouTubeLoaderService {
  private apiReady$ = new ReplaySubject<void>(1);
  private isScriptInjected = false;

  constructor() {
    this.initLoader();
  }

  private initLoader(): void {
    if (typeof window === 'undefined') return;

    // If YT API is already loaded and ready
    if (window.YT && window.YT.Player) {
      this.apiReady$.next();
      return;
    }

    // Set up global callback if not present
    const existingCallback = window.onYouTubeIframeAPIReady;
    window.onYouTubeIframeAPIReady = () => {
      if (typeof existingCallback === 'function') {
        try {
          existingCallback();
        } catch (e) {}
      }
      this.apiReady$.next();
    };

    // Inject the YouTube iframe API script tag if not already on the page
    const existingScript = document.querySelector('script[src*="youtube.com/iframe_api"]');
    if (!existingScript && !this.isScriptInjected) {
      this.isScriptInjected = true;
      const tag = document.createElement('script');
      tag.src = 'https://www.youtube.com/iframe_api';
      tag.async = true;
      document.body.appendChild(tag);
    }
  }

  loadApi(): Observable<void> {
    if (typeof window !== 'undefined' && window.YT && window.YT.Player) {
      return new Observable<void>(observer => {
        queueMicrotask(() => {
          observer.next();
          observer.complete();
        });
      });
    }

    return this.apiReady$.asObservable().pipe(
      take(1),
      timeout({
        each: 10000,
        with: () => throwError(() => new Error('YouTube IFrame API failed to load (timeout)'))
      })
    );
  }
}
