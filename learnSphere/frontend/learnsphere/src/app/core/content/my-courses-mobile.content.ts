/** Copy for the approved 360 px My Courses. */
export const MY_COURSES_MOBILE_COPY = {
  filterLabel: 'Filter courses',
  chips: { ALL: 'All', IN_PROGRESS: 'In progress', COMPLETED: 'Completed' },
  progress: (done: number, total: number) => `${done} of ${total}`,
  resume: (lesson: number) => `Resume lesson ${lesson}`,
  start: 'Start course',
  completed: 'Completed',
  details: (title: string) => `Course details: ${title}`,
  findNew: 'Find a new course',
  searching: (q: string) => `Results for "${q}"`,
  clearSearch: 'Clear',
  empty: 'No courses here yet.',
  continueWatched: (percent: number) => `${percent}% watched`,
  continueLabel: 'Continue lesson'
} as const;
