import { TestBed } from '@angular/core/testing';
import { Router, NavigationEnd } from '@angular/router';
import { Subject } from 'rxjs';
import { ThemeService, ThemeMode, isPublicRoute } from './theme.service';

describe('ThemeService (DS-02 / W1-13)', () => {
  let service: ThemeService;
  let routerEvents$: Subject<any>;
  let mockRouter: any;

  beforeEach(() => {
    localStorage.clear();
    routerEvents$ = new Subject<any>();
    mockRouter = {
      url: '/',
      events: routerEvents$.asObservable(),
      parseUrl: (url: string) => ({ root: { children: {} }, toString: () => url })
    };

    TestBed.configureTestingModule({
      providers: [
        ThemeService,
        { provide: Router, useValue: mockRouter }
      ]
    });
  });

  afterEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.documentElement.className = '';
    document.body.className = '';
  });

  it('should identify public routes accurately via isPublicRoute', () => {
    expect(isPublicRoute('/')).toBe(true);
    expect(isPublicRoute('/login')).toBe(true);
    expect(isPublicRoute('/register')).toBe(true);
    expect(isPublicRoute('/forgot-password')).toBe(true);
    expect(isPublicRoute('/verify-college')).toBe(true);
    expect(isPublicRoute('/verify-certificate')).toBe(true);
    expect(isPublicRoute('/verify-certificate/ABC-123')).toBe(true);

    expect(isPublicRoute('/student/dashboard')).toBe(false);
    expect(isPublicRoute('/student/courses')).toBe(false);
    expect(isPublicRoute('/teacher/dashboard')).toBe(false);
    expect(isPublicRoute('/admin/users')).toBe(false);
    expect(isPublicRoute('/payment/checkout/123')).toBe(false);
  });

  it('should pin theme to light on public route even if localStorage has dark', () => {
    localStorage.setItem('ls_theme_mode', 'dark');
    mockRouter.url = '/';

    service = TestBed.inject(ThemeService);

    expect(service.currentTheme).toBe('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(document.documentElement.classList.contains('light-theme')).toBe(true);
    expect(document.documentElement.classList.contains('dark-theme')).toBe(false);
    expect(document.body.classList.contains('light-theme')).toBe(true);
    // User preference in localStorage must remain intact
    expect(localStorage.getItem('ls_theme_mode')).toBe('dark');
  });

  it('should restore user saved preference when navigating to in-app route', () => {
    localStorage.setItem('ls_theme_mode', 'dark');
    mockRouter.url = '/';
    service = TestBed.inject(ThemeService);

    expect(service.currentTheme).toBe('light');

    // Navigate to in-app route
    mockRouter.url = '/student/dashboard';
    routerEvents$.next(new NavigationEnd(1, '/student/dashboard', '/student/dashboard'));

    expect(service.currentTheme).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(document.documentElement.classList.contains('dark-theme')).toBe(true);
    expect(document.body.classList.contains('dark-theme')).toBe(true);

    // Navigate back to public route /login
    mockRouter.url = '/login';
    routerEvents$.next(new NavigationEnd(2, '/login', '/login'));

    expect(service.currentTheme).toBe('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');

    // Navigate back to in-app route /teacher/courses
    mockRouter.url = '/teacher/courses';
    routerEvents$.next(new NavigationEnd(3, '/teacher/courses', '/teacher/courses'));

    expect(service.currentTheme).toBe('dark');
  });

  it('should default in-app routes to light if no saved preference and system is light', () => {
    mockRouter.url = '/student/dashboard';
    service = TestBed.inject(ThemeService);

    expect(service.currentTheme).toBe('light');
  });

  it('should persist theme changes on in-app routes to localStorage', () => {
    mockRouter.url = '/student/settings';
    service = TestBed.inject(ThemeService);

    service.setTheme('dark');
    expect(service.currentTheme).toBe('dark');
    expect(localStorage.getItem('ls_theme_mode')).toBe('dark');

    service.toggleTheme();
    expect(service.currentTheme).toBe('light');
    expect(localStorage.getItem('ls_theme_mode')).toBe('light');
  });
});
