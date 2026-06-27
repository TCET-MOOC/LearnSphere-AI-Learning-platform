import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-teacher-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent {
  teacherName = 'Prof. Sharma';

  kpiCards = [
    { label: 'Total students', value: '486', sub: '+12 this week', subColor: '#0F6E56' },
    { label: 'Active courses', value: '5', sub: '2 draft / 1 pending', subColor: '#6B6880' },
    { label: 'Avg. rating', value: '4.7 ★', sub: 'Top 10% platform', subColor: '#0F6E56' },
    { label: 'Royalty this month', value: '₹18,400', sub: 'Payout on 30 Jun', subColor: '#534AB7' }
  ];

  myCourses = [
    { title: 'Engineering Mathematics III', stats: '312 students · ★★★★★ 4.9', status: 'Live', pillClass: 'p-gr', bg: '#EEEDFE', icon: '📐' },
    { title: 'Discrete Mathematics', stats: '174 students · ★★★★ 4.5', status: 'Live', pillClass: 'p-gr', bg: '#E6F1FB', icon: '🔢' },
    { title: 'Statistics for Engineers', stats: '0 students · 8 lectures added', status: 'Draft', pillClass: 'p-am', bg: '#FAEEDA', icon: '📊' },
    { title: 'Numerical Methods', stats: 'Submitted · Admin review', status: 'Pending', pillClass: 'p-re', bg: '#FCEBEB', icon: '🧮' }
  ];

  recentMessages = [
    { initials: 'RS', name: 'Raj Shah', text: 'Can you clarify Laplace lec 8?', time: '10m', bg: '#EEEDFE', color: '#534AB7' },
    { initials: 'PK', name: 'Priya Kulkarni', text: 'Thank you for the feedback!', time: '1h', bg: '#EAF3DE', color: '#27500A' },
    { initials: 'AM', name: 'Arjun Mishra', text: 'When is the next live session?', time: '1d', bg: '#FAEEDA', color: '#633806' }
  ];
}