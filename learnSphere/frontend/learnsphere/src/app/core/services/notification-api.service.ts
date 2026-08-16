import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { AppNotificationDto } from '@core/models/social.model';

/**
 * NotificationApiService — real HTTP calls for the persisted Notification entity.
 * Not to be confused with core/services/notification.service.ts, which is the
 * unrelated toast/snackbar service.
 */
@Injectable({
  providedIn: 'root'
})
export class NotificationApiService {
  constructor(private api: ApiService) {}

  getNotifications(): Observable<AppNotificationDto[]> {
    return this.api.get<AppNotificationDto[]>('/notifications');
  }

  markRead(id: number): Observable<AppNotificationDto> {
    return this.api.put<AppNotificationDto>(`/notifications/${id}/read`, {});
  }

  markAllRead(): Observable<void> {
    return this.api.put<void>('/notifications/read-all', {});
  }

  getUnreadCount(): Observable<{ count: number }> {
    return this.api.get<{ count: number }>('/notifications/unread-count');
  }
}
