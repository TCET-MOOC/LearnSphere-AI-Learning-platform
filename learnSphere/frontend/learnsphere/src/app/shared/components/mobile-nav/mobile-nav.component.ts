import { Component, ElementRef, HostListener, Injector, afterNextRender, computed, inject, input, signal, viewChild } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { AuthService } from '@core/auth/auth.service';
import { ThemeService } from '@core/services/theme.service';
import { MOBILE_NAV, MOBILE_NAV_COPY, MobileRole } from '@core/content/mobile-nav.content';

/**
 * Phone navigation (approved 360 px redesign): a bottom bar with the role's four most-used
 * destinations plus More, and a More sheet with everything else. Hidden from 768 px up,
 * where the desktop sidebar is used. Opening the sheet adds a history entry so the
 * Android back gesture closes it before leaving the page.
 */
@Component({
  selector: 'app-mobile-nav',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, LucideAngularModule],
  templateUrl: './mobile-nav.component.html',
  styleUrls: ['./mobile-nav.component.scss']
})
export class MobileNavComponent {
  readonly role = input<MobileRole>('STUDENT');

  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly theme = inject(ThemeService);
  private readonly injector = inject(Injector);

  readonly copy = MOBILE_NAV_COPY;
  readonly config = computed(() => MOBILE_NAV[this.role()]);
  readonly open = signal(false);
  readonly closeButton = viewChild<ElementRef<HTMLButtonElement>>('closeButton');

  get userName(): string {
    return this.auth.currentUser?.fullName || this.config().roleLabel;
  }

  get initials(): string {
    return this.userName.split(/\s+/).filter(Boolean).slice(0, 2).map((p) => p[0]!.toUpperCase()).join('');
  }

  get darkTheme(): boolean {
    return !!this.theme.isDarkMode;
  }

  openMore(): void {
    if (this.open()) return;
    this.open.set(true);
    try {
      history.pushState({ ...(history.state ?? {}), lsMoreSheet: true }, '');
    } catch {
      // History may be unavailable (sandboxed previews); the sheet still works without it.
    }
    afterNextRender(() => this.closeButton()?.nativeElement.focus(), { injector: this.injector });
  }

  /** Closes the sheet; drops the history entry it added unless the browser already did. */
  close(viaBrowserBack = false): void {
    if (!this.open()) return;
    this.open.set(false);
    if (!viaBrowserBack && history.state?.lsMoreSheet) {
      history.back();
    }
  }

  /** A destination was chosen: its link replaces the sheet's history entry (replaceUrl). */
  chose(): void {
    this.open.set(false);
  }

  toggleTheme(): void {
    this.theme.toggleTheme();
  }

  signOut(): void {
    this.open.set(false);
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  @HostListener('window:popstate')
  onBrowserBack(): void {
    this.close(true);
  }

  @HostListener('document:keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && this.open()) {
      event.stopPropagation();
      this.close();
    }
  }
}
