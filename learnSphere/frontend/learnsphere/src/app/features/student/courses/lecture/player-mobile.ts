/**
 * Approved 360 px lesson player: pure rules behind the progress line and the sticky bar.
 * The server still decides completion (BR-25, 90 % recorded); these only drive what the
 * student sees and whether the button is offered.
 */
import { PLAYER_MOBILE_COPY } from '@core/content/player-mobile.content';

export const COMPLETION_THRESHOLD = 90;

const clampPercent = (n: number) => Math.max(0, Math.min(100, Math.round(n)));

/** Recorded progress: the server's figure or this session's honest watch time, whichever is ahead. */
export function recordedPercent(input: {
  serverPercent: number | null | undefined;
  watchedSeconds: number;
  durationSeconds: number;
  completed: boolean;
}): number {
  if (input.completed) return 100;
  const server = clampPercent(input.serverPercent ?? 0);
  const session = input.durationSeconds > 0 ? clampPercent((input.watchedSeconds / input.durationSeconds) * 100) : 0;
  return Math.max(server, session);
}

/** 1-based position of the lecture in course order (0 when it is not in the list). */
export function lessonPosition(lectures: ReadonlyArray<{ id: number }>, currentId: number): { index: number; total: number } {
  return { index: lectures.findIndex((l) => l.id === currentId) + 1, total: lectures.length };
}

export function nextLectureId(lectures: ReadonlyArray<{ id: number }>, currentId: number): number | null {
  const i = lectures.findIndex((l) => l.id === currentId);
  return i >= 0 && i + 1 < lectures.length ? lectures[i + 1].id : null;
}

export type CompleteActionKind = 'done' | 'quiz' | 'complete' | 'locked';

export interface CompleteAction {
  kind: CompleteActionKind;
  label: string;
  enabled: boolean;
}

export function completeAction(state: { completed: boolean; hasQuiz: boolean; percent: number }): CompleteAction {
  const copy = PLAYER_MOBILE_COPY.complete;
  if (state.completed) return { kind: 'done', label: copy.done, enabled: true };
  if (state.hasQuiz) return { kind: 'quiz', label: copy.quiz, enabled: true };
  if (state.percent >= COMPLETION_THRESHOLD) return { kind: 'complete', label: copy.complete, enabled: true };
  return { kind: 'locked', label: copy.locked(COMPLETION_THRESHOLD), enabled: false };
}
