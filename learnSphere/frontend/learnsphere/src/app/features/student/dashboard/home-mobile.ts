/**
 * Approved 360 px student Home: pure rules for the resume card and the "Coming up" list.
 * Only real data is shown; nothing is invented when a field is missing.
 */
import { HOME_MOBILE_COPY } from '@core/content/home-mobile.content';

export interface ResumableCourse {
  id: number;
  progressPercent?: number | null;
  completed?: boolean | null;
}

const isComplete = (c: ResumableCourse) => !!c.completed || (c.progressPercent ?? 0) >= 100;

/** The unfinished course with the most progress (the API has no "last opened" date). */
export function pickResumeCourse<T extends ResumableCourse>(courses: ReadonlyArray<T>): T | null {
  const open = courses.filter((c) => !isComplete(c));
  if (open.length === 0) return null;
  return open.reduce((best, c) => ((c.progressPercent ?? 0) > (best.progressPercent ?? 0) ? c : best));
}

export function firstName(fullName: string | null | undefined): string {
  return (fullName ?? '').trim().split(/\s+/)[0] ?? '';
}

export interface LiveSessionLike {
  id: number;
  title: string;
  courseTitle?: string;
  status?: string;
  scheduledAt?: string | null;
}

export interface AssessmentLike {
  id: number;
  title: string;
  questionCount?: number;
  durationMinutes?: number;
  scheduledAt?: string | null;
}

export interface ComingUpItem {
  kind: 'live' | 'test';
  title: string;
  detail: string;
  /** "Today", "Live", a short weekday, or '' when there is no date */
  day: string;
  /** "18:00", "7 Nov", or '' */
  time: string;
  live: boolean;
  action: string;
  route: string;
  sortKey: number;
}

const HOUR = 60 * 60 * 1000;
const sameDay = (a: Date, b: Date) =>
  a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
const timeOf = (d: Date) => `${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`;
const weekday = (d: Date) => d.toLocaleDateString('en-GB', { weekday: 'short' });
const dayMonth = (d: Date) => d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short' });

/** Live and upcoming classes, then tests; soonest first, undated last, at most three. */
export function comingUp(
  sessions: ReadonlyArray<LiveSessionLike>,
  assessments: ReadonlyArray<AssessmentLike>,
  now: Date = new Date(),
  limit = 3
): ComingUpItem[] {
  const copy = HOME_MOBILE_COPY;
  const items: ComingUpItem[] = [];

  for (const s of sessions) {
    const at = s.scheduledAt ? new Date(s.scheduledAt) : null;
    const live = s.status === 'LIVE';
    if (!live && (s.status !== 'SCHEDULED' || !at || at.getTime() < now.getTime() - HOUR)) continue;
    items.push({
      kind: 'live',
      title: s.title,
      detail: copy.liveDetail(s.courseTitle),
      day: live ? copy.liveNow : sameDay(at!, now) ? copy.today : weekday(at!),
      time: live ? '' : sameDay(at!, now) ? timeOf(at!) : dayMonth(at!),
      live,
      action: live ? copy.join : copy.open,
      route: `/student/live/${s.id}`,
      sortKey: live ? Number.NEGATIVE_INFINITY : at!.getTime()
    });
  }

  for (const a of assessments) {
    const at = a.scheduledAt ? new Date(a.scheduledAt) : null;
    const dated = !!at && at.getTime() >= now.getTime();
    items.push({
      kind: 'test',
      title: a.title,
      detail: copy.testDetail(a.questionCount, a.durationMinutes),
      day: dated ? (sameDay(at!, now) ? copy.today : weekday(at!)) : '',
      time: dated ? (sameDay(at!, now) ? timeOf(at!) : dayMonth(at!)) : '',
      live: false,
      action: copy.open,
      route: `/student/assessments/${a.id}`,
      sortKey: dated ? at!.getTime() : Number.POSITIVE_INFINITY
    });
  }

  return items.sort((x, y) => x.sortKey - y.sortKey).slice(0, limit);
}
