/**
 * Deterministic initials/background/color derivation for a display name.
 * Mirrors the algorithm in shared/components/avatar/avatar.component.ts so that
 * plain-object UI cards (e.g. conversation list items) that don't render
 * <app-avatar> directly still get consistent, stable colors per person.
 */

const BG_COLORS = ['#EEEDFE', '#EAF3DE', '#FAEEDA', '#FCEBEB', '#E1F5EE', '#E6F1FB'];
const TEXT_COLORS = ['#3C3489', '#27500A', '#633806', '#791F1F', '#085041', '#0C447C'];

export function getInitials(name: string | null | undefined): string {
  if (!name) return '??';
  const words = name.trim().split(/\s+/);
  if (words.length > 1) {
    return (words[0][0] + words[1][0]).toUpperCase();
  }
  return name.substring(0, 2).toUpperCase();
}

function charCodeSum(initials: string): number {
  return initials.charCodeAt(0) + (initials.charCodeAt(1) || 0);
}

export function getAvatarBg(name: string | null | undefined): string {
  const initials = getInitials(name);
  return BG_COLORS[charCodeSum(initials) % BG_COLORS.length];
}

export function getAvatarColor(name: string | null | undefined): string {
  const initials = getInitials(name);
  return TEXT_COLORS[charCodeSum(initials) % TEXT_COLORS.length];
}
