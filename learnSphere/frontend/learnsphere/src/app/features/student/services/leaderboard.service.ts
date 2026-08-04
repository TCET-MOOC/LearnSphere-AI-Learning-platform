import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { LeaderboardEntryDto, LeaderboardScope } from '@core/models/social.model';

/**
 * LeaderboardService — real HTTP calls against /api/leaderboard.
 */
@Injectable({
  providedIn: 'root'
})
export class LeaderboardService {
  constructor(private api: ApiService) {}

  getLeaderboard(scope: LeaderboardScope = 'global'): Observable<LeaderboardEntryDto[]> {
    return this.api.get<LeaderboardEntryDto[]>('/leaderboard', { params: { scope } });
  }
}
