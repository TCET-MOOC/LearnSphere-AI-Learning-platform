import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { College } from '@core/models/user.model';

/**
 * Public, read-only college directory (used by registration/onboarding dropdowns).
 * Admin-side management lives in AdminService.
 */
@Injectable({
  providedIn: 'root'
})
export class CollegeService {
  constructor(private apiService: ApiService) {}

  getColleges(): Observable<College[]> {
    return this.apiService.get<College[]>('/colleges');
  }
}
