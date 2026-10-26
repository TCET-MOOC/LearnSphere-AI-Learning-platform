import { defineConfig, devices } from '@playwright/test';

const baseURL = 'http://127.0.0.1:4200';

export default defineConfig({
  testDir: './e2e',
  outputDir: 'test-results',
  fullyParallel: true,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 2 : 0,
  workers: process.env['CI'] ? 1 : undefined,
  reporter: [['list'], ['html', { open: 'never' }]],
  timeout: 30_000,
  expect: {
    timeout: 5_000,
  },
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      testIgnore: /visual[\\/]/,
      use: { ...devices['Desktop Chrome'] },
    },
    {
      // DS-01 visual baseline: 46 contracts × 3 viewports × 2 themes.
      // Baselines live in e2e/visual/__screenshots__ and are platform-specific
      // by construction; CI runs this project on the same OS the baselines were
      // generated on (windows-latest).
      name: 'visual',
      testMatch: /visual[\\/].*\.spec\.ts/,
      use: { ...devices['Desktop Chrome'], deviceScaleFactor: 1 },
    },
  ],
  snapshotPathTemplate: '{testDir}/visual/__screenshots__/{arg}{ext}',
  webServer: {
    command: `npm start -- --host 127.0.0.1 --port 4200`,
    url: baseURL,
    reuseExistingServer: !process.env['CI'],
    timeout: 180_000,
    stdout: 'pipe',
    stderr: 'pipe',
  },
});
