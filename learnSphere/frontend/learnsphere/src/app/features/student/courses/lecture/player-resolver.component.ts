import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { StudentService } from '../../services/student.service';

/**
 * W3-11 / AUD-08: Canonical course player entry resolver.
 * Route: /student/courses/:courseId/player
 * Resolves the first incomplete content/lecture via getResumeTarget(courseId)
 * and deep-links directly to /student/courses/:courseId/lectures/:lectureId.
 */
@Component({
  selector: 'app-player-resolver',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="player-resolver-shell" style="min-height: 60vh; display: flex; align-items: center; justify-content: center; flex-direction: column; gap: 16px;">
      <div class="player-spinner" style="width: 40px; height: 40px; border: 3px solid var(--border, #e2e8f0); border-top-color: var(--brand, #4f46e5); border-radius: 50%; animation: spin 0.8s linear infinite;"></div>
      <p style="color: var(--text-muted, #64748b); font-size: 0.95rem;">Resuming course playback...</p>
    </div>
  `,
  styles: [`
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class PlayerResolverComponent implements OnInit {

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private studentService: StudentService
  ) {}

  ngOnInit(): void {
    const courseIdParam = this.route.snapshot.paramMap.get('courseId');
    const courseId = Number(courseIdParam);

    if (!courseIdParam || isNaN(courseId)) {
      this.router.navigate(['/student/courses'], { replaceUrl: true });
      return;
    }

    this.studentService.getResumeTarget(courseId).subscribe({
      next: (target) => {
        if (target && target.lectureId) {
          this.router.navigate(['/student/courses', courseId, 'lectures', target.lectureId], { replaceUrl: true });
        } else {
          this.fallbackToFirstLectureOrDetails(courseId);
        }
      },
      error: () => {
        // Course may be 100% complete (404) or user un-enrolled
        this.fallbackToFirstLectureOrDetails(courseId);
      }
    });
  }

  private fallbackToFirstLectureOrDetails(courseId: number): void {
    this.studentService.getCourseLectures(courseId).subscribe({
      next: (lectures) => {
        if (lectures && lectures.length > 0) {
          this.router.navigate(['/student/courses', courseId, 'lectures', lectures[0].id], { replaceUrl: true });
        } else {
          this.router.navigate(['/student/courses', courseId], { replaceUrl: true });
        }
      },
      error: () => {
        this.router.navigate(['/student/courses', courseId], { replaceUrl: true });
      }
    });
  }
}
