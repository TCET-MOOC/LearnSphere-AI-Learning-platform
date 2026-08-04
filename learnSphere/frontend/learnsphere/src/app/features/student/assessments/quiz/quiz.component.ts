import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AssessmentService } from '../../services/assessment.service';
import { AssessmentDetail, AttemptReview } from '@core/models/assessment.model';
import { NotificationService } from '@core/services/notification.service';
import { QuizInterfaceComponent, AnswerChange } from './components/quiz-interface.component';
import { AssessmentReviewComponent } from './components/assessment-review.component';
import { RemedialCardComponent } from './components/remedial-card.component';

type QuizStage = 'loading' | 'intro' | 'taking' | 'submitting' | 'review' | 'error';

/**
 * QuizComponent is the orchestrator for the quiz-taking flow: it loads the
 * test, lets the student begin an attempt, hands questions off to
 * QuizInterfaceComponent, persists each answer as it's given, and finally
 * finalizes the attempt and shows the AssessmentReviewComponent.
 *
 * Route: /student/assessments/:id (optionally with ?attemptId=123 to jump
 * straight to reviewing a past attempt instead of starting a new one).
 */
@Component({
  selector: 'app-quiz',
  standalone: true,
  imports: [CommonModule, RouterModule, QuizInterfaceComponent, AssessmentReviewComponent, RemedialCardComponent],
  templateUrl: './quiz.component.html',
  styleUrls: ['./quiz.component.scss']
})
export class QuizComponent implements OnInit {
  stage: QuizStage = 'loading';
  assessment: AssessmentDetail | null = null;
  attemptId: number | null = null;
  review: AttemptReview | null = null;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private assessmentService: AssessmentService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = Number(params.get('id'));
      if (!id) {
        this.stage = 'error';
        this.errorMessage = 'No test specified.';
        return;
      }
      const existingAttemptId = Number(this.route.snapshot.queryParamMap.get('attemptId'));
      this.load(id, existingAttemptId || null);
    });
  }

  private load(testId: number, attemptId: number | null): void {
    this.stage = 'loading';
    this.assessmentService.getAssessment(testId).subscribe({
      next: (assessment) => {
        this.assessment = assessment;
        if (attemptId) {
          this.attemptId = attemptId;
          this.loadReview(attemptId);
        } else {
          this.stage = 'intro';
        }
      },
      error: () => {
        this.stage = 'error';
        this.errorMessage = 'Could not load this test.';
      }
    });
  }

  beginAttempt(): void {
    if (!this.assessment) return;
    this.assessmentService.startAssessment(this.assessment.id).subscribe({
      next: (attempt) => {
        this.attemptId = attempt.attemptId;
        this.stage = 'taking';
      },
      error: () => this.notificationService.error('Could not start the test attempt.')
    });
  }

  onAnswerChanged(change: AnswerChange): void {
    if (!this.attemptId) return;
    this.assessmentService.submitAnswer(this.attemptId, change.questionId, change.answerText).subscribe({
      error: () => this.notificationService.error('Could not save that answer — please retry.')
    });
  }

  onFinish(): void {
    if (!this.attemptId) return;
    this.stage = 'submitting';
    this.assessmentService.finalizeAssessment(this.attemptId).subscribe({
      next: (review) => {
        this.review = review;
        this.stage = 'review';
      },
      error: () => {
        this.notificationService.error('Could not submit the test.');
        this.stage = 'taking';
      }
    });
  }

  private loadReview(attemptId: number): void {
    this.assessmentService.getAttemptReview(attemptId).subscribe({
      next: (review) => {
        this.review = review;
        this.stage = 'review';
      },
      error: () => {
        this.stage = 'error';
        this.errorMessage = 'Could not load this attempt.';
      }
    });
  }

  backToList(): void {
    this.router.navigate(['/student/assessments']);
  }
}
