/** Copy for the approved 360 px student Home. */
export const HOME_MOBILE_COPY = {
  greeting: (salutation: string, name: string) => (name ? `${salutation}, ${name}` : salutation),
  resumeTitle: 'Pick up where you left off',
  resumeContext: (course: string, index: number, total: number) =>
    index > 0 ? `${course}, lesson ${index} of ${total}` : course,
  resumeAction: 'Resume lesson',
  progressLabel: 'Lesson progress',
  allDoneTitle: 'All your courses are complete',
  allDoneBody: 'Find your next course in the catalogue.',
  explore: 'Explore courses',
  comingUp: 'Coming up',
  nothingComingUp: 'No classes or tests coming up.',
  today: 'Today',
  liveNow: 'Live',
  liveDetail: (course?: string) => (course ? `Live class, ${course}` : 'Live class'),
  testDetail: (questions?: number, minutes?: number) =>
    ['Test', questions ? `${questions} questions` : '', minutes ? `${minutes} min` : ''].filter(Boolean).join(', '),
  join: 'Join',
  open: 'Open',
  myCourses: 'My Courses',
  seeAll: (n: number) => `See all ${n}`,
  completed: 'Completed',
  noCourses: 'You are not enrolled in a course yet.'
} as const;
