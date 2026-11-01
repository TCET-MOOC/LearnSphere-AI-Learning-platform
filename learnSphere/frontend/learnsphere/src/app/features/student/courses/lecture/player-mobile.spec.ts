import { COMPLETION_THRESHOLD, completeAction, lessonPosition, nextLectureId, recordedPercent } from './player-mobile';

describe('player-mobile helpers (approved 360 px player)', () => {
  describe('recordedPercent', () => {
    it('uses the server percentage when it is ahead of this session', () => {
      expect(recordedPercent({ serverPercent: 62, watchedSeconds: 30, durationSeconds: 900, completed: false })).toBe(62);
    });

    it('uses honest watch time from this session when it is ahead of the server', () => {
      expect(recordedPercent({ serverPercent: 10, watchedSeconds: 450, durationSeconds: 900, completed: false })).toBe(50);
    });

    it('is 100 once the lecture is complete', () => {
      expect(recordedPercent({ serverPercent: 40, watchedSeconds: 0, durationSeconds: 900, completed: true })).toBe(100);
    });

    it('never goes above 100 or below 0 and survives an unknown duration', () => {
      expect(recordedPercent({ serverPercent: 140, watchedSeconds: 0, durationSeconds: 0, completed: false })).toBe(100);
      expect(recordedPercent({ serverPercent: undefined, watchedSeconds: 500, durationSeconds: 0, completed: false })).toBe(0);
      expect(recordedPercent({ serverPercent: -5, watchedSeconds: -10, durationSeconds: 900, completed: false })).toBe(0);
    });
  });

  describe('lessonPosition / nextLectureId', () => {
    const lectures = [{ id: 7 }, { id: 3 }, { id: 9 }];

    it('reports the 1-based position and total', () => {
      expect(lessonPosition(lectures, 3)).toEqual({ index: 2, total: 3 });
    });

    it('returns index 0 for an unknown lecture', () => {
      expect(lessonPosition(lectures, 99)).toEqual({ index: 0, total: 3 });
    });

    it('finds the next lecture in course order, and none after the last', () => {
      expect(nextLectureId(lectures, 7)).toBe(3);
      expect(nextLectureId(lectures, 9)).toBeNull();
      expect(nextLectureId(lectures, 99)).toBeNull();
    });
  });

  describe('completeAction', () => {
    it('waits for 90 % recorded before offering completion', () => {
      expect(COMPLETION_THRESHOLD).toBe(90);
      expect(completeAction({ completed: false, hasQuiz: false, percent: 89 })).toEqual({ kind: 'locked', label: 'Mark complete at 90%', enabled: false });
      expect(completeAction({ completed: false, hasQuiz: false, percent: 90 })).toEqual({ kind: 'complete', label: 'Mark complete', enabled: true });
    });

    it('sends the student to the quiz when the lecture has one', () => {
      expect(completeAction({ completed: false, hasQuiz: true, percent: 10 })).toEqual({ kind: 'quiz', label: 'Take quiz', enabled: true });
    });

    it('shows a completed lecture as done (tapping undoes it)', () => {
      expect(completeAction({ completed: true, hasQuiz: true, percent: 100 })).toEqual({ kind: 'done', label: 'Completed', enabled: true });
    });
  });
});
