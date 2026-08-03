export enum UserRole {
  STUDENT = 'STUDENT',
  TEACHER = 'TEACHER',
  ADMIN = 'ADMIN'
}

/**
 * Base User interface containing shared properties.
 * Matches UserResponseDto returned by the backend for every authenticated user,
 * regardless of role — role-specific fields are optional and populated as relevant.
 */
export interface User {
  id: number;
  fullName: string;
  email: string;
  role: UserRole;
  collegeId?: number;
  collegeName?: string;
  collegeVerificationStatus?: 'VERIFIED' | 'PENDING';
  departmentId?: number;
  departmentName?: string;
  avatarUrl?: string;
  bio?: string;
  status?: 'ACTIVE' | 'FLAGGED' | 'BLACKLISTED';
  twoFactorEnabled?: boolean;
  bankAccountLinked?: boolean;
  royaltyBalance?: number;
  attendanceScore?: number;
  leaderboardPoints?: number;
  lastActiveAt?: string;
  createdAt: string;
}

/** Convenience alias: student-facing view of User. */
export type Student = User;

/** Convenience alias: teacher-facing view of User. */
export type Teacher = User;

/** Convenience alias: admin-facing view of User. */
export type Admin = User;

/**
 * Interface representing Student Standing for standings dashboard.
 * Used by teachers to see progress and identify at-risk students.
 */
export interface StudentStanding {
  studentId: number;
  name: string;
  avatarUrl?: string;
  rank: number;
  scorePercent: number;
  lecturesWatched: number;
  totalLectures: number;
  lastActiveAt: string;
  isRemedial: boolean;
  isAtRisk: boolean; // below 40% progress OR inactive 14+ days
}

/**
 * Interface representing a College/Institution.
 * Managed by admin in the college-management dashboard.
 */
export interface College {
  id: number;
  name: string;
  city: string;
  studentCount: number;
  teacherCount: number;
  courseCount: number;
  verificationStatus: 'VERIFIED' | 'PENDING';
  appliedAt?: string;
}

/**
 * Interface representing a Teacher's request to affiliate with a college.
 * Requires admin review and ID document check.
 */
export interface AffiliationRequest {
  id: number;
  teacherName: string;
  avatarUrl?: string;
  claimedDepartment: string;
  claimedCollegeName: string;
  idDocumentUrl: string;
}
