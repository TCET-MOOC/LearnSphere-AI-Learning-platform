import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SentimentService } from '../services/sentiment.service';
import { NotificationService } from '@core/services/notification.service';

@Component({
  selector: 'app-sentiment',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sentiment.component.html',
  styleUrls: ['./sentiment.component.scss']
})
export class SentimentComponent implements OnInit {

  // Tab is display-only: the backend currently returns one platform-wide
  // snapshot, so switching tabs doesn't refetch a different slice yet.
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
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
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
      },
      error: () => {
        this.notificationService.error('Failed to load sentiment data.');
        this.loading = false;
      }
    });
  }

  setTab(tab: string) {
    this.activeTab = tab;
  }
}
