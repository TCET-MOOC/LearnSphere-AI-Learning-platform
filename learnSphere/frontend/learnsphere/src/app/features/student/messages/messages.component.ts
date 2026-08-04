import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MessagingService } from '../services/messaging.service';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { ConversationDto, MessageDto } from '@core/models/social.model';
import { getAvatarBg, getAvatarColor, getInitials } from '@core/utils/avatar.util';
import { formatClockTime, timeAgo } from '@core/utils/time.util';

// Mirrors the teacher messages component structure for consistency.
// The only differences: sender field uses 'student' | 'teacher' (instead of
// teacher's 'teacher' | 'student'), and conversations are with teachers
// (not students).

interface Message {
  id: string;
  sender: 'student' | 'teacher';
  text: string;
  time: string;
}

interface Conversation {
  id: number;
  teacherName: string;
  initials: string;
  bg: string;
  color: string;
  course: string;
  lastMessage: string;
  lastTime: string;
  unread: number;
  messages: Message[];
}

@Component({
  selector: 'app-student-messages',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './messages.component.html',
  styleUrls: ['./messages.component.scss']
})
export class MessagesComponent implements OnInit {
  searchText = '';
  activeFilter: 'all' | 'unread' | 'courses' = 'all';
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
      teacherName: dto.otherUserName,
      initials: getInitials(dto.otherUserName),
      bg: getAvatarBg(dto.otherUserName),
      color: getAvatarColor(dto.otherUserName),
      course: dto.courseName || '',
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
      sender: dto.senderId === currentUserId ? 'student' : 'teacher',
      text: dto.text,
      time: formatClockTime(dto.sentAt)
    };
  }

  // Filtered list — same logic as teacher version.
  get filteredConversations(): Conversation[] {
    let list = this.conversations;
    if (this.activeFilter === 'unread') {
      list = list.filter(c => c.unread > 0);
    }
    if (this.searchText) {
      const q = this.searchText.toLowerCase();
      list = list.filter(c =>
        c.teacherName.toLowerCase().includes(q) ||
        c.course.toLowerCase().includes(q) ||
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

  setFilter(filter: 'all' | 'unread' | 'courses'): void {
    this.activeFilter = filter;
  }
}
