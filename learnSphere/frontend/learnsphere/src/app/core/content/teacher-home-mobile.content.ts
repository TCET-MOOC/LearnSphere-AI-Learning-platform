/** Copy for the approved 360 px teacher Home. */
export const TEACHER_HOME_MOBILE_COPY = {
  liveNow: 'Live now',
  nextToday: (time: string) => `Your next class, today at ${time}`,
  nextLater: (day: string, time: string) => `Your next class, ${day} at ${time}`,
  noClass: 'No classes scheduled.',
  openLive: 'Open live classes',
  schedule: 'Schedule a class',
  awaitingTitle: 'Waiting for your reply',
  awaitingAll: (n: number) => `All ${n}`,
  nothingAwaiting: 'No messages waiting.',
  coursesTitle: 'Courses',
  manageCourses: 'Manage',
  noCourses: 'No courses yet.',
  status: { DRAFT: 'Draft', PENDING: 'In review', LIVE: 'Live', ARCHIVED: 'Archived' } as Record<string, string>
} as const;
