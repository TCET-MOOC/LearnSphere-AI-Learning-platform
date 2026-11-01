import { REPORT_EVERY_SECONDS, progressPayload, shouldReport } from './watch-progress';

/** Post-W6 audit (BR-25): the player reports honest watch time; the server decides completion. */
describe('watch progress reporting', () => {
  it('reports by canonical content id when the lecture has one', () => {
    expect(progressPayload({ id: 7, contentId: 42 }, 95.7)).toEqual({ content: { contentId: 42 }, secondsWatched: 95 });
  });

  it('falls back to the legacy lecture id', () => {
    expect(progressPayload({ id: 7, contentId: null }, 12)).toEqual({ lecture: { id: 7 }, secondsWatched: 12 });
  });

  it('never sends a negative or fractional number of seconds', () => {
    expect(progressPayload({ id: 7 }, -3).secondsWatched).toBe(0);
  });

  it(`reports after every ${REPORT_EVERY_SECONDS} s of new watching, and once more at the end`, () => {
    expect(REPORT_EVERY_SECONDS).toBe(15);
    expect(shouldReport(14.9, 0, false)).toBe(false);
    expect(shouldReport(15, 0, false)).toBe(true);
    expect(shouldReport(29, 15, false)).toBe(false);
    expect(shouldReport(31, 15, false)).toBe(true);
    expect(shouldReport(31.4, 31, true)).toBe(false); // nothing new: under a whole second
    expect(shouldReport(33, 31, true)).toBe(true);
  });
});
