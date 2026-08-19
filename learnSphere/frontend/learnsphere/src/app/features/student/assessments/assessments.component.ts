import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AssessmentService } from '../services/assessment.service';
import { StudentService } from '../services/student.service';
import { Assessment } from '@core/models/assessment.model';
import { NotificationService } from '@core/services/notification.service';

import { LucideAngularModule } from 'lucide-angular';

/**
 * AssessmentsComponent lists the tests/quizzes available across every course
 * the current student is enrolled in, with a "Start" button that hands off
 * to the quiz-taking flow.
 */
@Component({
  selector: 'app-assessments',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  templateUrl: './assessments.component.html',
  styleUrls: ['./assessments.component.scss']
})
export class AssessmentsComponent implements OnInit {
  assessments: Assessment[] = [];
  loading = true;

  constructor(
    private assessmentService: AssessmentService,
    private studentService: StudentService,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.studentService.getEnrolledCourses().subscribe({
      next: (courses) => {
        if (courses.length === 0) {
          this.assessments = [];
          this.loading = false;
          this.cdr.markForCheck();
          return;
        }
        forkJoin(
          courses.map((course) =>
            this.assessmentService.getAssessments(course.id).pipe(catchError(() => of([] as Assessment[])))
          )
        ).subscribe({
          next: (lists) => {
            this.assessments = lists.flat().sort((a, b) => (a.isRemedial ? 1 : 0) - (b.isRemedial ? 1 : 0));
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
        this.notificationService.error('Could not load your courses.');
      }
    });
  }

  get standardAssessments(): Assessment[] {
    return this.assessments.filter((a) => !a.isRemedial);
  }

  get remedialAssessments(): Assessment[] {
    return this.assessments.filter((a) => a.isRemedial);
  }
}
