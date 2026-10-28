import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { importProvidersFrom } from '@angular/core';
import { LucideAngularModule, GraduationCap, Globe, FileText, Monitor, Ruler, Search, User, Play, Settings, Menu, X, ShieldCheck } from 'lucide-angular';
import { LandingComponent } from './landing.component';

describe('LandingComponent Surface & Token Conformance (DS-02 / W1-13)', () => {
  let component: LandingComponent;
  let fixture: ComponentFixture<LandingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LandingComponent],
      providers: [
        provideRouter([]),
        importProvidersFrom(
          LucideAngularModule.pick({
            GraduationCap, Globe, FileText, Monitor, Ruler, Search, User, Play, Settings, Menu, X, ShieldCheck
          })
        )
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LandingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render the landing component', () => {
    expect(component).toBeTruthy();
  });

  it('should not contain literal #ffffff or #F8FAF8 surface styles in major section wrappers', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const allDivs = Array.from(compiled.querySelectorAll('div[style]')) as HTMLElement[];

    const offendingSurfaces = allDivs.filter(div => {
      const style = div.getAttribute('style') || '';
      return (
        style.includes('background:#ffffff') ||
        style.includes('background: #ffffff') ||
        style.includes('background:#F8FAF8') ||
        style.includes('background: #F8FAF8')
      );
    });

    expect(offendingSurfaces.length).toBe(0);
  });

  it('should use semantic h2 headings instead of div.h2 (DS-03 / W1-14)', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const divH2s = compiled.querySelectorAll('div.h2');
    expect(divH2s.length).toBe(0);

    const realH2s = compiled.querySelectorAll('h2');
    expect(realH2s.length).toBeGreaterThanOrEqual(5);
  });

  it('should maintain a valid heading hierarchy starting with h1', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const h1s = compiled.querySelectorAll('h1');
    expect(h1s.length).toBe(1);

    const headings = Array.from(compiled.querySelectorAll('h1, h2, h3, h4, h5, h6'));
    expect(headings[0].tagName.toLowerCase()).toBe('h1');
    expect(headings[1].tagName.toLowerCase()).toBe('h2');
  });

  describe('Landing Truth & Navigation (DS-04 / W1-15)', () => {
    it('should unhide the desktop navigation bar and provide semantic links', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const nav = compiled.querySelector('nav.desktop-nav') as HTMLElement;
      expect(nav).toBeTruthy();

      const inlineStyle = nav.getAttribute('style') || '';
      expect(inlineStyle.toLowerCase()).not.toContain('display:none');
      expect(inlineStyle.toLowerCase()).not.toContain('display: none');

      const links = Array.from(nav.querySelectorAll('a'));
      expect(links.length).toBeGreaterThanOrEqual(4);
      expect(links.every(link => link.getAttribute('href') || link.getAttribute('routerlink') || link.getAttribute('ng-reflect-router-link'))).toBe(true);
    });

    it('should provide a mobile menu toggle button and drawer', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const mobileBtn = compiled.querySelector('.mobile-menu-btn') as HTMLButtonElement;
      expect(mobileBtn).toBeTruthy();

      expect(component.isMobileMenuOpen).toBe(false);
      mobileBtn.click();
      fixture.detectChanges();
      expect(component.isMobileMenuOpen).toBe(true);

      const mobileNav = compiled.querySelector('.mobile-nav') as HTMLElement;
      expect(mobileNav).toBeTruthy();
    });

    it('should eliminate dead cursor:pointer spans without navigation', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const allSpans = Array.from(compiled.querySelectorAll('span')) as HTMLElement[];
      const deadCursorSpans = allSpans.filter(span => {
        const style = span.getAttribute('style') || '';
        const isPointer = style.includes('cursor:pointer') || style.includes('cursor: pointer');
        const hasLink = span.hasAttribute('routerLink') || span.hasAttribute('href') || span.getAttribute('role') === 'button';
        return isPointer && !hasLink;
      });

      expect(deadCursorSpans.length).toBe(0);
    });

    it('should ensure "Register your college" CTA has a routerLink', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const buttons = Array.from(compiled.querySelectorAll('button, a')) as HTMLElement[];
      const collegeCta = buttons.find(b => b.textContent?.includes('Register your college'));
      expect(collegeCta).toBeTruthy();
      expect(collegeCta?.getAttribute('routerlink') || collegeCta?.getAttribute('ng-reflect-router-link')).toBeTruthy();
    });

    it('should eliminate invented numbers (1,200+, 85+, 40k+)', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const text = compiled.textContent || '';
      expect(text).not.toContain('1,200+');
      expect(text).not.toContain('85+');
      expect(text).not.toContain('40k+');
    });

    it('should display the institutional verified seal badge in the hero section (DS-08 / DS-09)', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const badge = compiled.querySelector('.hero-section .verified-seal-badge');
      expect(badge).toBeTruthy();
      expect(badge?.textContent).toContain('Institutional Verification Standard');
    });

    it('should link to certificate verification in the footer', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const verifyLink = compiled.querySelector('footer a[routerLink="/verify-certificate"]');
      expect(verifyLink).toBeTruthy();
      expect(verifyLink?.textContent).toContain('Verify Certificate');
    });
  });
});

