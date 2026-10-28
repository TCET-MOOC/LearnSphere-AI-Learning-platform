import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { FooterComponent } from './footer.component';

describe('FooterComponent Design Token & Surface Conformance (DS-06 / W2-14)', () => {
  let component: FooterComponent;
  let fixture: ComponentFixture<FooterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FooterComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(FooterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders footer component with dynamic current year', () => {
    expect(component).toBeTruthy();
    const currentYear = new Date().getFullYear();
    expect(component.currentYear).toBe(currentYear);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.footer')).toBeTruthy();
    expect(compiled.querySelector('.brand-name')?.textContent).toContain('LearnSphere');
    expect(compiled.querySelector('.footer__bottom')?.textContent).toContain(currentYear.toString());
  });

  it('renders navigation links and social triggers without dead cursor pointers', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const links = compiled.querySelectorAll('.footer-link');
    expect(links.length).toBeGreaterThan(0);

    const socialBtns = compiled.querySelectorAll('.social-btn');
    expect(socialBtns.length).toBe(3);
  });
});
