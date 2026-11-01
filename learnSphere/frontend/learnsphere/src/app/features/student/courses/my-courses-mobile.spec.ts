import { courseRowState, filterCounts } from './my-courses-mobile';

describe('my-courses-mobile helpers (approved 360 px My Courses)', () => {
  it('shows lessons done and the next lesson to resume', () => {
    expect(courseRowState({ lectureCount: 12, completedLecturesCount: 5, progressPercent: 42 })).toEqual({
      percent: 42, progress: '5 of 12', action: 'Resume lesson 6', completed: false
    });
  });

  it('offers to start a course with nothing watched', () => {
    expect(courseRowState({ lectureCount: 10, completedLecturesCount: 0, progressPercent: 0 })).toMatchObject({
      progress: '0 of 10', action: 'Start course', completed: false
    });
  });

  it('marks a finished course complete, by flag or by 100 %', () => {
    expect(courseRowState({ lectureCount: 8, completedLecturesCount: 8, completed: true })).toMatchObject({
      percent: 100, action: 'Completed', completed: true
    });
    expect(courseRowState({ lectureCount: 8, completedLecturesCount: 7, progressPercent: 100 })).toMatchObject({ completed: true });
  });

  it('never points past the last lesson and survives missing counts', () => {
    expect(courseRowState({ lectureCount: 4, completedLecturesCount: 4, progressPercent: 96 }).action).toBe('Resume lesson 4');
    expect(courseRowState({})).toEqual({ percent: 0, progress: '', action: 'Start course', completed: false });
  });

  it('counts courses per filter', () => {
    expect(filterCounts([
      { progressPercent: 42 },
      { completed: true },
      { progressPercent: 0 },
      { progressPercent: 100 }
    ])).toEqual({ ALL: 4, IN_PROGRESS: 2, COMPLETED: 2 });
  });
});
