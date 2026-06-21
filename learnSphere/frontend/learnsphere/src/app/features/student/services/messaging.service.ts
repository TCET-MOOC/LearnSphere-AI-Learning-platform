import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

// Mirrors teacher/services/messaging.service.ts shape.
// All methods return mock data via of() — swap bodies for real HTTP calls
// through core/services/api.service.ts once the backend is ready.
// Example: return this.api.get<Conversation[]>('/student/conversations');

@Injectable({
  providedIn: 'root'
})
export class MessagingService {

  constructor() {}

  // Returns all conversations for the current student.
  getConversations(): Observable<any[]> {
    return of([]);
  }

  // Returns the message thread with a specific teacher.
  getMessages(teacherId: string): Observable<any[]> {
    return of([]);
  }

  // Sends a message to a teacher. Returns the created message.
  sendMessage(teacherId: string, text: string): Observable<any> {
    return of({ id: Date.now().toString(), sender: 'student', text, time: 'Just now' });
  }
}
