/**
 * Stylelint configuration — design-system guard rails (DS-01 / W1-12).
 *
 * The rules are deliberately small: they encode the NFR-04(a)/(c)/(e) bans
 * that can be checked mechanically today. Each one starts from the measured
 * baseline in `stylelint.baseline.json` and may only go down (per-file ratchet,
 * enforced by `scripts/stylelint-ratchet.mjs`).
 *
 * Token files are exempt: `_tokens.scss` is the single place allowed to hold
 * raw colour values, and `_variables.scss` only aliases CSS custom properties.
 *
 * Run: `npm run lint:styles` (gate) / `npm run lint:styles:update` (re-baseline).
 */
export default {
  customSyntax: 'postcss-scss',
  ignoreFiles: [
    'dist/**',
    'node_modules/**',
    '.angular/**',
    'playwright-report/**',
    'test-results/**',
    // Token layer: the only sanctioned home for raw values.
    'src/styles/_tokens.scss',
    'src/styles/_variables.scss',
  ],
  rules: {
    // NFR-04(a): no hex colour literal outside the token layer.
    'color-no-hex': true,

    // NFR-04(c)/(e): one radius scale, and `transition: all` is banned.
    'declaration-property-value-disallowed-list': {
      '/^border(-(top|bottom|left|right))?(-(left|right|start|end))?-radius$/': [
        '/\\b(?!0(?:px|rem|em|%))\\d*\\.?\\d+(?:px|rem|em|%)\\b/',
      ],
      '/^transition(-property)?$/': ['/^all\\b/', '/,\\s*all\\b/'],
    },
  },
};
