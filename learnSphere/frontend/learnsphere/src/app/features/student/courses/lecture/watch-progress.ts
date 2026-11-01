/**
 * Watch-progress reporting (post-W6 audit, AC-12 / BR-25).
 * The player only reports honest watch time (normal playback, seeks excluded); the server
 * derives the percentage from the content's length and alone decides completion.
 */
export const REPORT_EVERY_SECONDS = 15;

export interface WatchProgressPayload {
  content?: { contentId: number };
  lecture?: { id: number };
  secondsWatched: number;
}

export function progressPayload(target: { id: number; contentId?: number | null }, watched: number): WatchProgressPayload {
  const secondsWatched = Math.max(0, Math.floor(watched || 0));
  return target.contentId
    ? { content: { contentId: target.contentId }, secondsWatched }
    : { lecture: { id: target.id }, secondsWatched };
}

/** Report after every 15 s of new watching, and at the end when at least a second is new. */
export function shouldReport(watched: number, lastReported: number, ended: boolean): boolean {
  const fresh = Math.floor(watched) - lastReported;
  return ended ? fresh >= 1 : fresh >= REPORT_EVERY_SECONDS;
}
