import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { DialogDirective } from './dialog.directive';

@Component({
  standalone: true,
  imports: [DialogDirective],
  template: `
    <button id="opener" (click)="open = true">Open</button>
    @if (open) {
      <div id="dlg" appDialog aria-labelledby="dlg-title" (dialogClose)="open = false">
        <h2 id="dlg-title">Title</h2>
        <button id="first">First</button>
        <button id="disabled" disabled>Disabled</button>
        <a id="last" href="/x">Last</a>
      </div>
    }
  `,
})
class HostComponent {
  open = false;
}

describe('DialogDirective (W7-08: modal focus trap)', () => {
  function setup() {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.autoDetectChanges();
    document.body.appendChild(fixture.nativeElement);
    const $ = (id: string) => fixture.nativeElement.querySelector(`#${id}`) as HTMLElement | null;
    const opener = $('opener')!;
    opener.focus();
    opener.click();
    fixture.detectChanges();
    return { fixture, $, opener };
  }
  const tab = (target: HTMLElement, shift = false) => {
    const e = new KeyboardEvent('keydown', { key: 'Tab', shiftKey: shift, bubbles: true, cancelable: true });
    target.dispatchEvent(e);
    return e;
  };

  it('is announced as a modal dialog', () => {
    const { $ } = setup();
    expect($('dlg')!.getAttribute('role')).toBe('dialog');
    expect($('dlg')!.getAttribute('aria-modal')).toBe('true');
  });

  it('moves focus to the first focusable control when it opens', () => {
    const { $ } = setup();
    expect(document.activeElement).toBe($('first'));
  });

  it('wraps Tab from the last control to the first, and Shift+Tab from the first to the last', () => {
    const { $ } = setup();
    $('last')!.focus();
    const forward = tab($('last')!);
    expect(forward.defaultPrevented).toBe(true);
    expect(document.activeElement).toBe($('first'));

    const back = tab($('first')!, true);
    expect(back.defaultPrevented).toBe(true);
    expect(document.activeElement).toBe($('last'));
  });

  it('leaves Tab alone between inner controls', () => {
    const { $ } = setup();
    expect(tab($('first')!).defaultPrevented).toBe(false);
  });

  it('closes on Escape and returns focus to the control that opened it', () => {
    const { fixture, $, opener } = setup();
    $('first')!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();
    expect($('dlg')).toBeNull();
    expect(document.activeElement).toBe(opener);
  });
});
