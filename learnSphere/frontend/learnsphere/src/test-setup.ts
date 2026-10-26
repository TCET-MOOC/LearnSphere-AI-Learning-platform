import { vi } from 'vitest';

/**
 * Global browser shims for the jsdom test environment.
 *
 * Angular's unit-test builder initialises the polyfills and TestBed before this
 * file, so this is the right place for APIs jsdom does not implement.
 */

if (!window.matchMedia) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  });
}

if (!('ResizeObserver' in window)) {
  class ResizeObserverStub {
    observe(): void {}
    unobserve(): void {}
    disconnect(): void {}
  }

  Object.defineProperty(window, 'ResizeObserver', {
    writable: true,
    value: ResizeObserverStub,
  });
}

if (!('IntersectionObserver' in window)) {
  class IntersectionObserverStub {
    readonly root = null;
    readonly rootMargin = '';
    readonly thresholds = [];

    observe(): void {}
    unobserve(): void {}
    disconnect(): void {}
    takeRecords(): IntersectionObserverEntry[] {
      return [];
    }
  }

  Object.defineProperty(window, 'IntersectionObserver', {
    writable: true,
    value: IntersectionObserverStub,
  });
}

if (!window.scrollTo) {
  Object.defineProperty(window, 'scrollTo', {
    writable: true,
    value: vi.fn(),
  });
}

if (!Element.prototype.scrollIntoView) {
  Object.defineProperty(Element.prototype, 'scrollIntoView', {
    writable: true,
    value: vi.fn(),
  });
}
