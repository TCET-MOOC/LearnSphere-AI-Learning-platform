import { Component, EventEmitter, Input, OnInit, Output, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AssessmentService } from '../../../services/assessment.service';
import { Assessment, AssessmentDetail, AttemptReview, TestAttempt } from '@core/models/assessment.model';
import { 
  LucideAngularModule, 
  HelpCircle, 
  CheckCircle2, 
  XCircle, 
  RotateCcw, 
  Award, 
  Sparkles, 
  ArrowRight, 
  ArrowLeft, 
  Check, 
  X,
  Clock
} from 'lucide-angular';

@Component({
  selector: 'app-post-video-quiz',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './post-video-quiz.component.html',
  styleUrls: ['./post-video-quiz.component.scss']
})
export class PostVideoQuizComponent implements OnInit {
  @Input() lectureId!: number;
  @Input() lectureTitle: string = '';
  @Input() courseId?: number;

  @Output() quizPassed = new EventEmitter<number>();
  @Output() close = new EventEmitter<void>();

  loading = true;
  quiz: Assessment | null = null;
  quizDetail: AssessmentDetail | null = null;
  attempt: TestAttempt | null = null;
  review: AttemptReview | null = null;

  currentIndex = 0;
  selectedAnswers: { [questionId: number]: string } = {};
  submitting = false;
  hasSubmitted = false;

  constructor(
    private assessmentService: AssessmentService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadLectureQuiz();
  }

  loadLectureQuiz(): void {
    this.loading = true;
    this.hasSubmitted = false;
    this.review = null;
    this.selectedAnswers = {};
    this.currentIndex = 0;

    this.assessmentService.getAssessmentsByLecture(this.lectureId).subscribe({
      next: (tests) => {
        if (tests && tests.length > 0) {
          this.quiz = tests[0];
          this.startQuiz(this.quiz.id);
        } else {
          this.quiz = null;
          this.loading = false;
          this.cdr.markForCheck();
        }
      },
      error: () => {
        this.quiz = null;
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private startQuiz(testId: number): void {
    this.assessmentService.getAssessment(testId).subscribe({
      next: (detail) => {
        this.quizDetail = detail;
        this.assessmentService.startAssessment(testId).subscribe({
          next: (att) => {
            this.attempt = att;
            this.loading = false;
            this.cdr.markForCheck();
          },
          error: () => {
            this.loading = false;
            this.cdr.markForCheck();
          }
        });
      },
      error: () => {
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  selectOption(questionId: number, option: string): void {
    if (this.hasSubmitted) return;
    this.selectedAnswers[questionId] = option;
    this.cdr.markForCheck();
  }

  nextQuestion(): void {
    if (this.quizDetail && this.currentIndex < this.quizDetail.questions.length - 1) {
      this.currentIndex++;
    }
  }

  prevQuestion(): void {
    if (this.currentIndex > 0) {
      this.currentIndex--;
    }
  }

  get currentQuestion() {
    return this.quizDetail?.questions?.[this.currentIndex] || null;
  }

  get allAnswered(): boolean {
    if (!this.quizDetail?.questions) return false;
    return this.quizDetail.questions.every(q => !!this.selectedAnswers[q.questionId]);
  }

  submitQuiz(): void {
    if (!this.attempt || !this.quizDetail || this.submitting) return;
    this.submitting = true;

    const answersToSubmit = this.quizDetail.questions.map(q => ({
      questionId: q.questionId,
      answerText: this.selectedAnswers[q.questionId] || ''
    }));

    // Submit answers sequentially
    const submitNext = (index: number) => {
      if (index >= answersToSubmit.length) {
        this.assessmentService.finalizeAssessment(this.attempt!.attemptId).subscribe({
          next: (rev) => {
            this.review = rev;
            this.hasSubmitted = true;
            this.submitting = false;

            const score = rev.score ?? 0;
            const maxScore = rev.maxScore || 0;
            const percentage = maxScore > 0 ? (score / maxScore) * 100 : 100;
            if (percentage >= 60) {
              this.quizPassed.emit(this.lectureId);
            }
            this.cdr.markForCheck();
          },
          error: () => {
            this.submitting = false;
            this.cdr.markForCheck();
          }
        });
        return;
      }

      const item = answersToSubmit[index];
      this.assessmentService.submitAnswer(this.attempt!.attemptId, item.questionId, item.answerText).subscribe({
        next: () => submitNext(index + 1),
        error: () => submitNext(index + 1)
      });
    };

    submitNext(0);
  }

  retakeQuiz(): void {
    if (this.quiz) {
      this.startQuiz(this.quiz.id);
    }
  }

  isAnswerCorrect(ans: any): boolean {
    return (ans?.marksAwarded ?? 0) > 0;
  }

  get scorePercentage(): number {
    if (!this.review) return 0;
    const score = this.review.score ?? 0;
    const max = this.review.maxScore || 1;
    return Math.round((score / max) * 100);
  }

  get isPassed(): boolean {
    return this.scorePercentage >= 60;
  }
}
