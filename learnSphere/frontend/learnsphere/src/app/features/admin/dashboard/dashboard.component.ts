import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent {
  kpiCards = [
    { label: 'Total revenue', value: '₹4.2L', sub: '+18% this month', subColor: '#0F6E56' },
    { label: 'Registered users', value: '3,840', sub: '+48 today', subColor: '#534AB7' },
    { label: 'Active courses', value: '62', sub: '7 pending review', subColor: '#D08C1A' },
    { label: 'Flagged items', value: '14', sub: 'Needs action', subColor: '#E5453A' }
  ];

  topTeachers = [
    { rank: '🥇', initials: 'AS', name: 'Prof. A. Sharma', stats: '486 students · ★4.9', earnings: '₹18.4k', bg: '#EEEDFE', color: '#534AB7' },
    { rank: '🥈', initials: 'SM', name: 'Prof. S. Mehta', stats: '410 students · ★4.8', earnings: '₹14.2k', bg: '#EAF3DE', color: '#27500A' },
    { rank: '🥉', initials: 'RP', name: 'Prof. R. Patil', stats: '298 students · ★4.7', earnings: '₹9.8k', bg: '#E1F5EE', color: '#085041' }
  ];

  topStudents = [
    { rank: '🥇', initials: 'PK', name: 'Priya Kulkarni', pts: '2,840 pts', bg: '#FAEEDA', color: '#633806' },
    { rank: '🥈', initials: 'AM', name: 'Arjun Mishra', pts: '2,710 pts', bg: '#EAF3DE', color: '#27500A' },
    { rank: '🥉', initials: 'NJ', name: 'Nisha Joshi', pts: '1,940 pts', bg: '#E1F5EE', color: '#085041' }
  ];

  courseApprovals = [
    { title: 'Statistics for Engineers', meta: 'Prof. Sharma · 8 lectures', icon: '📐' },
    { title: 'Fluid Mechanics', meta: 'Prof. Nair · 12 lectures', icon: '⚗️' },
    { title: 'Machine Learning Basics', meta: 'Prof. Gupta · 18 lectures', icon: '🤖' }
  ];
}