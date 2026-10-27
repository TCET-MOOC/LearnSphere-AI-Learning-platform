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
});
