import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LucideAngularModule, ClipboardCheck, Plus } from 'lucide-angular';
import { TeacherAssessmentsComponent } from './assessments.component';
import { TeacherAssessmentService } from '../services/assessment.service';
import { NotificationService } from '@core/services/notification.service';

/**
 * W7-16 (SCR-35; W4-05, AC-17): the teacher assessments screen lists the teacher's tests and the
 * TEXT answers waiting for a grade, grades through `PATCH /answers/{id}/grade` and shows the
 * server's state afterwards. Scores outside 0…max never reach the server.
 */
describe('TeacherAssessmentsComponent (W7-16)', () => {
  let fixture: ComponentFixture<TeacherAssessmentsComponent>;
  let el: HTMLElement;
  let api: Record<string, ReturnType<typeof vi.fn>>;
  let notify: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn>; info: ReturnType<typeof vi.fn> };

  const TESTS = [
    { testId: 4, title: 'Essay check', courseId: 1, courseTitle: 'Distributed Systems', lectureId: null, lectureTitle: null,
      questionCount: 2, textQuestionCount: 1, pendingCount: 1 },
    { testId: 5, title: 'Quick quiz', courseId: 1, courseTitle: 'Distributed Systems', lectureId: 9, lectureTitle: 'CAP theorem',
      questionCount: 0, textQuestionCount: 0, pendingCount: 0 },
  ];
  const PENDING = [
    { answerId: 31, attemptId: 7, testId: 4, testTitle: 'Essay check', courseTitle: 'Distributed Systems', studentId: 2,
      studentName: 'Aarav Sharma', questionId: 12, questionText: 'Explain eventual consistency.', maxMarks: 10,
      answerText: 'They agree eventually.', submittedAt: '2026-11-01T10:20:00' },
  ];

  async function setUp(overrides: Record<string, ReturnType<typeof vi.fn>> = {}) {
    api = {
      getTeacherAssessments: vi.fn(() => of(TESTS)),
      getPendingAnswers: vi.fn(() => of(PENDING)),
      gradeAnswer: vi.fn(() => of({ studentAnswerId: 31, answerText: 'They agree eventually.', marksAwarded: 7.5 })),
      ...overrides,
    };
    notify = { success: vi.fn(), error: vi.fn(), info: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [TeacherAssessmentsComponent, LucideAngularModule.pick({ ClipboardCheck, Plus })],
      providers: [
        provideRouter([]),
        { provide: TeacherAssessmentService, useValue: api },
        { provide: NotificationService, useValue: notify },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(TeacherAssessmentsComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    el = fixture.nativeElement;
  }

  const settle = async () => { fixture.detectChanges(); await fixture.whenStable(); fixture.detectChanges(); };
  const card = () => el.querySelector('.grading-card') as HTMLElement;
  const marks = () => card().querySelector('input[type="number"]') as HTMLInputElement;
  const saveBtn = () => Array.from(card().querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Save grade') as HTMLButtonElement;
  const typeMarks = (v: string) => { marks().value = v; marks().dispatchEvent(new Event('input')); fixture.detectChanges(); };

  it('lists the teacher\'s tests with question and pending counts', async () => {
    await setUp();
    expect(api['getTeacherAssessments']).toHaveBeenCalledTimes(1);
    const text = el.querySelector('app-data-table')?.textContent ?? '';
    expect(text).toContain('Essay check');
    expect(text).toContain('Distributed Systems');
    expect(text).toContain('CAP theorem');
    expect(el.querySelector('a[href="/teacher/upload"]')?.textContent?.trim()).toBe('Create a test');
  });

  it('shows each answer waiting for a grade with its question, student and the maximum', async () => {
    await setUp();
    expect(card().textContent).toContain('Aarav Sharma');
    expect(card().textContent).toContain('Explain eventual consistency.');
    expect(card().textContent).toContain('They agree eventually.');
    expect(card().textContent).toContain('out of 10');
    expect(marks().max).toBe('10');
  });

  it('grades through the API, then reloads the queue and the counts from the server', async () => {
    await setUp();
    typeMarks('7.5');
    saveBtn().click();
    await settle();
    expect(api['gradeAnswer']).toHaveBeenCalledWith(31, 7.5);
    expect(api['getPendingAnswers']).toHaveBeenCalledTimes(2);
    expect(api['getTeacherAssessments']).toHaveBeenCalledTimes(2);
    expect(notify.success).toHaveBeenCalled();
  });

  it('a score outside 0…max is refused before any request', async () => {
    await setUp();
    typeMarks('11');
    saveBtn().click();
    await settle();
    expect(api['gradeAnswer']).not.toHaveBeenCalled();
    expect(card().textContent).toContain('Enter a score from 0 to 10.');
    typeMarks('-1');
    saveBtn().click();
    await settle();
    expect(api['gradeAnswer']).not.toHaveBeenCalled();
  });

  it('a refused grade shows the server reason and claims no success', async () => {
    await setUp({ gradeAnswer: vi.fn(() => throwError(() => ({ status: 400, error: { detail: 'marksAwarded must be between 0 and 10' } }))) });
    typeMarks('5');
    saveBtn().click();
    await settle();
    expect(notify.success).not.toHaveBeenCalled();
    expect(notify.error).toHaveBeenCalledWith('marksAwarded must be between 0 and 10');
    expect(card()).toBeTruthy();
  });

  it('empty states: nothing waiting, and no tests yet', async () => {
    await setUp({ getPendingAnswers: vi.fn(() => of([])), getTeacherAssessments: vi.fn(() => of([])) });
    expect(el.textContent).toContain('Nothing is waiting for a grade');
    expect(el.textContent).toContain('No tests yet');
    expect(card()).toBeNull();
  });
});
