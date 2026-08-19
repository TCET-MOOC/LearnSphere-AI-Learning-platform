import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { finalize } from 'rxjs';
import { LeaderboardService } from '../services/leaderboard.service';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { LeaderboardEntryDto, LeaderboardScope } from '@core/models/social.model';
import { getAvatarBg, getAvatarColor, getInitials } from '@core/utils/avatar.util';
import { LucideAngularModule, Trophy, Medal, Star, Rocket, CheckCircle, FileText, MessageCircle, Loader2, RotateCcw } from 'lucide-angular';

@Component({
  selector: 'app-leaderboard',
  standalone: true,
  imports: [
    CommonModule,
    LucideAngularModule
  ],
  templateUrl: './leaderboard.component.html',
  styleUrls: ['./leaderboard.component.scss']
})
export class LeaderboardComponent implements OnInit {
  activeTab = 'monthly';
  entries: LeaderboardEntryDto[] = [];
  loading = false;
  hasError = false;

  constructor(
    private leaderboardService: LeaderboardService,
    private authService: AuthService,
    private notify: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  setTab(tab: string): void {
    this.activeTab = tab;
    this.load();
  }

  // Backend only exposes 'global' and 'college' scopes. 'My Batch' and
  // 'All College' both map to the college-scoped leaderboard (no separate
  // batch/department grouping exists yet); 'By Course' and 'Monthly' fall
  // back to the global leaderboard (no course- or time-windowed leaderboard
  // exists yet). See final report for this simplification.
  private get scope(): LeaderboardScope {
    return this.activeTab === 'batch' || this.activeTab === 'college' ? 'college' : 'global';
  }

  load(): void {
    this.loading = true;
    this.hasError = false;
    this.cdr.markForCheck();

    this.leaderboardService.getLeaderboard(this.scope)
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (entries) => {
          this.entries = entries || [];
          this.hasError = false;
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Failed to load leaderboard:', err);
          this.entries = [];
          this.hasError = true;
          this.notify.error('Could not load the leaderboard.');
          this.cdr.markForCheck();
        }
      });
  }

  get topThree(): LeaderboardEntryDto[] {
    return this.entries.slice(0, 3);
  }

  get myEntry(): LeaderboardEntryDto | undefined {
    const myId = this.authService.currentUser?.id;
    return this.entries.find(e => e.studentId === myId);
  }

  get afterMeEntry(): LeaderboardEntryDto | undefined {
    const me = this.myEntry;
    if (!me) return undefined;
    return this.entries.find(e => e.rank === me.rank + 1);
  }

  get aboveMeEntry(): LeaderboardEntryDto | undefined {
    const me = this.myEntry;
    if (!me || me.rank <= 1) return undefined;
    return this.entries.find(e => e.rank === me.rank - 1);
  }

  get showMeInMainList(): boolean {
    const me = this.myEntry;
    return !!me && me.rank > 3;
  }

  get showDivider(): boolean {
    const me = this.myEntry;
    return !!me && me.rank > 4;
  }

  get dividerLabel(): string {
    const me = this.myEntry;
    if (!me) return '';
    return me.rank === 5 ? '· · · rank 4 · · ·' : `· · · ranks 4 – ${me.rank - 1} · · ·`;
  }

  get totalStudents(): number {
    return this.entries.length;
  }

  get pointsToNext(): number | null {
    const me = this.myEntry;
    const above = this.aboveMeEntry;
    if (!me || !above) return null;
    return above.points - me.points;
  }



  getInitials(name: string): string { return getInitials(name); }
  getBg(name: string): string { return getAvatarBg(name); }
  getColor(name: string): string { return getAvatarColor(name); }
}
