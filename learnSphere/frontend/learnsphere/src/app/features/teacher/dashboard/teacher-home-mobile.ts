/** Approved 360 px teacher Home: pure rules for the next class, waiting replies and course status. */
import { TEACHER_HOME_MOBILE_COPY } from '@core/content/teacher-home-mobile.content';
import { getInitials } from '@core/utils/avatar.util';
import { timeAgo } from '@core/utils/time.util';

export interface SessionLike {
  id: number;
  title: string;
  courseTitle?: string;
  status?: string;
  scheduledAt?: string | null;
}

export interface NextClass {
  id: number;
  title: string;
  course: string;
  when: string;
  live: boolean;
}

const HOUR = 60 * 60 * 1000;
const sameDay = (a: Date, b: Date) =>
  a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
const clock = (d: Date) => `${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`;
const dayName = (d: Date) => d.toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short' });

/** A live class first; otherwise the soonest scheduled class (up to an hour late). */
export function nextClass(sessions: ReadonlyArray<SessionLike>, now: Date = new Date()): NextClass | null {
  const copy = TEACHER_HOME_MOBILE_COPY;
  const live = sessions.find((s) => s.status === 'LIVE');
  if (live) return { id: live.id, title: live.title, course: live.courseTitle ?? '', when: copy.liveNow, live: true };
  const upcoming = sessions
    .filter((s) => s.status === 'SCHEDULED' && s.scheduledAt && new Date(s.scheduledAt).getTime() >= now.getTime() - HOUR)
    .sort((a, b) => new Date(a.scheduledAt!).getTime() - new Date(b.scheduledAt!).getTime())[0];
  if (!upcoming) return null;
  const at = new Date(upcoming.scheduledAt!);
  return {
    id: upcoming.id,
    title: upcoming.title,
    course: upcoming.courseTitle ?? '',
    when: sameDay(at, now) ? copy.nextToday(clock(at)) : copy.nextLater(dayName(at), clock(at)),
    live: false
  };
}

export interface ConversationLike {
  id: number;
  otherUserName: string;
  lastMessage?: string;
  lastMessageAt?: string;
  unreadCount: number;
}

export interface AwaitingItem {
  id: number;
  name: string;
  initials: string;
  text: string;
  ago: string;
  route: string;
  query: { c: number };
}

/** Conversations with unread messages, newest first, capped; `total` counts them all. */
export function awaitingReply(conversations: ReadonlyArray<ConversationLike>, limit = 3): { items: AwaitingItem[]; total: number } {
  const unread = conversations
    .filter((c) => (c.unreadCount ?? 0) > 0)
    .sort((a, b) => new Date(b.lastMessageAt ?? 0).getTime() - new Date(a.lastMessageAt ?? 0).getTime());
  return {
    total: unread.length,
    items: unread.slice(0, limit).map((c) => ({
      id: c.id,
      name: c.otherUserName,
      initials: getInitials(c.otherUserName),
      text: c.lastMessage ?? '',
      ago: timeAgo(c.lastMessageAt),
      route: '/teacher/messages',
      query: { c: c.id }
    }))
  };
}

export function courseStatusLabel(status: string | null | undefined): string {
  return status ? TEACHER_HOME_MOBILE_COPY.status[status] ?? '' : '';
}
