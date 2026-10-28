import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { VerifyCertificateComponent } from './verify-certificate.component';
import { CertificateService } from '../../student/services/certificate.service';
import { LucideAngularModule, ShieldAlert, Award, Search, CheckCircle2, Download } from 'lucide-angular';

describe('VerifyCertificateComponent Verified Identity Proof Screen (DS-08 / W2-16)', () => {
  let component: VerifyCertificateComponent;
  let fixture: ComponentFixture<VerifyCertificateComponent>;
  let mockCertService: { verifyCertificate: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    mockCertService = {
      verifyCertificate: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [
        VerifyCertificateComponent,
        LucideAngularModule.pick({ ShieldAlert, Award, Search, CheckCircle2, Download })
      ],
      providers: [
        provideRouter([]),
        { provide: CertificateService, useValue: mockCertService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(VerifyCertificateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders verify credential portal shell with official registry badge', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.portal-brand')?.textContent).toContain('LearnSphere');
    expect(compiled.querySelector('.header-tag')?.textContent).toContain('Official Verification Registry');
    expect(compiled.querySelector('.hero-title')?.textContent).toContain('Verify Official Academic Certificate');
  });

  it('displays authentic verified certificate details and tabular code when valid', () => {
    mockCertService.verifyCertificate.mockReturnValue(of({
      valid: true,
      verificationCode: 'LS-2026-ENG-9876',
      studentName: 'Rahul Verma',
      courseTitle: 'Advanced Data Structures & Algorithms',
      instructorName: 'Prof. K. Sharma',
      issuedAt: '2026-05-15T10:00:00Z',
      grade: 'Distinction (94%)',
      message: 'Certificate officially registered under Mumbai University affiliation.'
    }));

    component.searchCode = 'LS-2026-ENG-9876';
    component.onSearch();
    fixture.detectChanges();

    expect(mockCertService.verifyCertificate).toHaveBeenCalledWith('LS-2026-ENG-9876');
    expect(component.result?.valid).toBe(true);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.cert-card-valid')).toBeTruthy();
    expect(compiled.querySelector('.status-banner.valid-banner')?.textContent).toContain('Authentic Certificate Verified');
    expect(compiled.querySelector('.student-val')?.textContent).toContain('Rahul Verma');
    expect(compiled.querySelector('.code-val')?.textContent).toContain('LS-2026-ENG-9876');
  });

  it('displays not recognized warning card on invalid certificate ID or verification error', () => {
    mockCertService.verifyCertificate.mockReturnValue(throwError(() => new Error('Not found')));

    component.searchCode = 'INVALID-CODE';
    component.onSearch();
    fixture.detectChanges();

    expect(component.result?.valid).toBe(false);
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.cert-card-invalid')).toBeTruthy();
    expect(compiled.querySelector('.invalid-banner')?.textContent).toContain('Certificate Not Recognized');
  });
});
