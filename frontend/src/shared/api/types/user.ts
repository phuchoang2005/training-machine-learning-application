export type UserRole = "USER" | "ADMIN";
export type UserStatus = "ACTIVE" | "DISABLED";

/**
 * Authenticated session user.
 * In dev-auth mode the bearer token equals the user's email;
 * the backend resolves the token back to this record via its seeded accounts.
 */
export type CurrentUser = {
  userId: string;
  email: string;
  fullName: string;
  role: UserRole;
  status: UserStatus;
  lastLoginAt: string;
};

/** Minimal identity embedded inside job and audit records. */
export type UserSummary = Pick<CurrentUser, "userId" | "email" | "fullName">;
