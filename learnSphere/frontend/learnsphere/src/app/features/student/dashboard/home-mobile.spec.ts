import { comingUp, firstName, pickResumeCourse } from './home-mobile';

describe('home-mobile helpers (approved 360 px Home)', () => {
  describe('pickResumeCourse', () => {
    it('prefers the unfinished course with the most progress', () => {
      const courses = [
        { id: 1, progressPercent: 10 },
        { id: 2, progressPercent: 100, completed: true },
        { id: 3, progressPercent: 55 }
      ];
      expect(pickResumeCourse(courses)?.id).toBe(3);
    });

    it('falls back to an unstarted course, and to nothing when all are complete', () => {
      expect(pickResumeCourse([{ id: 4, progressPercent: 0 }])?.id).toBe(4);
      expect(pickResumeCourse([{ id: 5, progressPercent: 100 }, { id: 6, completed: true }])).toBeNull();
      expect(pickResumeCourse([])).toBeNull();
    });
  });

  describe('firstName', () => {
    it('uses the first word of the full name, or nothing', () => {
      expect(firstName('Aarav Sharma')).toBe('Aarav');
      expect(firstName('  ')).toBe('');
      expect(firstName(undefined)).toBe('');
    });
  });

  describe('comingUp', () => {
    const now = new Date('2026-11-01T10:00:00');

    it('lists live and upcoming classes and tests, soonest first, at most three', () => {
      const items = comingUp(
        [
          { id: 1, title: 'Later class', courseTitle: 'ML', status: 'SCHEDULED', scheduledAt: '2026-11-03T12:00:00' },
          { id: 2, title: 'Today class', courseTitle: 'ML', status: 'SCHEDULED', scheduledAt: '2026-11-01T18:00:00' },
          { id: 3, title: 'Live now', courseTitle: 'ML', status: 'LIVE', scheduledAt: '2026-11-01T09:30:00' }
        ],
        [{ id: 9, title: 'Check', questionCount: 3, durationMinutes: 30, scheduledAt: '2026-11-07T09:00:00' }],
        now
      );
      expect(items.map((i) => i.title)).toEqual(['Live now', 'Today class', 'Later class']);
      expect(items[0]).toMatchObject({ kind: 'live', live: true, route: '/student/live/3' });
      expect(items[1]).toMatchObject({ kind: 'live', live: false, day: 'Today', route: '/student/live/2' });
    });

    it('drops ended sessions and scheduled sessions more than an hour past', () => {
      const items = comingUp(
        [
          { id: 1, title: 'Ended', status: 'ENDED', scheduledAt: '2026-11-01T11:00:00' },
          { id: 2, title: 'Stale', status: 'SCHEDULED', scheduledAt: '2026-10-20T11:00:00' }
        ],
        [],
        now
      );
      expect(items).toEqual([]);
    });

    it('shows tests with their facts and puts undated tests after dated items', () => {
      const items = comingUp(
        [{ id: 2, title: 'Class', status: 'SCHEDULED', scheduledAt: '2026-11-02T12:00:00' }],
        [
          { id: 8, title: 'Undated', questionCount: 5, durationMinutes: 20 },
          { id: 9, title: 'Dated', questionCount: 3, durationMinutes: 30, scheduledAt: '2026-11-07T09:00:00' }
        ],
        now
      );
      expect(items.map((i) => i.title)).toEqual(['Class', 'Dated', 'Undated']);
      expect(items[1]).toMatchObject({ kind: 'test', detail: 'Test, 3 questions, 30 min', route: '/student/assessments/9' });
      expect(items[2].day).toBe('');
    });
  });
});
