import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { SentimentService } from '../services/sentiment.service';
import { NotificationService } from '@core/services/notification.service';

@Component({
  selector: 'app-sentiment',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './sentiment.component.html',
  styleUrls: ['./sentiment.component.scss']
})
export class SentimentComponent implements OnInit {

  activeTab = 'Platform-wide';
  tabs = ['Platform-wide', 'By course', 'By teacher', 'By college'];

  loading = false;
  totalAnalyzed = 0;
  positivePercent = 0;
  neutralPercent = 0;
  negativePercent = 0;
  courseTypeSentiment: { type: string; positive: number; color: string }[] = [];
  negativeKeywords: { word: string; count: number; severity: string }[] = [];
  recommendations: { title: string; description: string; action: string; actionClass: string }[] = [];
  teacherScores: { initials: string; name: string; comments: number; score: string; status: string }[] = [];

  constructor(
    private sentimentService: SentimentService,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.sentimentService.getSentiment().subscribe({
      next: (summary) => {
        this.totalAnalyzed = summary.totalAnalyzed;
        this.positivePercent = summary.positivePercent;
        this.neutralPercent = summary.neutralPercent;
        this.negativePercent = summary.negativePercent;
        this.courseTypeSentiment = summary.courseTypeSentiment;
        this.negativeKeywords = summary.negativeKeywords;
        this.recommendations = summary.recommendations;
        this.teacherScores = summary.teacherScores;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.notificationService.error('Failed to load sentiment data.');
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  setTab(tab: string): void {
    this.activeTab = tab;
  }

  onRecommendationAction(rec: { title: string; action: string }): void {
    const act = (rec.action + ' ' + rec.title).toLowerCase();
    if (act.includes('teacher') || act.includes('faculty') || act.includes('user')) {
      this.router.navigate(['/admin/users']);
    } else if (act.includes('course') || act.includes('curriculum')) {
      this.router.navigate(['/admin/courses']);
    } else if (act.includes('flag') || act.includes('moderat') || act.includes('spam') || act.includes('bull')) {
      this.router.navigate(['/admin/flagged']);
    } else if (act.includes('college') || act.includes('campus')) {
      this.router.navigate(['/admin/colleges']);
    } else {
      this.notificationService.info(`Action '${rec.action}' initiated.`);
    }
  }
}
