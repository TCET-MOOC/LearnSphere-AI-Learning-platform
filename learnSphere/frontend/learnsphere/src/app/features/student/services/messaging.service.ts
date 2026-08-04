import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { ConversationDto, MessageDto, StartConversationRequest } from '@core/models/social.model';

/**
 * MessagingService (student) — real HTTP calls against the shared
 * /api/conversations backend (usable by any authenticated role).
 */
@Injectable({
  providedIn: 'root'
})
export class MessagingService {

  constructor(private api: ApiService) {}

  // Returns all conversations for the current student.
  getConversations(): Observable<ConversationDto[]> {
    return this.api.get<ConversationDto[]>('/conversations');
  }

  // Returns the message thread for a conversation (marks messages read as a side effect).
  getMessages(conversationId: number): Observable<MessageDto[]> {
    return this.api.get<MessageDto[]>(`/conversations/${conversationId}/messages`);
  }

  // Sends a message in an existing conversation.
  sendMessage(conversationId: number, text: string): Observable<MessageDto> {
    return this.api.post<MessageDto>(`/conversations/${conversationId}/messages`, { text });
  }

  // Starts (or reuses) a conversation with another user, optionally scoped to a course.
  startConversation(request: StartConversationRequest): Observable<ConversationDto> {
    return this.api.post<ConversationDto>('/conversations', request);
  }
}
