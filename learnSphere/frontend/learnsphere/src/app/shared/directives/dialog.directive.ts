import { AfterViewInit, Directive, ElementRef, EventEmitter, HostListener, OnDestroy, OnInit, Output, inject } from '@angular/core';

const FOCUSABLE = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

/**
 * Modal behaviour for a hand-built dialog panel (W7-08; `PLAN §7` "modals trap focus").
 *
 * - `role="dialog"` and `aria-modal="true"` (name it with `aria-labelledby` in the template);
 * - focus moves into the panel when it opens, and Tab / Shift+Tab wrap inside it;
 * - Escape emits `dialogClose` (the owner hides the panel, as its backdrop click already does);
 * - focus returns to the control that opened it when the panel is destroyed.
 */
@Directive({ selector: '[appDialog]', standalone: true })
export class DialogDirective implements OnInit, AfterViewInit, OnDestroy {
  @Output() readonly dialogClose = new EventEmitter<void>();

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly opener = (typeof document !== 'undefined' ? document.activeElement : null) as HTMLElement | null;

  ngOnInit(): void {
    if (!this.host.hasAttribute('role')) this.host.setAttribute('role', 'dialog');
    this.host.setAttribute('aria-modal', 'true');
    if (!this.host.hasAttribute('tabindex')) this.host.setAttribute('tabindex', '-1');
  }

  ngAfterViewInit(): void {
    (this.focusable()[0] ?? this.host).focus();
  }

  ngOnDestroy(): void {
    if (this.opener?.isConnected) this.opener.focus();
  }

  @HostListener('keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.stopPropagation();
      this.dialogClose.emit();
      return;
    }
    if (event.key !== 'Tab') return;
    const items = this.focusable();
    if (!items.length) {
      event.preventDefault();
      return;
    }
    const first = items[0];
    const last = items[items.length - 1];
    const active = document.activeElement;
    if (event.shiftKey && (active === first || active === this.host)) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && active === last) {
      event.preventDefault();
      first.focus();
    }
  }

  private focusable(): HTMLElement[] {
    return Array.from(this.host.querySelectorAll<HTMLElement>(FOCUSABLE)).filter((el) => !el.closest('[hidden]'));
  }
}
