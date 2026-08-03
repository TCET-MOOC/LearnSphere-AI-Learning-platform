import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

interface AdminUser {
  initials: string;
  name: string;
  email: string;
  role: 'Student' | 'Teacher' | 'External';
  department: string;
  status: 'Active' | 'Teacher' | 'Flagged' | 'Blacklisted';
  tone: 'purple' | 'green' | 'amber' | 'red' | 'grey';
  meta: string;
}

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss']
})
export class UsersComponent {
  tabs = ['All users (3,840)', 'Students (3,420)', 'Teachers (398)', 'Flagged (22)', 'Blacklisted (6)'];
  activeTab = 'All users (3,840)';

  summaryCards = [
    { label: 'Total users', value: '3,840', sub: '+48 today', tone: 'green' },
    { label: 'Students', value: '3,420', sub: 'Across 8 colleges', tone: 'purple' },
    { label: 'Teachers', value: '398', sub: '62 active courses', tone: 'blue' },
    { label: 'Blacklisted', value: '6', sub: 'Permanent ban', tone: 'red' }
  ];

  users: AdminUser[] = [
    {
      initials: 'RS',
      name: 'Raj Shah',
      email: 'raj.shah@mhcollege.edu.in',
      role: 'Student',
      department: 'IT Dept',
      status: 'Active',
      tone: 'purple',
      meta: 'Last active 12 min ago'
    },
    {
      initials: 'PK',
      name: 'Priya Kulkarni',
      email: 'priya.k@mhcollege.edu.in',
      role: 'Student',
      department: 'IT Dept',
      status: 'Active',
      tone: 'amber',
      meta: 'Certificate downloaded today'
    },
    {
      initials: 'AS',
      name: 'Prof. A. Sharma',
      email: 'a.sharma@mhcollege.edu.in',
      role: 'Teacher',
      department: 'Math Dept',
      status: 'Teacher',
      tone: 'purple',
      meta: '5 active courses'
    },
    {
      initials: 'VD',
      name: 'Vikram Desai',
      email: 'v.desai@mhcollege.edu.in',
      role: 'Student',
      department: 'Computer Science',
      status: 'Flagged',
      tone: 'red',
      meta: 'AI flagged · 1 warning'
    },
    {
      initials: 'KP',
      name: 'Kiran Pawar',
      email: 'k.pawar@ext.edu',
      role: 'External',
      department: 'External',
      status: 'Blacklisted',
      tone: 'grey',
      meta: 'Permanent restriction'
    }
  ];

  activity = [
    { label: 'Daily active', value: 62, tone: 'purple' },
    { label: 'Weekly active', value: 81, tone: 'green' },
    { label: 'Inactive 30d+', value: 14, tone: 'red' }
  ];

  setTab(tab: string): void {
    this.activeTab = tab;
  }

  getStatusClass(status: AdminUser['status']): string {
    return {
      Active: 'pill--green',
      Teacher: 'pill--purple',
      Flagged: 'pill--red',
      Blacklisted: 'pill--dark'
    }[status];
  }
}
