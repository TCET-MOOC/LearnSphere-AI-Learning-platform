import { Injectable, Inject, PLATFORM_ID, Optional } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router, NavigationEnd } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';

export type ThemeMode = 'dark' | 'light';

export const PUBLIC_ROUTES = [
  '/',
  '/login',
  '/register',
  '/forgot-password',
  '/verify-college',
  '/verify-certificate',
  '/verify/certificate'
];

export function isPublicRoute(url: string): boolean {
  if (!url) return true;
  const clean = url.split('?')[0].split('#')[0].replace(/\/+$/, '') || '/';
  return PUBLIC_ROUTES.some(route => route === clean || clean.startsWith(route + '/'));
}

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly THEME_KEY = 'ls_theme_mode';
  private themeSubject: BehaviorSubject<ThemeMode>;
  public theme$: Observable<ThemeMode>;

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    @Optional() private router?: Router
  ) {
    const initialTheme = this.getInitialTheme();
    this.themeSubject = new BehaviorSubject<ThemeMode>(initialTheme);
    this.theme$ = this.themeSubject.asObservable();
    this.applyTheme(initialTheme);

    if (this.router) {
      this.router.events.subscribe(event => {
        if (event instanceof NavigationEnd) {
          this.handleRouteChange(event.urlAfterRedirects || event.url);
        }
      });
    }
  }

  get currentTheme(): ThemeMode {
    return this.themeSubject.value;
  }

  get isDarkMode(): boolean {
    return this.themeSubject.value === 'dark';
  }

  toggleTheme(): void {
    const nextTheme: ThemeMode = this.themeSubject.value === 'dark' ? 'light' : 'dark';
    this.setTheme(nextTheme);
  }

  setTheme(theme: ThemeMode): void {
    this.themeSubject.next(theme);
    this.applyTheme(theme);
    if (isPlatformBrowser(this.platformId)) {
      try {
        localStorage.setItem(this.THEME_KEY, theme);
      } catch (e) {}
    }
  }

  private handleRouteChange(url: string): void {
    if (isPublicRoute(url)) {
      if (this.themeSubject.value !== 'light') {
        this.themeSubject.next('light');
        this.applyTheme('light');
      }
    } else {
      const savedTheme = this.getSavedUserTheme();
      if (this.themeSubject.value !== savedTheme) {
        this.themeSubject.next(savedTheme);
        this.applyTheme(savedTheme);
      }
    }
  }

  private getInitialTheme(): ThemeMode {
    if (isPlatformBrowser(this.platformId)) {
      const currentUrl = this.router?.url || window.location.pathname;
      if (isPublicRoute(currentUrl)) {
        return 'light';
      }
      return this.getSavedUserTheme();
    }
    return 'light';
  }

  private getSavedUserTheme(): ThemeMode {
    if (isPlatformBrowser(this.platformId)) {
      try {
        const saved = localStorage.getItem(this.THEME_KEY) as ThemeMode | null;
        if (saved === 'dark' || saved === 'light') {
          return saved;
        }
        if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
          return 'dark';
        }
      } catch (e) {}
    }
    return 'light';
  }

  private applyTheme(theme: ThemeMode): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const root = document.documentElement;
    const body = document.body;

    root.setAttribute('data-theme', theme);

    if (theme === 'dark') {
      root.classList.add('dark-theme');
      root.classList.remove('light-theme');
      body.classList.add('dark-theme');
      body.classList.remove('light-theme');
    } else {
      root.classList.add('light-theme');
      root.classList.remove('dark-theme');
      body.classList.add('light-theme');
      body.classList.remove('dark-theme');
    }
  }
}

