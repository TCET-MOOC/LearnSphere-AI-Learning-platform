/**
 * Accessible names for shared controls (W7-08; WCAG 2.5.3 "label in name", 4.1.2 "name, role, value").
 * Names of controls with visible text start with that text so speech-input users can say what they see.
 *
 * Unlike the per-screen content files, this copy is used by many lazy chunks (shell, dialogs). In the
 * unit-test bundle a bare `export const` imported by several lazy chunks resolved to `undefined`
 * inside the components (see `extracted-copy.spec.ts`); reading it through a class static does not,
 * so consumers use `A11yCopy.text`.
 */
const COPY = {
  sidebarCollapse: 'LearnSphere AI: collapse sidebar',
  sidebarExpand: 'LearnSphere AI: expand sidebar',
  chatbotOpen: 'Ask AI assistant',
  chatbotClose: 'Close AI assistant',
  closeDialog: 'Close dialog',
} as const;

export class A11yCopy {
  static readonly text = COPY;
}
