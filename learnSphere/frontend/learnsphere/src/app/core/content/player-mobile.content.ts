/** Copy for the approved 360 px lesson player (focus mode). */
export const PLAYER_MOBILE_COPY = {
  back: 'Back to course',
  lessonOf: (index: number, total: number) => `Lesson ${index} of ${total}`,
  progress: (percent: number, threshold: number) => `${percent}% watched. Completes at ${threshold}%.`,
  completedLine: 'Lesson complete.',
  progressLabel: 'Recorded watch progress',
  bookmark: 'Bookmark',
  bookmarked: 'Bookmarked',
  addNote: 'Add note',
  quiz: 'Quiz',
  certificate: 'Certificate',
  tabUpNext: 'Up next',
  tabNotes: 'Notes',
  tabDiscussion: 'Discussion',
  tabAssignments: 'Assignments',
  playing: 'Playing',
  nextLesson: 'Next lesson',
  courseOverview: 'Course overview',
  complete: {
    done: 'Completed',
    quiz: 'Take quiz',
    complete: 'Mark complete',
    locked: (threshold: number) => `Mark complete at ${threshold}%`
  }
} as const;
