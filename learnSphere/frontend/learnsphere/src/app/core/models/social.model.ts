/**
 * Shared models for the Social domain: Messaging, Announcements, Notifications,
 * Discussion, and Leaderboard. Mirrors the backend response DTOs 1:1 so all three
 * portals (student/teacher/admin) can consume the same shapes.
 */

export interface ConversationDto {
  id: number;
  otherUserId: number;
  otherUserName: string;
  otherUserAvatarUrl?: string;
  otherUserRole?: 'student' | 'teacher' | 'admin';
  courseId?: number;
  courseName?: string;
  lastMessage?: string;
  lastMessageAt?: string;
  createdAt: string;
  unreadCount: number;
}

export interface MessageDto {
  id: number;
  conversationId: number;
  senderId: number;
  senderName: string;
  senderAvatarUrl?: string;
  text: string;
  sentAt: string;
  readAt?: string;
}

export interface StartConversationRequest {
  otherUserId: number;
  courseId?: number;
}

export type AnnouncementAudience = 'ALL' | 'STUDENT' | 'TEACHER' | 'ADMIN';

export interface AnnouncementDto {
  id: number;
  title: string;
  body: string;
  authorId?: number;
  authorName?: string;
  category?: string;
  pinned: boolean;
  audience: AnnouncementAudience;
  createdAt: string;
}

export interface AnnouncementRequest {
  title: string;
  body: string;
  category?: string;
  audience?: AnnouncementAudience;
  pinned?: boolean;
}

export interface AppNotificationDto {
  id: number;
  title: string;
  body: string;
  category?: string;
  read: boolean;
  createdAt: string;
}

export interface DiscussionPostDto {
  id: number;
  courseId?: number;
  lectureId?: number;
  authorId: number;
  authorName: string;
  authorAvatarUrl?: string;
  body: string;
  parentPostId?: number;
  createdAt: string;
  flagged: boolean;
  replies: DiscussionPostDto[];
}

export interface DiscussionPostRequest {
  courseId?: number;
  lectureId?: number;
  parentPostId?: number;
  body: string;
}

export interface LeaderboardEntryDto {
  rank: number;
  studentId: number;
  name: string;
  avatarUrl?: string;
  points: number;
}

export type LeaderboardScope = 'global' | 'college';
