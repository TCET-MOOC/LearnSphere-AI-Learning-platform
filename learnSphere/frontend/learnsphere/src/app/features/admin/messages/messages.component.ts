import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { MessagingService } from '../services/messaging.service';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { ConversationDto, MessageDto } from '@core/models/social.model';
import { getAvatarBg, getAvatarColor, getInitials } from '@core/utils/avatar.util';
import { formatClockTime, timeAgo } from '@core/utils/time.util';

interface Message {
  id: string;
  sender: 'admin' | 'other';
  text: string;
  time: string;
}

interface Conversation {
  id: number;
  personName: string;
  role: 'teacher' | 'student';
  initials: string;
  bg: string;
  color: string;
  context: string;
  lastMessage: string;
  lastTime: string;
  unread: number;
  messages: Message[];
}

@Component({
  selector: 'app-admin-messages',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './messages.component.html',
  styleUrls: ['./messages.component.scss']
})
export class MessagesComponent implements OnInit {
  searchText = '';
  activeFilter: 'all' | 'unread' = 'all';
  newMessage = '';

  conversations: Conversation[] = [];
  selectedConversation: Conversation | null = null;
  loading = false;

  constructor(
    private messagingService: MessagingService,
    private authService: AuthService,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadConversations();
  }

  private loadConversations(): void {
    this.loading = true;
    this.messagingService.getConversations().subscribe({
      next: (dtos) => {
        this.conversations = dtos.map((dto) => this.toConversation(dto));
        this.loading = false;
        if (this.conversations.length > 0) {
          this.selectConversation(this.conversations[0]);
        }
      },
      error: () => {
        this.loading = false;
        this.notify.error('Could not load conversations.');
      }
    });
  }

  private toConversation(dto: ConversationDto): Conversation {
    return {
      id: dto.id,
      personName: dto.otherUserName,
      role: dto.otherUserRole === 'teacher' ? 'teacher' : 'student',
      initials: getInitials(dto.otherUserName),
      bg: getAvatarBg(dto.otherUserName),
      color: getAvatarColor(dto.otherUserName),
      context: dto.courseName || 'Direct message',
      lastMessage: dto.lastMessage || '',
      lastTime: dto.lastMessageAt ? timeAgo(dto.lastMessageAt) : '',
      unread: dto.unreadCount,
      messages: []
    };
  }

  private toMessage(dto: MessageDto): Message {
    const currentUserId = this.authService.currentUser?.id;
    return {
      id: String(dto.id),
      sender: dto.senderId === currentUserId ? 'admin' : 'other',
      text: dto.text,
      time: formatClockTime(dto.sentAt)
    };
  }

  get filteredConversations(): Conversation[] {
    let list = this.conversations;
    if (this.activeFilter === 'unread') {
      list = list.filter(c => c.unread > 0);
    }
    if (this.searchText) {
      const q = this.searchText.toLowerCase();
      list = list.filter(c =>
        c.personName.toLowerCase().includes(q) ||
        c.context.toLowerCase().includes(q) ||
        c.lastMessage.toLowerCase().includes(q)
      );
    }
    return list;
  }

  get totalUnread(): number {
    return this.conversations.reduce((sum, c) => sum + c.unread, 0);
  }

  selectConversation(conv: Conversation): void {
    this.selectedConversation = conv;
    conv.unread = 0;
    this.messagingService.getMessages(conv.id).subscribe({
      next: (dtos) => {
        conv.messages = dtos.map((dto) => this.toMessage(dto));
      },
      error: () => this.notify.error('Could not load messages.')
    });
  }

  sendMessage(): void {
    const text = this.newMessage.trim();
    if (!text || !this.selectedConversation) return;
    const conv = this.selectedConversation;

    this.messagingService.sendMessage(conv.id, text).subscribe({
      next: (dto) => {
        conv.messages.push(this.toMessage(dto));
        conv.lastMessage = text;
        conv.lastTime = 'Just now';
        this.newMessage = '';
      },
      error: () => this.notify.error('Could not send message.')
    });
  }

  setFilter(filter: 'all' | 'unread'): void {
    this.activeFilter = filter;
  }
}
