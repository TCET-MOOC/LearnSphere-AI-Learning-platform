import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

// Mirrors the teacher messages component structure for consistency.
// The only differences: sender field uses 'student' | 'teacher' (instead of
// teacher's 'teacher' | 'student'), and conversations are with teachers
// (not students).

interface Message {
  id: string;
  sender: 'student' | 'teacher';
  text: string;
  time: string;
}

interface Conversation {
  id: string;
  teacherName: string;
  initials: string;
  bg: string;
  color: string;
  course: string;
  lastMessage: string;
  lastTime: string;
  unread: number;
  messages: Message[];
}

@Component({
  selector: 'app-student-messages',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './messages.component.html',
  styleUrls: ['./messages.component.scss']
})
export class MessagesComponent {
  searchText = '';
  activeFilter: 'all' | 'unread' | 'courses' = 'all';
  newMessage = '';

  // Mock conversations — mirrors teacher version's shape and volume.
  // In production these come from MessagingService.getConversations().
  conversations: Conversation[] = [
    {
      id: '1',
      teacherName: 'Prof. Rakesh Sharma',
      initials: 'RS',
      bg: '#EEEDFE',
      color: '#534AB7',
      course: 'Engineering Mathematics III',
      lastMessage: 'I\'ll cover partial fractions again tomorrow.',
      lastTime: '10m ago',
      unread: 1,
      messages: [
        { id: 'm1', sender: 'student', text: 'Hello Professor, I had a doubt from today\'s lecture.', time: '2:30 PM' },
        { id: 'm2', sender: 'teacher', text: 'Sure, which topic are you referring to?', time: '2:35 PM' },
        { id: 'm3', sender: 'student', text: 'The Laplace transform section in lecture 8. I couldn\'t follow the inverse transform step.', time: '2:38 PM' },
        { id: 'm4', sender: 'teacher', text: 'Good question. The key is to use partial fraction decomposition before applying the inverse. I\'ll cover partial fractions again tomorrow.', time: '2:42 PM' }
      ]
    },
    {
      id: '2',
      teacherName: 'Dr. Priya Nair',
      initials: 'PN',
      bg: '#EAF3DE',
      color: '#27500A',
      course: 'Data Structures & Algorithms',
      lastMessage: 'Your assignment scores look great!',
      lastTime: '1h ago',
      unread: 0,
      messages: [
        { id: 'm1', sender: 'student', text: 'Ma\'am, I submitted my assignment yesterday. Could you review it?', time: '11:00 AM' },
        { id: 'm2', sender: 'teacher', text: 'I\'ve reviewed it. Very well written — a few minor corrections in Q3 and Q7. I\'ve left comments.', time: '11:30 AM' },
        { id: 'm3', sender: 'student', text: 'Thank you, I\'ll go through the comments.', time: '11:35 AM' },
        { id: 'm4', sender: 'teacher', text: 'Your assignment scores look great!', time: '11:40 AM' }
      ]
    },
    {
      id: '3',
      teacherName: 'Dr. Sunita Patil',
      initials: 'SP',
      bg: '#FAEEDA',
      color: '#633806',
      course: 'Thermodynamics & Heat Transfer',
      lastMessage: 'Live session scheduled for Friday 4 PM.',
      lastTime: '1d ago',
      unread: 1,
      messages: [
        { id: 'm1', sender: 'student', text: 'Ma\'am, will there be a live session this week?', time: 'Yesterday' },
        { id: 'm2', sender: 'teacher', text: 'Yes, I\'ve scheduled one for Friday at 4 PM. We\'ll cover the Carnot cycle problems.', time: 'Yesterday' },
        { id: 'm3', sender: 'teacher', text: 'Live session scheduled for Friday 4 PM.', time: 'Yesterday' }
      ]
    },
    {
      id: '4',
      teacherName: 'Prof. Rakesh Sharma',
      initials: 'RS',
      bg: '#EEEDFE',
      color: '#534AB7',
      course: 'Engineering Mathematics III',
      lastMessage: 'Notes uploaded to the portal.',
      lastTime: '3d ago',
      unread: 0,
      messages: [
        { id: 'm1', sender: 'student', text: 'Sir, I missed the last lecture. Is there a recording available?', time: '3 days ago' },
        { id: 'm2', sender: 'teacher', text: 'Yes, I\'ve uploaded it to the course portal. Check the Video Lectures section.', time: '3 days ago' },
        { id: 'm3', sender: 'teacher', text: 'Notes uploaded to the portal.', time: '3 days ago' }
      ]
    }
  ];

  selectedConversation: Conversation = this.conversations[0];

  // Filtered list — same logic as teacher version.
  get filteredConversations(): Conversation[] {
    let list = this.conversations;
    if (this.activeFilter === 'unread') {
      list = list.filter(c => c.unread > 0);
    }
    if (this.searchText) {
      const q = this.searchText.toLowerCase();
      list = list.filter(c =>
        c.teacherName.toLowerCase().includes(q) ||
        c.course.toLowerCase().includes(q) ||
        c.lastMessage.toLowerCase().includes(q)
      );
    }
    return list;
  }

  get totalUnread(): number {
    return this.conversations.reduce((sum, c) => sum + c.unread, 0);
  }

  selectConversation(conv: Conversation): void {
    this.selectedConversation = conv;
    conv.unread = 0;
  }

  // Sends a message — mirrors teacher version. In production this would call
  // MessagingService.sendMessage() and乐观-update the local list.
  sendMessage(): void {
    const text = this.newMessage.trim();
    if (!text) return;

    this.selectedConversation.messages.push({
      id: 'm' + Date.now(),
      sender: 'student',
      text,
      time: 'Just now'
    });
    this.selectedConversation.lastMessage = text;
    this.selectedConversation.lastTime = 'Just now';
    this.newMessage = '';
  }

  setFilter(filter: 'all' | 'unread' | 'courses'): void {
    this.activeFilter = filter;
  }
}
