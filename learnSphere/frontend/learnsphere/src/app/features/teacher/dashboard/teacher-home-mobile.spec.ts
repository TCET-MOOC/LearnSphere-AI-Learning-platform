import { awaitingReply, courseStatusLabel, nextClass } from './teacher-home-mobile';

describe('teacher-home-mobile helpers (approved 360 px teacher Home)', () => {
  const now = new Date('2026-11-01T10:00:00');

  describe('nextClass', () => {
    it('picks a live class first, then the soonest scheduled one', () => {
      const sessions = [
        { id: 1, title: 'Later', courseTitle: 'Maths', status: 'SCHEDULED', scheduledAt: '2026-11-03T12:00:00' },
        { id: 2, title: 'Today', courseTitle: 'Maths', status: 'SCHEDULED', scheduledAt: '2026-11-01T18:00:00' }
      ];
      expect(nextClass(sessions, now)).toMatchObject({ id: 2, title: 'Today', when: 'Your next class, today at 18:00', live: false });
      expect(nextClass([...sessions, { id: 3, title: 'Now', status: 'LIVE', scheduledAt: '2026-11-01T09:00:00' }], now))
        .toMatchObject({ id: 3, when: 'Live now', live: true });
    });

    it('names the day for a later class and ignores ended or stale ones', () => {
      expect(nextClass([{ id: 1, title: 'Later', status: 'SCHEDULED', scheduledAt: '2026-11-07T18:30:00' }], now)?.when)
        .toBe('Your next class, Sat 7 Nov at 18:30');
      expect(nextClass([
        { id: 1, title: 'Ended', status: 'ENDED', scheduledAt: '2026-11-01T11:00:00' },
        { id: 2, title: 'Stale', status: 'SCHEDULED', scheduledAt: '2026-10-01T11:00:00' }
      ], now)).toBeNull();
    });
  });

  describe('awaitingReply', () => {
    it('lists unread conversations, newest first, with a deep link and the total', () => {
      const result = awaitingReply([
        { id: 1, otherUserName: 'Priya Kulkarni', lastMessage: 'Next live?', lastMessageAt: '2026-11-01T09:00:00', unreadCount: 1 },
        { id: 2, otherUserName: 'Raj Shah', lastMessage: 'Lecture 8?', lastMessageAt: '2026-11-01T09:50:00', unreadCount: 2 },
        { id: 3, otherUserName: 'Read Already', lastMessage: 'Thanks', lastMessageAt: '2026-11-01T09:55:00', unreadCount: 0 }
      ], 3);
      expect(result.total).toBe(2);
      expect(result.items.map((i) => i.name)).toEqual(['Raj Shah', 'Priya Kulkarni']);
      expect(result.items[0]).toMatchObject({ initials: 'RS', text: 'Lecture 8?', route: '/teacher/messages', query: { c: 2 } });
    });

    it('caps the list but keeps the full total', () => {
      const many = Array.from({ length: 5 }, (_, i) => ({ id: i, otherUserName: `S ${i}`, unreadCount: 1, lastMessageAt: `2026-11-01T0${i}:00:00` }));
      const result = awaitingReply(many, 3);
      expect(result.items.length).toBe(3);
      expect(result.total).toBe(5);
    });
  });

  it('labels course statuses in plain words', () => {
    expect(courseStatusLabel('DRAFT')).toBe('Draft');
    expect(courseStatusLabel('PENDING')).toBe('In review');
    expect(courseStatusLabel('LIVE')).toBe('Live');
    expect(courseStatusLabel('ARCHIVED')).toBe('Archived');
    expect(courseStatusLabel(undefined)).toBe('');
  });
});
