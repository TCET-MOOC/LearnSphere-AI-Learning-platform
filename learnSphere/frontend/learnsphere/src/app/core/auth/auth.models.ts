import { User, UserRole } from '@core/models/user.model';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  role: UserRole | string;
  collegeId?: number;
  departmentId?: number;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface VerifyCollegeRequest {
  collegeId: number;
  claimedDepartment?: string;
  idDocumentUrl?: string;
}
