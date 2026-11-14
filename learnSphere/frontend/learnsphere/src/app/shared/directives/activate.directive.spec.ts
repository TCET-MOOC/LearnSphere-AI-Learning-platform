import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivateDirective } from './activate.directive';

@Component({
  standalone: true,
  imports: [ActivateDirective],
  template: `
    <div id="plain" appActivate (click)="clicks = clicks + 1"><input id="inner" /></div>
    <div id="tab" appActivate role="tab" tabindex="-1" (click)="clicks = clicks + 1">Tab</div>
  `,
})
class HostComponent {
  clicks = 0;
}

describe('ActivateDirective (W7-08: keyboard path for click-only elements)', () => {
  function setup() {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    const el = (id: string) => fixture.nativeElement.querySelector(`#${id}`) as HTMLElement;
    return { host: fixture.componentInstance, el };
  }
  const key = (target: HTMLElement, k: string) => {
    const e = new KeyboardEvent('keydown', { key: k, bubbles: true, cancelable: true });
    target.dispatchEvent(e);
    return e;
  };

  it('makes the element focusable and announces it as a button', () => {
    const { el } = setup();
    expect(el('plain').getAttribute('tabindex')).toBe('0');
    expect(el('plain').getAttribute('role')).toBe('button');
  });

  it('keeps an explicit role and tabindex', () => {
    const { el } = setup();
    expect(el('tab').getAttribute('role')).toBe('tab');
    expect(el('tab').getAttribute('tabindex')).toBe('-1');
  });

  it('activates on Enter and on Space, and stops Space from scrolling the page', () => {
    const { host, el } = setup();
    key(el('plain'), 'Enter');
    const space = key(el('plain'), ' ');
    expect(host.clicks).toBe(2);
    expect(space.defaultPrevented).toBe(true);
  });

  it('ignores keys typed into a nested control', () => {
    const { host, el } = setup();
    key(el('inner'), 'Enter');
    key(el('inner'), ' ');
    expect(host.clicks).toBe(0);
  });
});
