/**
 * Layout defects a user can see: controls or text drawn over each other, content cut off by
 * its container, text clipped without an ellipsis, and anything pushing the page sideways.
 * Runs in the page; floating layers (fixed position: the AI button, toasts, open dropdowns)
 * are ignored because they overlap content by design.
 */
import { Page } from '@playwright/test';

export interface LayoutIssue {
  kind: 'overlap' | 'clipped' | 'text-cut' | 'page-overflow';
  a: string;
  b?: string;
  rect: { x: number; y: number; w: number; h: number };
}

export async function auditLayout(page: Page): Promise<LayoutIssue[]> {
  return page.evaluate(() => {
    const issues: LayoutIssue[] = [];
    const SELECT = 'button, a, input, select, textarea, label, h1, h2, h3, h4, h5, h6, p, li, td, th, img, [role="button"], [role="tab"]';

    const describe = (el: Element) => {
      const cls = (el.getAttribute('class') || '').split(/\s+/).filter((c) => c && !c.startsWith('ng-')).slice(0, 2).join('.');
      const text = (el.textContent || '').replace(/\s+/g, ' ').trim().slice(0, 40);
      return `${el.tagName.toLowerCase()}${cls ? '.' + cls : ''}${text ? ` "${text}"` : ''}`;
    };

    const inFloatingLayer = (el: Element) => {
      for (let n: Element | null = el; n && n !== document.body; n = n.parentElement) {
        const cs = getComputedStyle(n);
        if (cs.position === 'fixed' || cs.position === 'sticky') return true;
        if (n.matches('app-ai-chatbot, .cdk-overlay-container, [role="dialog"], [role="menu"], [role="tooltip"]')) return true;
      }
      return false;
    };

    const visible = (el: Element) => {
      const r = el.getBoundingClientRect();
      if (r.width < 2 || r.height < 2) return false;
      const cs = getComputedStyle(el);
      return cs.visibility !== 'hidden' && cs.display !== 'none' && Number(cs.opacity) > 0.05;
    };

    // Elements that own visible content of their own (not just wrappers around other candidates).
    const all = Array.from(document.querySelectorAll(SELECT)).filter((el) => visible(el) && !inFloatingLayer(el));
    const candidates = all.filter((el) => !all.some((o) => o !== el && el.contains(o) && o.matches('button, a, input, select, textarea')) || el.matches('button, a, input, select, textarea'));

    const rects = new Map(candidates.map((el) => [el, el.getBoundingClientRect()]));
    const box = (r: DOMRect) => ({ x: Math.round(r.x), y: Math.round(r.y), w: Math.round(r.width), h: Math.round(r.height) });

    // 1. Overlap between unrelated candidates.
    for (let i = 0; i < candidates.length; i++) {
      for (let j = i + 1; j < candidates.length; j++) {
        const a = candidates[i], b = candidates[j];
        if (a.contains(b) || b.contains(a)) continue;
        const ra = rects.get(a)!, rb = rects.get(b)!;
        const w = Math.min(ra.right, rb.right) - Math.max(ra.left, rb.left);
        const h = Math.min(ra.bottom, rb.bottom) - Math.max(ra.top, rb.top);
        if (w <= 3 || h <= 3) continue;
        const smaller = Math.min(ra.width * ra.height, rb.width * rb.height);
        if (w * h < 0.15 * smaller) continue;
        issues.push({ kind: 'overlap', a: describe(a), b: describe(b), rect: { x: Math.round(Math.max(ra.left, rb.left)), y: Math.round(Math.max(ra.top, rb.top)), w: Math.round(w), h: Math.round(h) } });
      }
    }

    // 2. Cut off by an ancestor that hides overflow (not a scroller).
    for (const el of candidates) {
      const r = rects.get(el)!;
      for (let n = el.parentElement; n && n !== document.body; n = n.parentElement) {
        const cs = getComputedStyle(n);
        const hidesX = cs.overflowX === 'hidden' || cs.overflowX === 'clip';
        const hidesY = cs.overflowY === 'hidden' || cs.overflowY === 'clip';
        const scrolls = /auto|scroll/.test(cs.overflowX + cs.overflowY);
        if (scrolls) break;
        if (!hidesX && !hidesY) continue;
        const pr = n.getBoundingClientRect();
        const outX = hidesX && (r.left < pr.left - 2 || r.right > pr.right + 2);
        const outY = hidesY && (r.top < pr.top - 2 || r.bottom > pr.bottom + 2);
        if (outX || outY) {
          issues.push({ kind: 'clipped', a: describe(el), b: describe(n), rect: box(r) });
          break;
        }
      }
    }

    // 3. Text clipped inside its own box without an ellipsis.
    for (const el of candidates) {
      const cs = getComputedStyle(el);
      if (!(cs.overflowX === 'hidden' || cs.overflowX === 'clip')) continue;
      if (cs.textOverflow === 'ellipsis') continue;
      const he = el as HTMLElement;
      if (he.scrollWidth > he.clientWidth + 2) issues.push({ kind: 'text-cut', a: describe(el), rect: box(rects.get(el)!) });
    }

    // 4. Anything pushing the page sideways.
    const extra = document.documentElement.scrollWidth - window.innerWidth;
    if (extra > 0) {
      const culprit = candidates.find((el) => rects.get(el)!.right > window.innerWidth + 1);
      issues.push({ kind: 'page-overflow', a: culprit ? describe(culprit) : `page wider by ${extra}px`, rect: { x: window.innerWidth, y: 0, w: extra, h: 1 } });
    }
    return issues;
  });
}

/** Outlines each issue on the page so a screenshot shows where it is. */
export async function markIssues(page: Page, issues: LayoutIssue[]): Promise<void> {
  await page.evaluate((list) => {
    for (const i of list) {
      const d = document.createElement('div');
      d.style.cssText = `position:fixed;left:${i.rect.x}px;top:${i.rect.y}px;width:${Math.max(i.rect.w, 4)}px;height:${Math.max(i.rect.h, 4)}px;outline:2px solid ${i.kind === 'overlap' ? 'red' : i.kind === 'clipped' ? 'orange' : 'magenta'};z-index:2147483647;pointer-events:none`;
      document.body.appendChild(d);
    }
  }, issues);
}
