export enum UserRole {
  STUDENT = 'STUDENT',
  TEACHER = 'TEACHER',
  ADMIN = 'ADMIN'
}

/**
 * Base User interface containing shared properties.
 */
export interface User {
  id: string;
  email: string;
  role: UserRole;
  collegeId: string;
  name: string;
  avatarUrl?: string;
  createdAt: string;
}

/**
 * Interface representing Student profile details.
 */
export interface Student extends User {
  enrolledCourses: string[];
  attendanceScore: number;
  leaderboardPoints: number;
}

/**
 * Interface representing Teacher profile details.
 */
export interface Teacher extends User {
  department: string;
  bio: string;
  bankAccountLinked: boolean;
  royaltyBalance: number;
}

/**
 * Interface representing Admin profile details.
 */
export interface Admin extends User {
  twoFactorEnabled: boolean;
  lastAuditAction: string;
}

/**
 * Interface representing Student Standing for standings dashboard.
 * Used by teachers to see progress and identify at-risk students.
 */
export interface StudentStanding {
  studentId: string;
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
  id: string;
  name: string;
  city: string;
  studentCount: number;
  teacherCount: number;
  courseCount: number;
  verificationStatus: 'VERIFIED' | 'PENDING';
  appliedAt?: string; // e.g. "Applied 19 Jun"
}

/**
 * Interface representing a Teacher's request to affiliate with a college.
 * Requires admin review and ID document check.
 */
export interface AffiliationRequest {
  id: string;
  teacherName: string;
  avatarUrl?: string;
  claimedDepartment: string;
  claimedCollegeName: string;
  idDocumentUrl: string; // URL to the uploaded ID document image or PDF
}
