import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router, NavigationEnd } from '@angular/router';
import { BehaviorSubject, filter } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SidebarService {
  private collapsedKey = 'learnsphere_sidebar_collapsed';
  
  // Desktop collapsed state
  private collapsedSubject = new BehaviorSubject<boolean>(false);
  public isCollapsed$ = this.collapsedSubject.asObservable();

  // Mobile off-canvas drawer open state
  private mobileOpenSubject = new BehaviorSubject<boolean>(false);
  public isMobileOpen$ = this.mobileOpenSubject.asObservable();

  constructor(
    @Inject(PLATFORM_ID) private platformId: Object,
    private router: Router
  ) {
    if (isPlatformBrowser(this.platformId)) {
      const savedState = localStorage.getItem(this.collapsedKey);
      if (savedState) {
        this.collapsedSubject.next(savedState === 'true');
      }

      // Automatically close the mobile drawer upon route navigation
      this.router.events
        .pipe(filter(event => event instanceof NavigationEnd))
        .subscribe(() => {
          this.closeMobile();
        });
    }
  }

  public toggleSidebar(): void {
    const currentState = this.collapsedSubject.value;
    const newState = !currentState;
    
    this.collapsedSubject.next(newState);
    
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.collapsedKey, String(newState));
    }
  }

  public toggleMobile(): void {
    this.mobileOpenSubject.next(!this.mobileOpenSubject.value);
  }

  public openMobile(): void {
    this.mobileOpenSubject.next(true);
  }

  public closeMobile(): void {
    if (this.mobileOpenSubject.value) {
      this.mobileOpenSubject.next(false);
    }
  }
}
