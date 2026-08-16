import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { AnnouncementDto, AnnouncementRequest } from '@core/models/social.model';

/**
 * AnnouncementService — shared across all three portals (student/teacher/admin).
 * Backed by the neutral /api/announcements endpoint (audience filtering happens
 * server-side based on the caller's role).
 */
@Injectable({
  providedIn: 'root'
})
export class AnnouncementService {
  constructor(private api: ApiService) {}

  getAnnouncements(): Observable<AnnouncementDto[]> {
    return this.api.get<AnnouncementDto[]>('/announcements');
  }

  createAnnouncement(request: AnnouncementRequest): Observable<AnnouncementDto> {
    return this.api.post<AnnouncementDto>('/announcements', request);
  }

  updateAnnouncement(id: number, request: Partial<AnnouncementRequest>): Observable<AnnouncementDto> {
    return this.api.put<AnnouncementDto>(`/announcements/${id}`, request);
  }

  deleteAnnouncement(id: number): Observable<void> {
    return this.api.delete<void>(`/announcements/${id}`);
  }

  pinAnnouncement(id: number): Observable<AnnouncementDto> {
    return this.api.put<AnnouncementDto>(`/announcements/${id}/pin`, {});
  }
}
