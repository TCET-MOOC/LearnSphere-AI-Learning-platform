import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { importProvidersFrom } from '@angular/core';
import { LucideAngularModule, GraduationCap, Globe, FileText, Monitor, Ruler, Search, User, Play, Settings } from 'lucide-angular';
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
            GraduationCap, Globe, FileText, Monitor, Ruler, Search, User, Play, Settings
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
});
