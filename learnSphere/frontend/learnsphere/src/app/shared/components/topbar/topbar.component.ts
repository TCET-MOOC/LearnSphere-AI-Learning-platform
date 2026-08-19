import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { NotificationStateService } from '../../../core/services/notification-state.service';
import { NotificationApiService } from '../../../core/services/notification-api.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ThemeService } from '../../../core/services/theme.service';
import { timeAgo } from '../../../core/utils/time.util';
import { SidebarService } from '../../../core/services/sidebar.service';
import { WebSocketService } from '../../../core/services/websocket.service';
import { Subscription } from 'rxjs';
import { 
  LucideAngularModule, 
  Bell, 
  Search, 
  Moon, 
  Sun, 
  MessageSquare, 
  Megaphone, 
  Sparkles, 
  Award, 
  User, 
  Settings, 
  X, 
  ChevronRight,
  Menu
} from 'lucide-angular';

export interface TopbarNotificationItem {
  id: number;
  title: string;
  body: string;
  time: string;
  read: boolean;
  category: 'quiz' | 'certificate' | 'announcement' | 'discussion' | 'system';
  link?: string;
}

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  templateUrl: './topbar.component.html',
  styleUrls: ['./topbar.component.scss']
})
export class TopbarComponent implements OnInit {
  isMenuOpen = false;
  isNotificationsOpen = false;

  notifications: TopbarNotificationItem[] = [];
  private wsSubscription?: Subscription;

  constructor(
    private router: Router, 
    private notificationState: NotificationStateService,
    private notificationApi: NotificationApiService,
    private authService: AuthService,
    private themeService: ThemeService,
    private wsService: WebSocketService,
    public sidebarService: SidebarService
  ) {}

  ngOnInit(): void {
    this.loadNotifications();
    this.subscribeToRealtimeNotifications();
  }

  private subscribeToRealtimeNotifications(): void {
    const user = this.authService.currentUser;
    if (!user) return;

    const topic = `/topic/notifications/${user.id}`;
    this.wsSubscription = this.wsService.subscribeToTopic<any>(topic).subscribe({
      next: (dto) => {
        if (!dto) return;
        const newItem: TopbarNotificationItem = {
          id: dto.id,
          title: dto.title || 'New Notification',
          body: dto.body,
          time: 'Just now',
          read: false,
          category: (dto.category as any) || this.detectCategory(dto.title, dto.body)
        };
        this.notifications.unshift(newItem);
        this.notificationState.setCount(this.role, this.unreadNotifications + 1);
      }
    });
  }

  toggleMobileSidebar(): void {
    this.sidebarService.toggleMobile();
  }

  get isDarkMode(): boolean {
    return this.themeService.isDarkMode;
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  private detectCategory(title: string = '', body: string = ''): 'quiz' | 'certificate' | 'announcement' | 'discussion' | 'system' {
    const combined = `${title} ${body}`.toLowerCase();
    if (combined.includes('quiz') || combined.includes('test') || combined.includes('assessment')) return 'quiz';
    if (combined.includes('certificate') || combined.includes('grade') || combined.includes('badge')) return 'certificate';
    if (combined.includes('announcement') || combined.includes('notice') || combined.includes('broadcast')) return 'announcement';
    if (combined.includes('message') || combined.includes('reply') || combined.includes('discussion') || combined.includes('comment')) return 'discussion';
    return 'system';
  }

  getNotificationIcon(category: string): string {
    switch (category) {
      case 'quiz': return 'sparkles';
      case 'certificate': return 'award';
      case 'announcement': return 'megaphone';
      case 'discussion': return 'message-square';
      default: return 'bell';
    }
  }

  loadNotifications(): void {
    if (!this.authService.currentUser) return;
    this.notificationApi.getNotifications().subscribe({
      next: (dtos) => {
        this.notifications = dtos.map(dto => ({
          id: dto.id,
          title: dto.title || 'Notification',
          body: dto.body,
          time: timeAgo(dto.createdAt),
          read: dto.read,
          category: (dto.category as any) || this.detectCategory(dto.title, dto.body)
        }));
        const unreadCount = dtos.filter(d => !d.read).length;
        this.notificationState.setCount(this.role, unreadCount);
      },
      error: () => {
        this.notifications = [];
      }
    });
  }

  onNotificationClick(n: TopbarNotificationItem): void {
    if (!n.read) {
      n.read = true;
      this.notificationState.markOneRead(this.role);
      this.notificationApi.markRead(n.id).subscribe({ error: () => {} });
    }
    this.closeNotifications();
  }

  get role(): 'student' | 'teacher' | 'admin' {
    if (this.router.url.startsWith('/teacher')) return 'teacher';
    if (this.router.url.startsWith('/admin')) return 'admin';
    return 'student';
  }

  get userName(): string {
    return this.authService.currentUser?.fullName || 'User';
  }

  get userEmail(): string {
    return this.authService.currentUser?.email || '';
  }

  get userInitials(): string {
    const name = this.userName;
    return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase() || 'U';
  }

  get unreadNotifications(): number {
    return this.notificationState.count(this.role);
  }

  getMessagesRoute(): string {
    const url = this.router.url;
    if (url.startsWith('/student')) return '/student/messages';
    if (url.startsWith('/teacher')) return '/teacher/messages';
    if (url.startsWith('/admin')) return '/admin/messages';
    return '/login';
  }

  getRoleRoute(page: 'announcements' | 'notifications' | 'profile' | 'settings'): string {
    return `/${this.role}/${page}`;
  }

  toggleMenu() {
    this.isMenuOpen = !this.isMenuOpen;
    if (this.isMenuOpen) this.isNotificationsOpen = false;
  }

  closeMenu() {
    this.isMenuOpen = false;
  }

  toggleNotifications() {
    this.isNotificationsOpen = !this.isNotificationsOpen;
    if (this.isNotificationsOpen) this.isMenuOpen = false;
  }

  closeNotifications() {
    this.isNotificationsOpen = false;
  }

  markAllAsRead() {
    this.notificationState.markAllRead(this.role);
    this.notifications.forEach(n => n.read = true);
    this.notificationApi.markAllRead().subscribe({ error: () => {} });
  }

  signOut() {
    this.authService.logout();
    this.closeMenu();
    this.router.navigate(['/login']);
  }
}
