import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MessagingService } from '../services/messaging.service';
import { StudentService } from '../services/student.service';
import { AuthService } from '@core/auth/auth.service';
import { NotificationService } from '@core/services/notification.service';
import { WebSocketService } from '@core/services/websocket.service';
import { Subscription } from 'rxjs';
import { ConversationDto, MessageDto } from '@core/models/social.model';
import { Course } from '@core/models/course.model';
import { getAvatarBg, getAvatarColor, getInitials } from '@core/utils/avatar.util';
import { formatClockTime, timeAgo } from '@core/utils/time.util';
import { LucideAngularModule, Plus, Search, Send, MessageSquare, X, BookOpen } from 'lucide-angular';

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
  imports: [CommonModule, FormsModule, RouterModule, LucideAngularModule],
  templateUrl: './messages.component.html',
  styleUrls: ['./messages.component.scss']
})
export class MessagesComponent implements OnInit, OnDestroy {
  searchText = '';
  activeFilter: 'all' | 'unread' | 'courses' = 'all';
  newMessage = '';

  conversations: Conversation[] = [];
  selectedConversation: Conversation | null = null;
  loading = false;

  private convWsSub?: Subscription;
  private userWsSub?: Subscription;

  // New message modal state
  isComposeOpen = false;
  enrolledCourses: Course[] = [];
  selectedCourseId: number | null = null;
  composeText = '';
  sendingCompose = false;

  constructor(
    private messagingService: MessagingService,
    private studentService: StudentService,
    private authService: AuthService,
    private wsService: WebSocketService,
    private notify: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadConversations();
    this.loadCourses();
    this.subscribeToUserMessages();
  }

  private subscribeToUserMessages(): void {
    const user = this.authService.currentUser;
    if (!user) return;

    this.userWsSub = this.wsService.subscribeToTopic<MessageDto>(`/topic/user/${user.id}/messages`).subscribe({
      next: (msg) => {
        if (!msg) return;
        const conv = this.conversations.find(c => c.id === msg.conversationId);
        if (conv) {
          conv.lastMessage = msg.text;
          conv.lastTime = 'Just now';
          if (this.selectedConversation?.id !== conv.id) {
            conv.unread = (conv.unread || 0) + 1;
          }
        } else {
          this.messagingService.getConversations().subscribe({
            next: (dtos) => {
              this.conversations = dtos.map(d => this.toConversation(d));
            }
          });
        }
      }
    });
  }

  private loadCourses(): void {
    this.studentService.getEnrolledCourses().subscribe({
      next: (courses) => {
        this.enrolledCourses = courses;
        if (courses.length > 0) {
          this.selectedCourseId = courses[0].id;
        }
      },
      error: () => {}
    });
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

    if (this.convWsSub) {
      this.convWsSub.unsubscribe();
    }

    this.convWsSub = this.wsService.subscribeToTopic<MessageDto>(`/topic/conversations/${conv.id}`).subscribe({
      next: (msg) => {
        if (!msg) return;
        const exists = conv.messages.some(m => m.id === String(msg.id));
        if (!exists) {
          conv.messages.push(this.toMessage(msg));
          conv.lastMessage = msg.text;
          conv.lastTime = 'Just now';
        }
      }
    });

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
        const exists = conv.messages.some(m => m.id === String(dto.id));
        if (!exists) {
          conv.messages.push(this.toMessage(dto));
        }
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

  openCompose(): void {
    if (this.enrolledCourses.length === 0) {
      this.notify.info('You must be enrolled in a course to message an instructor.');
      return;
    }
    this.composeText = '';
    if (!this.selectedCourseId && this.enrolledCourses.length > 0) {
      this.selectedCourseId = this.enrolledCourses[0].id;
    }
    this.isComposeOpen = true;
  }

  closeCompose(): void {
    this.isComposeOpen = false;
    this.composeText = '';
  }

  getSelectedCourse(): Course | undefined {
    return this.enrolledCourses.find(c => c.id === this.selectedCourseId);
  }

  sendCompose(): void {
    const text = this.composeText.trim();
    const course = this.getSelectedCourse();
    if (!text || !course) return;

    this.sendingCompose = true;
    this.messagingService.startConversation({
      otherUserId: course.teacherId,
      courseId: course.id
    }).subscribe({
      next: (convDto) => {
        this.messagingService.sendMessage(convDto.id, text).subscribe({
          next: () => {
            this.sendingCompose = false;
            this.closeCompose();
            this.notify.success('Message sent to instructor!');
            this.loadConversations();
          },
          error: () => {
            this.sendingCompose = false;
            this.notify.error('Conversation created, but failed to send message.');
            this.loadConversations();
          }
        });
      },
      error: () => {
        this.sendingCompose = false;
        this.notify.error('Could not start conversation with instructor.');
      }
    });
  }

  ngOnDestroy(): void {
    if (this.convWsSub) this.convWsSub.unsubscribe();
    if (this.userWsSub) this.userWsSub.unsubscribe();
  }
}
