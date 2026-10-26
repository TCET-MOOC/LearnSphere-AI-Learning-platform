/**
 * Deterministic API doubles for the visual baseline (DS-01 / W1-12).
 *
 * The harness never talks to a real backend: every `/api/**` request is
 * answered from the fixtures below, so a screenshot depends only on the
 * frontend code. Fixtures follow the shapes in `src/app/core/models`.
 *
 * Cross-origin is real here (the app runs on 127.0.0.1:4200 and `environment.apiUrl`
 * points at localhost:8080 / 8081), so preflights are answered explicitly.
 */
import type { Page } from '@playwright/test';

const CORS_HEADERS = {
  'access-control-allow-origin': '*',
  'access-control-allow-methods': 'GET,POST,PUT,PATCH,DELETE,OPTIONS',
  'access-control-allow-headers': '*',
  'access-control-max-age': '600',
};

const ISO = '2026-10-25T09:00:00Z';

export const VISUAL_USERS = {
  public: null,
  student: {
    id: 1,
    fullName: 'Aarav Sharma',
    email: 'student@learnsphere.test',
    role: 'STUDENT',
    collegeId: 1,
    collegeName: 'IIT Bombay',
    collegeVerificationStatus: 'VERIFIED',
    departmentId: 1,
    departmentName: 'Computer Science',
    avatarUrl: '',
    bio: 'Second-year CS student learning applied machine learning.',
    status: 'ACTIVE',
    twoFactorEnabled: false,
    bankAccountLinked: false,
    royaltyBalance: 0,
    attendanceScore: 92,
    leaderboardPoints: 1840,
    lastActiveAt: ISO,
    createdAt: '2026-01-15T09:00:00Z',
  },
  teacher: {
    id: 2,
    fullName: 'Meera Iyer',
    email: 'teacher@learnsphere.test',
    role: 'TEACHER',
    collegeId: 1,
    collegeName: 'IIT Bombay',
    collegeVerificationStatus: 'VERIFIED',
    departmentId: 1,
    departmentName: 'Computer Science',
    avatarUrl: '',
    bio: 'Faculty, applied machine learning.',
    status: 'ACTIVE',
    twoFactorEnabled: false,
    bankAccountLinked: true,
    royaltyBalance: 48250,
    attendanceScore: 100,
    leaderboardPoints: 0,
    lastActiveAt: ISO,
    createdAt: '2025-08-01T09:00:00Z',
  },
  admin: {
    id: 3,
    fullName: 'Rohan Gupta',
    email: 'admin@learnsphere.test',
    role: 'ADMIN',
    collegeId: 1,
    collegeName: 'IIT Bombay',
    collegeVerificationStatus: 'VERIFIED',
    departmentId: 1,
    departmentName: 'Administration',
    avatarUrl: '',
    bio: 'Platform administrator.',
    status: 'ACTIVE',
    twoFactorEnabled: true,
    bankAccountLinked: false,
    royaltyBalance: 0,
    attendanceScore: 100,
    leaderboardPoints: 0,
    lastActiveAt: ISO,
    createdAt: '2025-01-01T09:00:00Z',
  },
} as const;

const COURSES = [
  {
    id: 1,
    title: 'Applied Machine Learning',
    description: 'Build, evaluate and ship machine-learning systems end to end.',
    teacherId: 2,
    teacherName: 'Meera Iyer',
    department: 'Computer Science',
    thumbnail: '',
    status: 'LIVE',
    price: 1299,
    createdAt: '2026-06-01T09:00:00Z',
    lectureCount: 12,
    completedLecturesCount: 5,
    progressPercent: 42,
    completed: false,
  },
  {
    id: 2,
    title: 'Design Systems in Practice',
    description: 'Tokens, components and accessibility for product teams.',
    teacherId: 2,
    teacherName: 'Meera Iyer',
    department: 'Design',
    thumbnail: '',
    status: 'LIVE',
    price: 899,
    createdAt: '2026-07-12T09:00:00Z',
    lectureCount: 8,
    completedLecturesCount: 8,
    progressPercent: 100,
    completed: true,
  },
  {
    id: 3,
    title: 'Databases for Engineers',
    description: 'Relational modelling, indexing and transactions in PostgreSQL.',
    teacherId: 2,
    teacherName: 'Meera Iyer',
    department: 'Computer Science',
    thumbnail: '',
    status: 'PENDING',
    price: 0,
    createdAt: '2026-09-03T09:00:00Z',
    lectureCount: 10,
    completedLecturesCount: 0,
    progressPercent: 0,
    completed: false,
  },
];

const LECTURES = [
  {
    id: 1,
    courseId: 1,
    title: 'What machine learning actually solves',
    number: 1,
    videoUrl: '',
    duration: 754,
    isDownloadable: true,
    status: 'PUBLISHED',
  },
  {
    id: 2,
    courseId: 1,
    title: 'Features, labels and leakage',
    number: 2,
    videoUrl: '',
    duration: 912,
    isDownloadable: true,
    status: 'PUBLISHED',
  },
  {
    id: 3,
    courseId: 1,
    title: 'Evaluating a classifier',
    number: 3,
    videoUrl: '',
    duration: 1105,
    isDownloadable: false,
    status: 'PUBLISHED',
  },
];

const MODULES = [
  { moduleId: 1, courseId: 1, title: 'Foundations', position: 1, createdAt: ISO },
  { moduleId: 2, courseId: 1, title: 'Supervised learning', position: 2, createdAt: ISO },
];

const CONTENTS = [
  {
    contentId: 1,
    moduleId: 1,
    contentType: 'VIDEO',
    title: 'What machine learning actually solves',
    filePath: '',
    durationSeconds: 754,
    position: 1,
    uploadedAt: ISO,
  },
  {
    contentId: 2,
    moduleId: 1,
    contentType: 'PDF',
    title: 'Lecture 1 slides',
    filePath: '',
    durationSeconds: 0,
    position: 2,
    uploadedAt: ISO,
  },
  {
    contentId: 3,
    moduleId: 2,
    contentType: 'QUIZ',
    title: 'Supervised learning check',
    filePath: '',
    durationSeconds: 0,
    position: 1,
    uploadedAt: ISO,
  },
];

const ASSIGNMENTS = [
  {
    assignmentId: 1,
    moduleId: 1,
    title: 'Baseline classifier notebook',
    description: 'Train and evaluate a baseline model on the provided dataset.',
    maxMarks: 100,
    dueDate: '2026-11-08T18:00:00Z',
    createdAt: ISO,
  },
];

const NOTES = [
  {
    id: 1,
    courseId: 1,
    courseName: 'Applied Machine Learning',
    lectureId: 1,
    lectureLabel: 'Lec 1',
    timestampSeconds: 154,
    title: 'Definition of learning',
    content: 'A program learns when its performance on a task improves with experience.',
    tags: ['definition', 'exam'],
    createdAt: '2026-10-20T09:00:00Z',
  },
  {
    id: 2,
    courseId: 1,
    courseName: 'Applied Machine Learning',
    lectureId: 2,
    lectureLabel: 'Lec 2',
    timestampSeconds: 612,
    title: 'Leakage checklist',
    content: 'Split before scaling; never fit a transformer on the test fold.',
    tags: ['preprocessing'],
    createdAt: '2026-10-22T09:00:00Z',
  },
];

const BOOKMARKS = [
  {
    id: 1,
    lectureId: 1,
    lectureTitle: 'What machine learning actually solves',
    courseId: 1,
    courseName: 'Applied Machine Learning',
    timestampSeconds: 320,
    label: 'Formal definition',
    createdAt: '2026-10-19T09:00:00Z',
  },
  {
    id: 2,
    lectureId: 2,
    lectureTitle: 'Features, labels and leakage',
    courseId: 1,
    courseName: 'Applied Machine Learning',
    timestampSeconds: 845,
    label: 'Leakage example',
    createdAt: '2026-10-21T09:00:00Z',
  },
];

const CERTIFICATES = [
  {
    id: 1,
    courseId: 2,
    courseTitle: 'Design Systems in Practice',
    type: 'STANDARD',
    title: 'Certificate of Completion',
    verificationCode: 'LS-2026-0001',
    studentName: 'Aarav Sharma',
    instructorName: 'Meera Iyer',
    grade: 'A',
    score: 92,
    issuedAt: '2026-10-01T09:00:00Z',
  },
];

const ANNOUNCEMENTS = [
  {
    id: 1,
    title: 'Mid-term schedule published',
    body: 'The assessment window opens on 2 November and closes on 6 November.',
    authorId: 2,
    authorName: 'Meera Iyer',
    category: 'Assessments',
    pinned: true,
    audience: 'STUDENT',
    createdAt: '2026-10-24T09:00:00Z',
  },
  {
    id: 2,
    title: 'New lecture on model evaluation',
    body: 'Lecture 3 is live, with the notebook attached to the module.',
    authorId: 2,
    authorName: 'Meera Iyer',
    category: 'Course update',
    pinned: false,
    audience: 'ALL',
    createdAt: '2026-10-23T09:00:00Z',
  },
];

const NOTIFICATIONS = [
  {
    id: 1,
    title: 'Assignment graded',
    body: 'Your baseline classifier notebook was graded.',
    category: 'Assessment',
    read: false,
    createdAt: '2026-10-24T10:00:00Z',
  },
  {
    id: 2,
    title: 'New announcement',
    body: 'Mid-term schedule published.',
    category: 'Course',
    read: false,
    createdAt: '2026-10-24T09:30:00Z',
  },
  {
    id: 3,
    title: 'Certificate issued',
    body: 'Your Design Systems certificate is ready to download.',
    category: 'Certificate',
    read: true,
    createdAt: '2026-10-20T09:00:00Z',
  },
];

const CONVERSATIONS = [
  {
    id: 1,
    otherUserId: 2,
    otherUserName: 'Meera Iyer',
    otherUserAvatarUrl: '',
    otherUserRole: 'teacher',
    courseId: 1,
    courseName: 'Applied Machine Learning',
    lastMessage: 'Send me the notebook when it is ready.',
    lastMessageAt: '2026-10-24T11:00:00Z',
    createdAt: '2026-10-10T09:00:00Z',
    unreadCount: 1,
  },
  {
    id: 2,
    otherUserId: 5,
    otherUserName: 'Priya Nair',
    otherUserAvatarUrl: '',
    otherUserRole: 'student',
    courseId: 1,
    courseName: 'Applied Machine Learning',
    lastMessage: 'Thanks, that fixed the split.',
    lastMessageAt: '2026-10-22T15:00:00Z',
    createdAt: '2026-10-11T09:00:00Z',
    unreadCount: 0,
  },
];

const MESSAGES = [
  {
    id: 1,
    conversationId: 1,
    senderId: 1,
    senderName: 'Aarav Sharma',
    text: 'Should the validation split happen before scaling?',
    sentAt: '2026-10-24T10:40:00Z',
  },
  {
    id: 2,
    conversationId: 1,
    senderId: 2,
    senderName: 'Meera Iyer',
    text: 'Yes. Split first, then fit the scaler on the training fold only.',
    sentAt: '2026-10-24T10:52:00Z',
  },
  {
    id: 3,
    conversationId: 1,
    senderId: 2,
    senderName: 'Meera Iyer',
    text: 'Send me the notebook when it is ready.',
    sentAt: '2026-10-24T11:00:00Z',
  },
];

const LEADERBOARD = [
  { rank: 1, studentId: 9, name: 'Ishita Rao', avatarUrl: '', points: 2480 },
  { rank: 2, studentId: 1, name: 'Aarav Sharma', avatarUrl: '', points: 1840 },
  { rank: 3, studentId: 7, name: 'Kabir Menon', avatarUrl: '', points: 1725 },
  { rank: 4, studentId: 4, name: 'Priya Nair', avatarUrl: '', points: 1590 },
  { rank: 5, studentId: 6, name: 'Dev Patel', avatarUrl: '', points: 1430 },
];

const ASSESSMENTS = [
  {
    id: 1,
    courseId: 1,
    courseTitle: 'Applied Machine Learning',
    title: 'Supervised learning check',
    durationMinutes: 30,
    isRemedial: false,
    questionCount: 3,
    scheduledAt: '2026-10-20T09:00:00Z',
  },
  {
    id: 2,
    courseId: 2,
    courseTitle: 'Design Systems in Practice',
    title: 'Tokens and accessibility',
    durationMinutes: 20,
    isRemedial: true,
    questionCount: 2,
    scheduledAt: null,
  },
];

const ASSESSMENT_DETAIL = {
  id: 1,
  courseId: 1,
  title: 'Supervised learning check',
  durationMinutes: 30,
  securityPolicy: 'STANDARD',
  isRemedial: false,
  scheduledAt: '2026-10-20T09:00:00Z',
  questions: [
    {
      questionId: 1,
      body: 'Which split prevents test-set leakage?',
      questionType: 'MCQ',
      marks: 2,
      options: ['Scale, then split', 'Split, then scale', 'Scale both folds separately'],
    },
    {
      questionId: 2,
      body: 'A model with 99% training accuracy and 60% validation accuracy is overfitting.',
      questionType: 'TF',
      marks: 2,
      options: ['True', 'False'],
    },
    {
      questionId: 3,
      body: 'Explain why accuracy is a poor metric for imbalanced classes.',
      questionType: 'TEXT',
      marks: 4,
      options: null,
    },
  ],
};

const ATTEMPT_REVIEW = {
  attemptId: 1,
  testId: 1,
  testTitle: 'Supervised learning check',
  status: 'COMPLETED',
  score: 6,
  maxScore: 8,
  isRemedial: false,
  attemptedAt: '2026-10-20T10:00:00Z',
  answers: [
    {
      questionId: 1,
      questionBody: 'Which split prevents test-set leakage?',
      questionType: 'MCQ',
      marks: 2,
      options: ['Scale, then split', 'Split, then scale', 'Scale both folds separately'],
      correctAnswer: 'Split, then scale',
      studentAnswer: 'Split, then scale',
      marksAwarded: 2,
    },
    {
      questionId: 2,
      questionBody: 'A model with 99% training accuracy and 60% validation accuracy is overfitting.',
      questionType: 'TF',
      marks: 2,
      options: ['True', 'False'],
      correctAnswer: 'True',
      studentAnswer: 'True',
      marksAwarded: 2,
    },
    {
      questionId: 3,
      questionBody: 'Explain why accuracy is a poor metric for imbalanced classes.',
      questionType: 'TEXT',
      marks: 4,
      options: null,
      correctAnswer: null,
      studentAnswer: 'Because predicting the majority class already scores high.',
      marksAwarded: 2,
    },
  ],
};

const DISCUSSION_POSTS = [
  {
    id: 1,
    courseId: 1,
    authorId: 1,
    authorName: 'Aarav Sharma',
    body: 'Is the validation split stratified in the notebook?',
    createdAt: '2026-10-22T09:00:00Z',
    flagged: false,
    replies: [
      {
        id: 2,
        courseId: 1,
        authorId: 2,
        authorName: 'Meera Iyer',
        body: 'Yes, and the seed is fixed so the split is reproducible.',
        parentPostId: 1,
        createdAt: '2026-10-22T10:00:00Z',
        flagged: false,
        replies: [],
      },
    ],
  },
];

const LIVE_SESSIONS = [
  {
    id: 1,
    courseId: 1,
    courseTitle: 'Applied Machine Learning',
    title: 'Model evaluation clinic',
    scheduledAt: '2026-10-26T15:00:00Z',
    status: 'SCHEDULED',
    joinUrl: null,
  },
  {
    id: 2,
    courseId: 2,
    courseTitle: 'Design Systems in Practice',
    title: 'Accessibility review',
    scheduledAt: '2026-10-24T15:00:00Z',
    status: 'ENDED',
    joinUrl: null,
  },
];

const COLLEGES = [
  {
    id: 1,
    name: 'IIT Bombay',
    city: 'Mumbai',
    studentCount: 1840,
    teacherCount: 120,
    courseCount: 48,
    verificationStatus: 'VERIFIED',
    appliedAt: '2025-06-01T09:00:00Z',
  },
  {
    id: 2,
    name: 'NIT Trichy',
    city: 'Tiruchirappalli',
    studentCount: 960,
    teacherCount: 74,
    courseCount: 21,
    verificationStatus: 'PENDING',
    appliedAt: '2026-09-15T09:00:00Z',
  },
];

const AFFILIATIONS = [
  {
    id: 1,
    teacherName: 'Ananya Deshpande',
    avatarUrl: '',
    claimedDepartment: 'Electrical Engineering',
    claimedCollegeName: 'NIT Trichy',
    idDocumentUrl: 'https://example.test/id/ananya.pdf',
  },
];

const ADMIN_USERS = [
  {
    id: 1,
    name: 'Aarav Sharma',
    email: 'student@learnsphere.test',
    role: 'STUDENT',
    department: 'Computer Science',
    college: 'IIT Bombay',
    status: 'ACTIVE',
    lastActiveAt: ISO,
    createdAt: '2026-01-15T09:00:00Z',
  },
  {
    id: 2,
    name: 'Meera Iyer',
    email: 'teacher@learnsphere.test',
    role: 'TEACHER',
    department: 'Computer Science',
    college: 'IIT Bombay',
    status: 'ACTIVE',
    lastActiveAt: ISO,
    createdAt: '2025-08-01T09:00:00Z',
  },
  {
    id: 4,
    name: 'Priya Nair',
    email: 'priya@learnsphere.test',
    role: 'STUDENT',
    department: 'Design',
    college: 'NIT Trichy',
    status: 'FLAGGED',
    lastActiveAt: '2026-09-30T09:00:00Z',
    createdAt: '2026-02-11T09:00:00Z',
  },
];

const FLAGGED = [
  {
    id: 1,
    contentType: 'DISCUSSION_POST',
    contentTypeLabel: 'Discussion post',
    contentId: 44,
    reason: 'BULLYING',
    severity: 'high',
    status: 'PENDING',
    authorId: 4,
    authorName: 'Priya Nair',
    authorInitials: 'PN',
    courseTitle: 'Applied Machine Learning',
    snippet: 'This explanation is useless; anyone posting it should be removed.',
    createdAt: '2026-10-24T08:00:00Z',
  },
  {
    id: 2,
    contentType: 'MESSAGE',
    contentTypeLabel: 'Message',
    contentId: 91,
    reason: 'SPAM',
    severity: 'medium',
    status: 'PENDING',
    authorId: 7,
    authorName: 'Kabir Menon',
    authorInitials: 'KM',
    courseTitle: null,
    snippet: 'Earn money fast: visit the link in my profile.',
    createdAt: '2026-10-23T16:00:00Z',
  },
];

const PENDING_PAYOUTS = [
  { teacherId: 2, teacherName: 'Meera Iyer', avatarUrl: null, pendingAmount: 48250, courseCount: 3 },
  { teacherId: 8, teacherName: 'Vikram Sethi', avatarUrl: null, pendingAmount: 12700, courseCount: 1 },
];

const PAYOUTS = [
  {
    id: 1,
    period: '2026-09',
    amount: 38400,
    status: 'PAID',
    transferredAt: '2026-10-01T09:00:00Z',
    createdAt: '2026-09-30T09:00:00Z',
  },
  {
    id: 2,
    period: '2026-10',
    amount: 48250,
    status: 'PENDING',
    transferredAt: null,
    createdAt: '2026-10-24T09:00:00Z',
  },
];

const ROYALTIES = {
  summary: { thisMonthTotal: 48250, totalEarned: 312400, pendingPayout: 48250, externalEnrollments: 37 },
  byCourse: [
    {
      courseId: 1,
      courseTitle: 'Applied Machine Learning',
      enrolledCount: 412,
      externalPaidCount: 26,
      amount: 33800,
      status: 'LIVE',
      isDraft: false,
    },
    {
      courseId: 2,
      courseTitle: 'Design Systems in Practice',
      enrolledCount: 188,
      externalPaidCount: 11,
      amount: 14450,
      status: 'LIVE',
      isDraft: false,
    },
  ],
  sourceBreakdown: [
    { source: 'INTERNAL', label: 'Internal enrolments', amount: 21750, percentage: 45 },
    { source: 'EXTERNAL', label: 'External enrolments', amount: 26500, percentage: 55 },
  ],
};

const REPORTS = {
  totalUsers: 6452,
  usersByRole: { STUDENT: 6120, TEACHER: 308, ADMIN: 24 },
  totalCourses: 214,
  coursesByStatus: { DRAFT: 18, PENDING: 9, LIVE: 176, ARCHIVED: 11 },
  totalRevenue: 4820000,
  totalCertificatesIssued: 1842,
  flaggedContentPending: 2,
};

const REVENUE = {
  totalRevenue: 4820000,
  platformEarnings: 964000,
  teacherRoyalties: 3856000,
  platformCutPercent: 20,
  teacherRoyaltyPercent: 80,
  byCourse: [
    { courseId: 1, courseTitle: 'Applied Machine Learning', teacherName: 'Meera Iyer', amount: 1680000, purchases: 412 },
    { courseId: 2, courseTitle: 'Design Systems in Practice', teacherName: 'Meera Iyer', amount: 774000, purchases: 188 },
  ],
  byMonth: [
    { month: '2026-07', amount: 412000 },
    { month: '2026-08', amount: 468000 },
    { month: '2026-09', amount: 523000 },
    { month: '2026-10', amount: 604000 },
  ],
};

const SENTIMENT = {
  totalAnalyzed: 1284,
  positivePercent: 62,
  neutralPercent: 27,
  negativePercent: 11,
  courseTypeSentiment: [
    { type: 'Computer Science', positive: 68, color: 'green' },
    { type: 'Design', positive: 61, color: 'blue' },
  ],
  negativeKeywords: [
    { word: 'confusing', count: 34, severity: 'medium' },
    { word: 'rushed', count: 21, severity: 'low' },
  ],
  recommendations: [
    {
      title: 'Re-record the evaluation lecture',
      description: 'Negative comments cluster around the evaluation module.',
      action: 'Review module',
      actionClass: 'primary',
    },
  ],
  teacherScores: [
    { initials: 'MI', name: 'Meera Iyer', comments: 184, score: '4.8', status: 'Excellent' },
  ],
};

const STANDINGS = [
  { studentId: 1, name: 'Aarav Sharma', avatarUrl: '', progressPercent: 42, avgScore: 78, isAtRisk: false },
  { studentId: 4, name: 'Priya Nair', avatarUrl: '', progressPercent: 18, avgScore: 46, isAtRisk: true },
  { studentId: 7, name: 'Kabir Menon', avatarUrl: '', progressPercent: 73, avgScore: 88, isAtRisk: false },
];

/** Fixture used when a route is not modelled yet: lists stay lists, objects stay objects. */
function fallback(path: string): unknown {
  const last = path.split('/').pop() ?? '';
  if (/^\d+$/.test(last) || last === 'me' || last === 'summary') return {};
  return [];
}

/**
 * Resolve a request path (without the `/api` prefix) to a fixture.
 * Ordered: exact match, then prefix/pattern. `undefined` means "not modelled".
 */
function resolveFixture(path: string): unknown {
  switch (path) {
    case '/users/me':
      return VISUAL_USERS.student;
    case '/student/courses':
    case '/courses':
    case '/teacher/courses':
    case '/admin/courses':
      return COURSES;
    case '/courses/trending':
      return [
        { id: 1, title: 'Applied Machine Learning', teacherName: 'Meera Iyer', department: 'Computer Science', students: 412, rating: 4.8 },
        { id: 2, title: 'Design Systems in Practice', teacherName: 'Meera Iyer', department: 'Design', students: 188, rating: 4.7 },
      ];
    case '/notes':
      return NOTES;
    case '/bookmarks':
      return BOOKMARKS;
    case '/certificates':
      return CERTIFICATES;
    case '/announcements':
      return ANNOUNCEMENTS;
    case '/notifications':
      return NOTIFICATIONS;
    case '/notifications/unread-count':
      return { count: 2 };
    case '/conversations':
      return CONVERSATIONS;
    case '/leaderboard':
    case '/leaderboard/me':
      return LEADERBOARD;
    case '/assessments':
      return ASSESSMENTS;
    case '/colleges':
    case '/admin/colleges':
      return COLLEGES;
    case '/admin/colleges/affiliations':
      return AFFILIATIONS;
    case '/admin/users':
      return ADMIN_USERS;
    case '/admin/users/activity-summary':
      return { totalUsers: 6452, dailyActive: 812, weeklyActive: 3240, inactive30Days: 418 };
    case '/admin/users/top-teachers':
      return [
        { id: 2, name: 'Meera Iyer', students: 600, earnings: 48250 },
        { id: 8, name: 'Vikram Sethi', students: 210, earnings: 12700 },
      ];
    case '/admin/reports/summary':
      return REPORTS;
    case '/admin/revenue':
      return REVENUE;
    case '/admin/sentiment':
      return SENTIMENT;
    case '/admin/payouts':
      return PENDING_PAYOUTS;
    case '/admin/flagged':
      return FLAGGED;
    case '/teacher/royalties':
      return ROYALTIES;
    case '/teacher/payouts':
      return PAYOUTS;
    case '/teacher/live-sessions':
    case '/student/live-sessions':
      return LIVE_SESSIONS;
    case '/payments/history':
      return [];
    case '/payments/status':
      return { paid: false };
    case '/teacher/courses/all/standings':
      return STANDINGS;
    case '/departments':
      return [
        { departmentId: 1, name: 'Computer Science' },
        { departmentId: 2, name: 'Design' },
        { departmentId: 3, name: 'Electrical Engineering' },
      ];
    case '/submissions/me':
      return [];
    default:
      break;
  }

  if (path === '/student/courses/1' || path === '/courses/1') return COURSES[0];
  if (/^\/student\/courses\/\d+$/.test(path)) return COURSES[0];
  if (/^\/courses\/\d+$/.test(path)) return COURSES[0];
  if (/^\/courses\/\d+\/modules$/.test(path)) return MODULES;
  if (/^\/courses\/\d+\/lectures$/.test(path)) return LECTURES;
  if (/^\/courses\/\d+\/leaderboard$/.test(path)) {
    return LEADERBOARD.map((row) => ({
      leaderboardId: row.rank,
      userId: row.studentId,
      userName: row.name,
      userAvatar: row.avatarUrl,
      courseId: 1,
      avgScore: 80 - row.rank * 4,
      globalRank: row.rank,
      deptRank: row.rank,
      updatedAt: ISO,
    }));
  }
  if (/^\/courses\/\d+\/threads$/.test(path)) {
    return [
      {
        threadId: 1,
        courseId: 1,
        userId: 1,
        authorName: 'Aarav Sharma',
        authorAvatar: '',
        title: 'How do I choose k in k-fold cross-validation?',
        content: 'The lecture used k=5 without explaining the trade-off.',
        createdAt: '2026-10-22T09:00:00Z',
        updatedAt: '2026-10-22T09:00:00Z',
      },
      {
        threadId: 2,
        courseId: 1,
        userId: 2,
        authorName: 'Meera Iyer',
        authorAvatar: '',
        title: 'Notebook for lecture 3 is attached',
        content: 'Use the notebook in module 2 to reproduce the figures.',
        createdAt: '2026-10-23T09:00:00Z',
        updatedAt: '2026-10-23T09:00:00Z',
      },
    ];
  }
  if (/^\/courses\/\d+\/discussion$/.test(path)) {
    return DISCUSSION_POSTS;
  }
  if (/^\/courses\/\d+\/feedback$/.test(path)) {
    return [
      {
        feedbackId: 1,
        userId: 1,
        userName: 'Aarav Sharma',
        courseId: 1,
        rating: 5,
        message: 'The notebooks make the theory concrete.',
        createdAt: '2026-10-18T09:00:00Z',
      },
      {
        feedbackId: 2,
        userId: 4,
        userName: 'Priya Nair',
        courseId: 1,
        rating: 4,
        message: 'Would like more exercises on evaluation.',
        createdAt: '2026-10-19T09:00:00Z',
      },
    ];
  }
  if (/^\/courses\/\d+\/feedback\/avg$/.test(path)) {
    return { courseId: 1, averageRating: 4.6, totalReviews: 128 };
  }
  if (/^\/courses\/\d+\/assignments$/.test(path)) return ASSIGNMENTS;
  if (/^\/modules\/\d+\/assignments$/.test(path)) return ASSIGNMENTS;
  if (/^\/modules\/\d+\/contents$/.test(path)) return CONTENTS;
  if (/^\/contents\/\d+$/.test(path)) return CONTENTS[0];
  if (/^\/assignments\/\d+$/.test(path)) return ASSIGNMENTS[0];
  if (/^\/assignments\/\d+\/submissions$/.test(path)) return [];
  if (/^\/student\/courses\/\d+\/lectures\/\d+$/.test(path)) return LECTURES[0];
  if (/^\/student\/courses\/\d+\/progress$/.test(path)) {
    return [{ id: 1, lectureId: 1, progressPercent: 42, secondsWatched: 318 }];
  }
  if (/^\/student\/lectures\/\d+\/progress$/.test(path)) {
    return { id: 1, lectureId: 1, progressPercent: 42, secondsWatched: 318 };
  }
  if (/^\/bookmarks\/lecture\/\d+$/.test(path)) return BOOKMARKS;
  if (/^\/lectures\/\d+\/discussion$/.test(path)) return DISCUSSION_POSTS;
  if (/^\/payments\/orders\/\d+$/.test(path)) {
    return [
      {
        id: 1,
        razorpayOrderId: 'RZP-ORD-2026-0001',
        userId: 1,
        courseId: 1,
        amount: 129900,
        currency: 'INR',
        status: 'PAID',
        createdAt: '2026-06-01T09:00:00Z',
        updatedAt: '2026-06-01T09:05:00Z',
      },
      {
        id: 2,
        razorpayOrderId: 'RZP-ORD-2026-0002',
        userId: 1,
        courseId: 3,
        amount: 0,
        currency: 'INR',
        status: 'CREATED',
        createdAt: '2026-09-03T09:00:00Z',
        updatedAt: '2026-09-03T09:00:00Z',
      },
    ];
  }
  if (/^\/assessments\/\d+\/start$/.test(path)) {
    return { attemptId: 1, testId: 1, status: 'IN_PROGRESS', score: null };
  }
  if (/^\/assessments\/attempts\/\d+$/.test(path)) return ATTEMPT_REVIEW;
  if (/^\/assessments\/lecture\/\d+$/.test(path)) return ASSESSMENTS;
  if (/^\/assessments\/\d+$/.test(path)) return ASSESSMENT_DETAIL;
  if (/^\/teacher\/tests\/course\/\d+$/.test(path)) return [];
  if (/^\/teacher\/tests\/lecture\/\d+$/.test(path)) return [];
  if (/^\/teacher\/tests\/\d+$/.test(path)) return { id: 1, title: 'Supervised learning check', courseId: 1, questions: [] };
  if (/^\/teacher\/courses\/\d+\/standings$/.test(path)) return STANDINGS;
  if (/^\/conversations\/\d+\/messages$/.test(path)) return MESSAGES;
  if (/^\/certificates\/verify\//.test(path)) {
    return {
      valid: true,
      verificationCode: 'LS-2026-0001',
      title: 'Certificate of Completion',
      courseTitle: 'Design Systems in Practice',
      studentName: 'Aarav Sharma',
      instructorName: 'Meera Iyer',
      type: 'STANDARD',
      grade: 'A',
      score: 92,
      issuedAt: '2026-10-01T09:00:00Z',
    };
  }
  if (/^\/threads\/\d+$/.test(path)) {
    return {
      threadId: 1,
      courseId: 1,
      userId: 1,
      authorName: 'Aarav Sharma',
      title: 'How do I choose k in k-fold cross-validation?',
      content: 'The lecture used k=5 without explaining the trade-off.',
      createdAt: '2026-10-22T09:00:00Z',
      posts: [],
    };
  }
  if (/^\/threads\/\d+\/posts$/.test(path)) return [];
  if (/^\/student\/live-sessions\/\d+$/.test(path)) return LIVE_SESSIONS[0];

  return undefined;
}

/**
 * Install the fixture router on a page. Call before `page.goto`.
 * Returns the set of unmatched paths, so a test can assert the harness is not
 * silently drifting away from the app.
 */
export async function installApiStubs(page: Page, role: keyof typeof VISUAL_USERS): Promise<Set<string>> {
  const misses = new Set<string>();

  await page.route('**/api/**', async (route) => {
    const request = route.request();
    const method = request.method();
    const url = new URL(request.url());

    if (method === 'OPTIONS') {
      await route.fulfill({ status: 204, headers: CORS_HEADERS });
      return;
    }

    const path = url.pathname.replace(/^\/api/, '');
    let body: unknown;

    if (path === '/users/me') {
      body = VISUAL_USERS[role] ?? VISUAL_USERS.student;
    } else {
      body = resolveFixture(path);
      if (body === undefined) {
        body = fallback(path);
        if (method === 'GET') misses.add(`${method} ${path}`);
      }
    }

    // A POST/PUT/PATCH against a modelled collection echoes a plausible body;
    // the visual baseline never submits a form, but pages occasionally prefetch.
    if (method !== 'GET' && Array.isArray(body)) {
      body = { success: true };
    }

    await route.fulfill({
      status: 200,
      headers: { ...CORS_HEADERS, 'content-type': 'application/json' },
      body: JSON.stringify(body),
    });
  });

  return misses;
}

/** Clear the service-worker-free storage between tests. */
export async function seedSession(
  page: Page,
  role: keyof typeof VISUAL_USERS,
  theme: 'light' | 'dark',
): Promise<void> {
  const user = VISUAL_USERS[role];
  await page.addInitScript(
    ({ snapshot, mode }) => {
      try {
        localStorage.clear();
        localStorage.setItem('ls_theme_mode', mode);
        if (snapshot) {
          localStorage.setItem('ls_token', 'visual-baseline-token');
          localStorage.setItem('ls_user', JSON.stringify(snapshot));
        }
      } catch {
        // about:blank has no storage origin; the real document gets its own run.
      }
    },
    { snapshot: user, mode: theme },
  );
}
