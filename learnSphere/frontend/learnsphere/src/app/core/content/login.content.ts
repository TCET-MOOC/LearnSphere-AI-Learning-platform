/**
 * Login copy (SCR-02, W7-07), moved verbatim from the template; wording changes belong to W7-15.
 * One content file per screen: a content module shared by several lazy screens resolved to
 * `undefined` in the unit-test bundle (see `extracted-copy.spec.ts`), so the few strings the auth
 * screens share are repeated in each screen's file.
 */
export const LOGIN_COPY = {
  homeLinkTitle: 'LearnSphere Home',
  logoAlt: 'LearnSphere AI',
  title: 'Welcome back',
  subtitle: 'Please enter your details to sign in.',
  emailLabel: 'College Email',
  emailPlaceholder: 'Enter your email',
  passwordLabel: 'Password',
  passwordPlaceholder: '8–16 chars (e.g. Pass@123)',
  passwordHint: '8–16 characters, min. 1 uppercase letter, 1 number, and 1 special character.',
  passwordRequired: 'Password is required.',
  passwordInvalid: 'Must be 8–16 characters with 1 uppercase, 1 number, and 1 special character.',
  forgotPassword: 'Forgot password?',
  submit: 'Sign In',
  submitting: 'Signing in…',
  noAccount: "Don't have an account?",
  signUp: 'Sign up',
  failed: 'Invalid email or password.',
} as const;
