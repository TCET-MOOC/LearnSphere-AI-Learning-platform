import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { StudentService } from '../services/student.service';
import { ApiService } from '@core/services/api.service';
import { NotificationService } from '@core/services/notification.service';
import { WebSocketService } from '@core/services/websocket.service';
import { AuthService } from '@core/auth/auth.service';
import { Subscription } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';

interface ChatMessage {
  id: string;
  sender: string;
  isTeacher: boolean;
  text: string;
  time: string;
  isQuestion?: boolean;
  isCensored?: boolean;
}

@Component({
  selector: 'app-student-live-room',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule,
    RouterModule,
    LucideAngularModule
  ],
  templateUrl: './live-room.component.html',
  styleUrls: ['./live-room.component.scss']
})
export class StudentLiveRoomComponent implements OnInit, OnDestroy {
  sessionId!: number;
  session: any = null;
  loading = true;

  // Live controls
  isHandRaised = false;
  isMuted = false;
  activeTab: 'chat' | 'qa' | 'notes' = 'chat';

  // Live Chat
  messages: ChatMessage[] = [
    {
      id: '1',
      sender: 'System',
      isTeacher: false,
      text: 'Welcome to the live interactive classroom! Please keep questions relevant to the lecture topic.',
      time: 'Just now'
    }
  ];
  newMessage = '';
  liveNotes = '';
  viewerCount = 38;
  strikeCount = 0;
  chatMutedUntil: Date | null = null;

  private viewerInterval: any;
  private chatWsSub?: Subscription;
  private sessionWsSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private studentService: StudentService,
    private apiService: ApiService,
    private authService: AuthService,
    private wsService: WebSocketService,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.sessionId = Number(idParam);
      this.loadSession(this.sessionId);
      this.subscribeToLiveRoom();
    } else {
      this.router.navigate(['/student/live']);
    }

    // Simulate active audience fluctuation
    this.viewerInterval = setInterval(() => {
      this.viewerCount = Math.max(25, this.viewerCount + Math.floor(Math.random() * 3) - 1);
      this.cdr.markForCheck();
    }, 8000);
  }

  private subscribeToLiveRoom(): void {
    // Listen to session status changes (e.g. Host ended stream)
    this.sessionWsSub = this.wsService.subscribeToTopic<any>(`/topic/live-sessions/${this.sessionId}`).subscribe({
      next: (sessionUpdate) => {
        if (sessionUpdate && sessionUpdate.status) {
          this.session = sessionUpdate;
          if (sessionUpdate.status === 'ENDED') {
            this.notificationService.info('The instructor has ended the live stream.');
          }
          this.cdr.markForCheck();
        }
      }
    });

    // Listen to live chat stream
    this.chatWsSub = this.wsService.subscribeToTopic<any>(`/topic/live-sessions/${this.sessionId}/chat`).subscribe({
      next: (msg) => {
        if (!msg) return;
        const currentUserName = this.authService.currentUser?.fullName;
        const isMe = msg.sender === currentUserName || msg.sender === 'You';
        
        // Append if not already locally rendered
        if (!isMe) {
          this.messages.push({
            id: String(Date.now() + Math.random()),
            sender: msg.sender || 'Participant',
            isTeacher: !!msg.isTeacher,
            text: msg.text,
            time: 'Just now',
            isQuestion: !!msg.isQuestion,
            isCensored: !!msg.isCensored
          });
          this.cdr.markForCheck();
        }
      }
    });
  }

  ngOnDestroy(): void {
    if (this.viewerInterval) {
      clearInterval(this.viewerInterval);
    }
    if (this.chatWsSub) this.chatWsSub.unsubscribe();
    if (this.sessionWsSub) this.sessionWsSub.unsubscribe();
  }

  loadSession(id: number): void {
    this.loading = true;
    this.studentService.getLiveSession(id).subscribe({
      next: (session) => {
        this.session = session;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.notificationService.error('Session not found or unavailable.');
        this.router.navigate(['/student/live']);
      }
    });
  }

  toggleHandRaise(): void {
    this.isHandRaised = !this.isHandRaised;
    if (this.isHandRaised) {
      this.notificationService.success('Hand raised! The instructor has been notified.');
      this.messages.push({
        id: Date.now().toString(),
        sender: 'You',
        isTeacher: false,
        text: '✋ Raised hand to ask a question.',
        time: 'Just now',
        isQuestion: true
      });
    } else {
      this.notificationService.info('Hand lowered.');
    }
  }

  sendMessage(): void {
    const text = this.newMessage.trim();
    if (!text) return;

    if (this.chatMutedUntil && new Date() < this.chatMutedUntil) {
      this.notificationService.error('You are on a temporary chat cooldown due to repeated policy violations.');
      return;
    }

    this.apiService.post<any>('/censor', { text }).subscribe({
      next: (result) => {
        if (result.shouldBlock) {
          this.strikeCount++;
          if (this.strikeCount >= 2) {
            this.chatMutedUntil = new Date(Date.now() + 15 * 60 * 1000);
            this.notificationService.error('⚠️ Severe violation detected. You have been placed on a 15-minute chat timeout.');
          } else {
            this.notificationService.error('⚠️ Message blocked: Language violating student safety policy was detected (Strike 1).');
          }
          this.newMessage = '';
          return;
        }

        if (!result.isClean) {
          this.notificationService.info('Notice: Inappropriate words were censored to maintain classroom etiquette.');
        }

        const chatPayload = {
          sender: this.authService.currentUser?.fullName || 'Student',
          isTeacher: false,
          text: result.maskedText,
          isQuestion: this.activeTab === 'qa',
          isCensored: !result.isClean
        };

        this.messages.push({
          id: Date.now().toString(),
          sender: 'You',
          isTeacher: false,
          text: result.maskedText,
          time: 'Just now',
          isQuestion: this.activeTab === 'qa',
          isCensored: !result.isClean
        });

        // Publish over WebSocket
        this.wsService.publish(`/app/live-sessions/${this.sessionId}/chat`, chatPayload);

        this.newMessage = '';
        this.cdr.markForCheck();
      },
      error: () => {
        // Fallback local delivery
        const chatPayload = {
          sender: this.authService.currentUser?.fullName || 'Student',
          isTeacher: false,
          text,
          isQuestion: this.activeTab === 'qa'
        };

        this.messages.push({
          id: Date.now().toString(),
          sender: 'You',
          isTeacher: false,
          text,
          time: 'Just now',
          isQuestion: this.activeTab === 'qa'
        });

        this.wsService.publish(`/app/live-sessions/${this.sessionId}/chat`, chatPayload);

        this.newMessage = '';
        this.cdr.markForCheck();
      }
    });
  }

  saveNote(): void {
    if (!this.liveNotes.trim()) return;
    this.studentService.createNote({
      courseId: this.session?.courseId,
      title: `Live Session: ${this.session?.title || 'Notes'}`,
      content: this.liveNotes
    }).subscribe({
      next: () => this.notificationService.success('Live notes saved to My Notes!'),
      error: () => this.notificationService.error('Failed to save notes.')
    });
  }

  leaveRoom(): void {
    this.router.navigate(['/student/live']);
  }
}
