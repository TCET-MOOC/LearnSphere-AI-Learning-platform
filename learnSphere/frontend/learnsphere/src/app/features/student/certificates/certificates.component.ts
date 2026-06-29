import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-certificates',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './certificates.component.html',
  styleUrls: ['./certificates.component.scss']
})
export class CertificatesComponent implements OnInit {
  
  earnedCerts = [
    { 
      courseName: 'Operating Systems', 
      date: '15 Jun 2026', 
      teacher: 'Prof. Kulkarni', 
      icon: '🏆'
    },
    { 
      courseName: 'Web Technologies', 
      date: '10 Jun 2026', 
      teacher: 'Prof. Joshi',
      icon: '🎓'
    },
    { 
      courseName: 'Python Fundamentals', 
      date: '1 Jun 2026', 
      teacher: 'Prof. Mehta',
      icon: '✅'
    }
  ];

  inProgressCerts = [
    { 
      courseName: 'Engineering Mathematics III', 
      requirement: 'Complete all 20 lectures + pass final quiz', 
      progressValue: 40, 
      progressText: '40% done · 8/20 lectures' 
    }
  ];

  remedialCerts = [
    { 
      courseName: 'Thermodynamics', 
      requirement: 'Complete 30-question certification exam', 
      statusText: 'Not attempted — needed for attendance' 
    }
  ];

  constructor() { }

  ngOnInit(): void { }
}