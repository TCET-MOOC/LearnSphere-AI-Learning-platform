/**
 * Copy for public certificate verification (SCR-06, W7-07). Moved verbatim from the template.
 * W7-15 / DS-13: the unverifiable "registry"/"ledger" claims are gone, and a missing instructor
 * or grade shows a dash instead of an invented value the API never returned.
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
  loading: 'Checking the certificate record…',
  validTitle: 'Authentic Certificate Verified',
  officialRecord: 'OFFICIAL RECORD',
  courseLabel: 'Course Program',
  studentLabel: 'Recipient Student',
  instructorLabel: 'Course Instructor',
  instructorFallback: '—',
  issuedLabel: 'Issue Date',
  gradeLabel: 'Distinction / Grade',
  gradeFallback: '—',
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
    'The certificate was revoked or has not been issued yet.',
  ],
  supportPrefix: 'If you believe this is an error, please contact the institution at',
  supportEmail: 'support@learnsphere.ai',
  lookupFailed: 'Unable to verify credential. Please ensure the code is correct.',
} as const;
