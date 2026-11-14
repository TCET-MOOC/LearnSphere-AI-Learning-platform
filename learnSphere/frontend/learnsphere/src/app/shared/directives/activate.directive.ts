import { Directive, ElementRef, HostListener, OnInit, inject } from '@angular/core';

/**
 * Keyboard path for an element that only had a `(click)` handler (W7-08; `spec §7`, `PLAN §7`).
 *
 * Adds `tabindex="0"` and `role="button"` unless the template sets its own, and turns Enter and
 * Space on the element itself into a click. Keys typed into a nested control are left alone.
 * Use it where swapping the element for a `<button>` would change the layout (NFR-01).
 */
@Directive({ selector: '[appActivate]', standalone: true })
export class ActivateDirective implements OnInit {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

  ngOnInit(): void {
    if (!this.host.hasAttribute('tabindex')) this.host.setAttribute('tabindex', '0');
    if (!this.host.hasAttribute('role')) this.host.setAttribute('role', 'button');
  }

  @HostListener('keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    if (event.target !== this.host) return;
    if (event.key !== 'Enter' && event.key !== ' ') return;
    event.preventDefault();
    this.host.click();
  }
}
