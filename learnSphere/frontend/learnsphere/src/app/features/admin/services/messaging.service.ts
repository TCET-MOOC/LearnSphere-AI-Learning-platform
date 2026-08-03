import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

// Admin messaging service — mirrors teacher/student messaging.service.ts shape.
// Admin can message both teachers and students (platform-level oversight).
// All methods return mock data via of() — swap bodies for real HTTP calls
// through core/services/api.service.ts once the backend is ready.

@Injectable({
  providedIn: 'root'
})
export class MessagingService {

  constructor() {}

  // Returns all conversations for the admin (both teachers and students).
  getConversations(): Observable<any[]> {
    return of([]);
  }

  // Returns the message thread with a specific user (teacher or student).
  getMessages(userId: string): Observable<any[]> {
    return of([]);
  }

  // Sends a message to a user. Returns the created message.
  sendMessage(userId: string, text: string): Observable<any> {
    return of({ id: Date.now().toString(), sender: 'admin', text, time: 'Just now' });
  }
}
