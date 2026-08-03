import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-sentiment',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sentiment.component.html',
  styleUrls: ['./sentiment.component.scss']
})
export class SentimentComponent implements OnInit {
  
  // Tab State
  activeTab = 'Platform-wide';
  tabs = ['Platform-wide', 'By course', 'By teacher', 'By college'];

  // Mock Data matching the UI
  courseTypeSentiment = [
    { type: 'Math / Science', positive: 76, color: '#0f9d58' },
    { type: 'Engineering', positive: 71, color: '#534AB7' },
    { type: 'CS / IT', positive: 80, color: '#0d47a1' },
    { type: 'Remedial', positive: 52, color: '#b06000' }
  ];

  negativeKeywords = [
    { word: 'difficult', count: 214, severity: 'high' },
    { word: 'confusing', count: 188, severity: 'high' },
    { word: 'slow pace', count: 144, severity: 'medium' },
    { word: 'audio issue', count: 112, severity: 'medium' },
    { word: 'incomplete', count: 98, severity: 'medium' },
    { word: 'boring', count: 76, severity: 'low' },
    { word: 'too fast', count: 64, severity: 'low' },
    { word: 'rude', count: 22, severity: 'high' }
  ];

  recommendations = [
    {
      title: '"audio issue" spike in Thermodynamics',
      description: '14 comments mention low audio in Lec 4–6',
      action: 'Notify teacher',
      actionClass: 'btn-outline-amber'
    },
    {
      title: '"rude" keyword in DSA discussion',
      description: 'Possible conflict between students — 3 posts',
      action: 'Review posts',
      actionClass: 'btn-outline-grey'
    }
  ];

  teacherScores = [
    { initials: 'AS', name: 'Prof. A. Sharma', comments: 142, score: '97% positive', status: 'positive' },
    { initials: 'SM', name: 'Prof. S. Mehta', comments: 118, score: '91% positive', status: 'positive' },
    { initials: 'RP', name: 'Prof. R. Patil', comments: 88, score: '68% positive', status: 'warning' }
  ];

  constructor() { }

  ngOnInit(): void { }

  setTab(tab: string) {
    this.activeTab = tab;
  }
}