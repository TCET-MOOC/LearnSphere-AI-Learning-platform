/**
 * W6-13 / AUD-01 (audit.md A5): the receipt modal prints as the receipt alone,
 * black on white, and the amount never wraps. Runs in a real browser because
 * print rules and layout are invisible to the jsdom unit tests.
 */
import { test, expect, Page } from '@playwright/test';

const student = {
  id: 1, fullName: 'Aarav Sharma', email: 'student@learnsphere.test', role: 'STUDENT', status: 'ACTIVE'
};

const history = [
  {
    id: 42, courseId: 1,
    courseTitle: 'Advanced Topics in Large-Scale Distributed Systems: Consensus, Replication and Fault Tolerance',
    department: 'Computer Science', amount: 2500, currency: 'INR', status: 'PAID',
    gatewayOrderId: 'order_P4xK9mQ2ZbT1aV', gatewayPaymentId: 'pay_P4xL0aB7cD8eF9',
    createdAt: '2026-11-01T14:00:00', paidAt: '2026-11-01T14:05:09'
  }
];

/** Answers every API call; credentialed requests need an exact origin, not '*'. */
async function stubApi(page: Page) {
  await page.route(/\/api\//, async (route) => {
    const req = route.request();
    const headers = {
      'access-control-allow-origin': req.headers()['origin'] ?? 'http://127.0.0.1:4200',
      'access-control-allow-credentials': 'true',
      'access-control-allow-methods': 'GET,POST,PUT,PATCH,DELETE,OPTIONS',
      'access-control-allow-headers': 'authorization,content-type,x-request-id'
    };
    if (req.method() === 'OPTIONS') return route.fulfill({ status: 204, headers });
    const path = new URL(req.url()).pathname;
    if (path.endsWith('/auth/login')) return route.fulfill({ json: { token: 'e2e-token', user: student }, headers });
    if (path.endsWith('/users/me')) return route.fulfill({ json: student, headers });
    if (path.endsWith('/payments/history')) return route.fulfill({ json: history, headers });
    return route.fulfill({ json: [], headers });
  });
}

async function openReceipt(page: Page) {
  await stubApi(page);
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto('/login');
  await page.getByPlaceholder('Enter your email').fill(student.email);
  await page.getByPlaceholder(/8.16 chars/).fill('Pass@1234');
  await page.getByRole('button', { name: 'Sign In' }).click();
  await page.waitForURL(/\/student\//);
  await page.locator('a[href="/student/purchases"]').first().click();
  await page.getByRole('button', { name: /Receipt/ }).click();
  await expect(page.locator('#printable-receipt')).toContainText('RCPT-42');
}

/** WCAG contrast ratio between two computed `rgb(...)` colours. */
function contrast(a: string, b: string): number {
  const lum = (c: string) => {
    const [r, g, bl] = (c.match(/\d+(\.\d+)?/g) ?? []).slice(0, 3).map(Number).map((v) => {
      const s = v / 255;
      return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4;
    });
    return 0.2126 * r + 0.7152 * g + 0.0722 * bl;
  };
  const [hi, lo] = [lum(a), lum(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
}

test.describe('Receipt modal (W6-13)', () => {
  test('footer buttons stay readable in the dark theme', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.addInitScript(() => localStorage.setItem('ls_theme_mode', 'dark'));
    await openReceipt(page);
    for (const name of ['Close', 'Print']) {
      const button = page.locator('.receipt-footer').getByRole('button', { name });
      // Effective background: blend each translucent layer over the ones beneath it.
      const { fg, bg } = await button.evaluate((el) => {
        const parse = (c: string) => {
          const n = (c.match(/[\d.]+/g) ?? ['0', '0', '0', '0']).map(Number);
          return { r: n[0], g: n[1], b: n[2], a: n.length > 3 ? n[3] : 1 };
        };
        const layers: Array<{ r: number; g: number; b: number; a: number }> = [];
        for (let node: Element | null = el; node; node = node.parentElement) {
          const layer = parse(getComputedStyle(node).backgroundColor);
          if (layer.a > 0) layers.push(layer);
          if (layer.a >= 1) break;
        }
        let base = { r: 255, g: 255, b: 255 };
        for (const l of layers.reverse()) {
          base = { r: l.r * l.a + base.r * (1 - l.a), g: l.g * l.a + base.g * (1 - l.a), b: l.b * l.a + base.b * (1 - l.a) };
        }
        return { fg: getComputedStyle(el).color, bg: `rgb(${Math.round(base.r)}, ${Math.round(base.g)}, ${Math.round(base.b)})` };
      });
      expect(contrast(fg, bg), `${name}: ${fg} on ${bg}`).toBeGreaterThanOrEqual(4.5);
    }
  });

  test('prints on a white page even in the dark theme', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.addInitScript(() => localStorage.setItem('ls_theme_mode', 'dark'));
    await openReceipt(page);
    await page.emulateMedia({ media: 'print', colorScheme: 'dark' });
    const page_ = await page.evaluate(() => {
      const opaque = (c: string) => (c === 'rgba(0, 0, 0, 0)' || c === 'transparent' ? 'white' : c);
      return {
        theme: document.documentElement.getAttribute('data-theme'),
        html: opaque(getComputedStyle(document.documentElement).backgroundColor),
        body: opaque(getComputedStyle(document.body).backgroundColor)
      };
    });
    expect(page_.theme).toBe('dark');
    expect([page_.html, page_.body]).toEqual([
      expect.stringMatching(/^(white|rgb\(255, 255, 255\))$/),
      expect.stringMatching(/^(white|rgb\(255, 255, 255\))$/)
    ]);
  });

  test('keeps the amount on one line and the print-only title off screen', async ({ page }) => {
    await openReceipt(page);
    await expect(page.locator('#printable-receipt').getByText('PAYMENT RECEIPT', { exact: true })).toBeHidden();
    const amount = page.locator('#printable-receipt .item-row:not(.header-row):not(.total-row) .text-right');
    const lines = await amount.evaluate((el) => {
      const lineHeight = parseFloat(getComputedStyle(el).lineHeight) || 20;
      return Math.round(el.getBoundingClientRect().height / lineHeight);
    });
    expect(lines).toBe(1);
  });

  test('prints only the receipt, black on white, at the top of the page', async ({ page }) => {
    await openReceipt(page);
    await page.emulateMedia({ media: 'print' });

    const receipt = page.locator('#printable-receipt');
    await expect(receipt).toBeVisible();
    expect((await receipt.boundingBox())!.y).toBeLessThan(40);
    // Same title and issuer as the downloaded PDF.
    await expect(receipt.getByText('PAYMENT RECEIPT', { exact: true })).toBeVisible();
    await expect(receipt.getByText('LearnSphere AI Learning Platform', { exact: true })).toBeVisible();

    // Not printed = not rendered at all, or rendered invisible.
    const printed = await page.evaluate(() => {
      const shows = (selector: string) => {
        const el = document.querySelector(selector)!;
        return el.getClientRects().length > 0 && getComputedStyle(el).visibility !== 'hidden';
      };
      return {
        heading: shows('h1'),
        footer: shows('.receipt-footer'),
        modalHeader: shows('.receipt-header'),
        sidebar: shows('a[href="/student/purchases"]')
      };
    });
    expect(printed).toEqual({ heading: false, footer: false, modalHeader: false, sidebar: false });

    const colours = await receipt.evaluate((root) =>
      Array.from(root.querySelectorAll<HTMLElement>('*'))
        .filter((el) => el.textContent?.trim() && el.children.length === 0)
        .map((el) => getComputedStyle(el).color)
    );
    expect(colours.length).toBeGreaterThan(5);
    expect(new Set(colours)).toEqual(new Set(['rgb(0, 0, 0)']));
  });
});
