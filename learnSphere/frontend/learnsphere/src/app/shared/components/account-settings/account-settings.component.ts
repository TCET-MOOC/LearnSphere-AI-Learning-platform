import { Component, Input, OnInit, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '@core/auth/auth.service';
import { User } from '@core/models/user.model';
import { NotificationService } from '@core/services/notification.service';
import { ThemeService } from '@core/services/theme.service';
import { getInitials } from '@core/utils/avatar.util';
import { AccountFact, AccountSettingsCopy, SettingRowDef, SettingsTabDef } from './account-settings.model';

/**
 * Settings for every role (SCR-26/41/61; W7-12, AUD-13, BR-24). Only real actions are live:
 * the theme switch, Change password (reset link) and Sign out (`POST /auth/logout`). Account
 * facts come from a fresh `GET /users/me`. Preferences the server cannot store are shown,
 * disabled and labelled, and nothing claims "Changes saved". Layout and classes are the ones
 * the three settings screens already used (NFR-01).
 */
@Component({
  selector: 'app-account-settings',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './account-settings.component.html',
  styleUrls: ['./account-settings.component.scss'],
})
export class AccountSettingsComponent implements OnInit {
  @Input({ required: true }) copy!: AccountSettingsCopy;

  private readonly auth = inject(AuthService);
  private readonly theme = inject(ThemeService);
  private readonly notify = inject(NotificationService);
  private readonly router = inject(Router);

  user: User | null = null;
  activeTab = '';
  sendingReset = false;

  ngOnInit(): void {
    this.user = this.auth.currentUser;
    this.activeTab = this.copy.tabs[0].name;
    this.auth.fetchCurrentUser().subscribe({
      next: (u) => (this.user = u),
      error: () => {},
    });
  }

  get name(): string { return this.user?.fullName || this.copy.fallbackName; }
  get email(): string { return this.user?.email || ''; }
  get initials(): string { return getInitials(this.name); }
  get tab(): SettingsTabDef { return this.copy.tabs.find((t) => t.name === this.activeTab) ?? this.copy.tabs[0]; }
  get isDark(): boolean { return this.theme.isDarkMode; }

  factValue(fact: AccountFact): string {
    const f = this.copy.facts;
    if (fact === 'role') return (this.user?.role && f.roles[this.user.role]) || '—';
    const status = this.user?.collegeVerificationStatus;
    return status === 'VERIFIED' ? f.verified : status === 'PENDING' ? f.pending : f.notVerified;
  }

  toggleTheme(): void {
    this.theme.toggleTheme();
  }

  changePassword(): void {
    if (this.sendingReset || !this.email) return;
    this.sendingReset = true;
    this.auth.forgotPassword({ email: this.email }).subscribe({
      next: (res) => {
        this.sendingReset = false;
        this.notify.success(res?.message || this.copy.resetSent(this.email));
      },
      error: () => {
        this.sendingReset = false;
        this.notify.error(this.copy.resetFailed);
      },
    });
  }

  signOut(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  trackRow(_: number, row: SettingRowDef): string { return row.label; }
}
