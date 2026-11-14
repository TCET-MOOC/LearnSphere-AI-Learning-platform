import { Page } from '@playwright/test';
import { ScreenContract } from '../visual/screen-contracts';
import { installApiStubs, seedSession } from '../visual/api-stubs';

export interface OpenScreenOptions {
  /** Open this route instead of the contract's own. */
  route?: string;
  /** Extra `page.route` stubs; registered after the defaults, so they win. */
  stubs?: (page: Page) => Promise<void>;
}

/** Opens a screen contract exactly as the visual baseline does: stubbed API, seeded session, fixed clock. */
export async function openScreen(
  page: Page,
  contract: ScreenContract,
  theme: 'light' | 'dark',
  options: OpenScreenOptions = {},
): Promise<void> {
  await installApiStubs(page, contract.role);
  if (options.stubs) await options.stubs(page);
  await seedSession(page, contract.role, theme);
  await page.clock.setFixedTime(new Date('2026-10-25T09:00:00Z'));
  await page.emulateMedia({ colorScheme: theme, reducedMotion: 'reduce' });
  await page.goto(options.route ?? contract.route!, { waitUntil: 'domcontentloaded' });
  await page.locator('app-root > :not(router-outlet)').first().waitFor({ state: 'attached', timeout: 15_000 });
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(400);
}
