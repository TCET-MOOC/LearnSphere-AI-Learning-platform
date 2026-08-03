import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-royalties',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './royalties.component.html',
  styleUrls: ['./royalties.component.scss']
})
export class RoyaltiesComponent implements OnInit {

  // Mock data matching the UI design
  earningsByCourse = [
    {
      title: 'Engineering Mathematics III',
      enrolled: 312,
      externalPaid: 28,
      amount: '₹11,200',
      status: 'this month'
    },
    {
      title: 'Discrete Mathematics',
      enrolled: 174,
      externalPaid: 20,
      amount: '₹7,200',
      status: 'this month'
    },
    {
      title: 'Statistics for Engineers',
      status: 'Draft — not yet live',
      amount: '—',
      isDraft: true
    }
  ];

  payoutHistory = [
    {
      month: 'May 2026 payout',
      date: 'Transferred 30 May',
      amount: '₹15,100',
      status: 'paid',
      statusClass: 'status-green'
    },
    {
      month: 'Apr 2026 payout',
      date: 'Transferred 30 Apr',
      amount: '₹12,800',
      status: 'paid',
      statusClass: 'status-green'
    },
    {
      month: 'Jun 2026 payout',
      date: 'Scheduled 30 Jun',
      amount: '₹18,400',
      status: 'pending',
      statusClass: 'status-amber'
    }
  ];

  royaltyBreakdown = [
    { label: 'External sales', percentage: 72, color: '#534AB7' }, 
    { label: 'College share', percentage: 20, color: '#0f9d58' },  
    { label: 'Remedial certs', percentage: 8, color: '#b97700' }   
  ];

  constructor() { }

  ngOnInit(): void { }
}