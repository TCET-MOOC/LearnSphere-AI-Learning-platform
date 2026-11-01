import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import {
  LucideAngularModule,
  ArrowUpRight,
  CheckCircle,
  CreditCard,
  Download,
  FileText,
  Printer,
  RefreshCw,
  ShoppingBag,
  X
} from 'lucide-angular';
import { PurchasesComponent } from './purchases.component';
import { PaymentsService } from '../services/payments.service';
import { StudentService } from '../services/student.service';
import { AuthService } from '@core/auth/auth.service';
import { PdfExportService } from '@core/services/pdf-export.service';

/** W6-13 / AUD-01 (audit.md A3–A5): receipt modal + PDF use real ledger data, PAID rows only. */
describe('PurchasesComponent receipt (W6-13)', () => {
  let fixture: ComponentFixture<PurchasesComponent>;
  let component: PurchasesComponent;
  let pdf: { generateStudentReceipt: ReturnType<typeof vi.fn> };
  let microserviceOrders: any[];

  const history = [
    {
      id: 42, courseId: 7, courseTitle: 'Distributed Systems', department: 'Computer Science',
      amount: 2500, currency: 'INR', status: 'PAID',
      gatewayOrderId: 'order_A', gatewayPaymentId: 'pay_A',
      createdAt: '2026-11-01T14:00:00', paidAt: '2026-11-01T14:05:09'
    },
    {
      id: 43, courseId: 8, courseTitle: 'Compilers', department: 'Computer Science',
      amount: 999, currency: 'INR', status: 'CREATED',
      gatewayOrderId: 'order_B', gatewayPaymentId: null,
      createdAt: '2026-11-01T15:00:00', paidAt: null
    }
  ];

  async function setUp() {
    pdf = { generateStudentReceipt: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [
        PurchasesComponent,
        LucideAngularModule.pick({
          ArrowUpRight, CheckCircle, CreditCard, Download, FileText, Printer, RefreshCw, ShoppingBag, X
        })
      ],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { currentUser: { id: 2, fullName: 'Asha Rao', email: 'asha@example.test' } } },
        { provide: PaymentsService, useValue: { getPaymentHistory: () => of(history), getMyOrders: () => of(microserviceOrders) } },
        { provide: StudentService, useValue: { getPublicCourse: vi.fn() } },
        { provide: PdfExportService, useValue: pdf }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PurchasesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => {
    microserviceOrders = [];
    await setUp();
  });

  const orderFor = (courseId: number) => component.orders.find((o) => o.courseId === courseId)!;
  const text = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

  it('offers a receipt only for a ledger row that is really PAID', () => {
    expect(orderFor(7).receipt).toBeTruthy();
    expect(orderFor(8).receipt).toBeNull();
    const buttons = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'))
      .filter((b) => b.textContent?.includes('Receipt'));
    expect(buttons.length).toBe(1);
  });

  it('keeps the ledger receipt when the payments service also reports the order as paid', async () => {
    TestBed.resetTestingModule();
    microserviceOrders = [
      { id: 900, razorpayOrderId: 'order_A', userId: 2, courseId: 7, amount: 250000, currency: 'INR', status: 'PAID' }
    ];
    await setUp();
    expect(orderFor(7).receipt?.paymentId).toBe('pay_A');
  });

  describe('history (W6-14 / AUD-02)', () => {
    const cards = () => Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.order-card'));
    const cardFor = (title: string) => cards().find((c) => c.textContent?.includes(title))!;

    it('labels each row with its real status: Paid / Pending', () => {
      expect(cardFor('Distributed Systems').querySelector('.order-pill')!.textContent!.trim()).toBe('Paid');
      expect(cardFor('Compilers').querySelector('.order-pill')!.textContent!.trim()).toBe('Pending');
    });

    it('counts and totals only real paid rows', () => {
      expect(component.summary).toMatchObject({ all: 2, paid: 1, pending: 1, failed: 0, paidCourses: 1, totalPaidRupees: 2500 });
      const stats = (fixture.nativeElement as HTMLElement).querySelector('.stats-grid')!.textContent!;
      expect(stats).toContain('2,500');
    });

    it('shows the pending row under the Pending filter', () => {
      component.setFilter('PENDING');
      fixture.detectChanges();
      expect(cards().map((c) => c.querySelector('.course-title')!.textContent!.trim())).toEqual(['Compilers']);
    });

    it('shows the payment id and capture time on a paid row', () => {
      const meta = cardFor('Distributed Systems').querySelector('.order-meta')!.textContent!;
      expect(meta).toContain('pay_A');
      expect(meta).toContain('01 Nov 2026, 14:05:09');
    });

    it('keeps a failed gateway attempt alongside the paid ledger row for the same course', async () => {
      TestBed.resetTestingModule();
      microserviceOrders = [
        { id: 901, razorpayOrderId: 'order_FAILED', userId: 2, courseId: 7, amount: 250000, currency: 'INR', status: 'FAILED', createdAt: '2026-11-01T13:00:00' }
      ];
      await setUp();
      expect(component.summary).toMatchObject({ all: 3, paid: 1, failed: 1, pending: 1 });
      expect(component.orders.filter((o) => o.courseId === 7).map((o) => o.status).sort()).toEqual(['FAILED', 'PAID']);
    });

    it('names an untitled attempt from a sibling row and never overwrites a known title', async () => {
      TestBed.resetTestingModule();
      microserviceOrders = [
        { id: 901, razorpayOrderId: 'order_FAILED', userId: 2, courseId: 7, amount: 250000, currency: 'INR', status: 'FAILED', createdAt: '2026-11-01T13:00:00' }
      ];
      await setUp();
      const course7 = component.orders.filter((o) => o.courseId === 7);
      expect(course7.map((o) => o.courseTitle)).toEqual(['Distributed Systems', 'Distributed Systems']);
      expect(TestBed.inject(StudentService).getPublicCourse).not.toHaveBeenCalled();
    });
  });

  it('does not open a receipt for a row without one', () => {
    component.openReceipt(orderFor(8));
    expect(component.selectedReceipt).toBeNull();
  });

  it('shows the real receipt number, order and payment ids, and paid date and time in the modal', () => {
    component.openReceipt(orderFor(7));
    fixture.detectChanges();
    const body = (fixture.nativeElement as HTMLElement).querySelector('#printable-receipt')!.textContent!;
    expect(body).toContain('RCPT-42');
    expect(body).toContain('order_A');
    expect(body).toContain('pay_A');
    expect(body).toContain('01 Nov 2026, 14:05:09');
    expect(body).toContain('Paid');
    expect(body).toContain('INR 2,500.00');
  });

  it('makes no tax claim and shows no fabricated INV- number', () => {
    component.openReceipt(orderFor(7));
    fixture.detectChanges();
    expect(text()).not.toMatch(/INV-|Tax|GST/);
  });

  it('downloads the same receipt the modal shows', () => {
    component.openReceipt(orderFor(7));
    component.downloadPdfReceipt();
    expect(pdf.generateStudentReceipt).toHaveBeenCalledTimes(1);
    expect(pdf.generateStudentReceipt.mock.calls[0][0]).toBe(component.selectedReceipt);
    expect(pdf.generateStudentReceipt.mock.calls[0][0]).toMatchObject({
      receiptNumber: 'RCPT-42', orderId: 'order_A', paymentId: 'pay_A',
      paidAtLabel: '01 Nov 2026, 14:05:09', studentEmail: 'asha@example.test'
    });
  });
});
