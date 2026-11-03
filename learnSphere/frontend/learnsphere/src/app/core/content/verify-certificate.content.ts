/**
 * Copy for public certificate verification (SCR-06, W7-07). Moved verbatim from the template.
 * Open for the copy pass (W7-15 / DS-13): "cryptographic registry", "national verification ledger"
 * and the fallback grade/instructor are unverifiable claims, not facts the API returns.
 */
export const VERIFY_CERTIFICATE_COPY = {
  brandMark: 'LS',
  brandName: 'LearnSphere',
  brandSuffix: '.AI',
  registryTag: 'Official Verification Registry',
  heroBadge: 'Credential Verification',
  heroTitle: 'Verify Official Academic Certificate',
  heroSub: 'Validate the authenticity and accreditation of credentials issued by LearnSphere AI learning programs.',
  searchPlaceholder: 'Enter Certificate ID (e.g. LS-2026-A1B2-C3D4)',
  verify: 'Verify Credential',
  verifying: 'Verifying...',
  loading: 'Validating cryptographic registry records...',
  validTitle: 'Authentic Certificate Verified',
  officialRecord: 'OFFICIAL RECORD',
  courseLabel: 'Course Program',
  studentLabel: 'Recipient Student',
  instructorLabel: 'Course Instructor',
  instructorFallback: 'LearnSphere Faculty Board',
  issuedLabel: 'Issue Date',
  gradeLabel: 'Distinction / Grade',
  gradeFallback: 'Pass with Distinction',
  idLabel: 'Certificate ID:',
  statusLabel: 'Status:',
  statusActive: 'Active & Recognized',
  qrAlt: 'Official QR Code',
  qrCaption: 'Scan to re-verify',
  print: 'Download / Print Verification',
  invalidTitle: 'Certificate Not Recognized',
  invalidPrefix: 'No valid credential record was found matching',
  reasonsTitle: 'Possible Reasons:',
  reasons: [
    'The certificate ID was mistyped or contains extra spaces.',
    'The certificate was issued under a different format or academic semester.',
    'The record has not yet been synced with the national verification ledger.',
  ],
  supportPrefix: 'If you believe this is an error, please contact the institution at',
  supportEmail: 'support@learnsphere.ai',
  lookupFailed: 'Unable to verify credential. Please ensure the code is correct.',
} as const;
