import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface Message {
  id: string;
  sender: 'teacher' | 'student';
  text: string;
  time: string;
}

interface Conversation {
  id: string;
  studentName: string;
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
  selector: 'app-teacher-messages',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './messages.component.html',
  styleUrls: ['./messages.component.scss']
})
export class MessagesComponent {
  searchText = '';
  activeFilter: 'all' | 'unread' | 'courses' = 'all';
  newMessage = '';

  conversations: Conversation[] = [
    {
      id: '1',
      studentName: 'Raj Shah',
      initials: 'RS',
      bg: '#EEEDFE',
      color: '#534AB7',
      course: 'Engineering Mathematics III',
      lastMessage: 'Can you clarify Laplace lec 8?',
      lastTime: '10m ago',
      unread: 2,
      messages: [
        { id: 'm1', sender: 'student', text: 'Hello Professor, I had a doubt from today\'s lecture.', time: '2:30 PM' },
        { id: 'm2', sender: 'teacher', text: 'Sure Raj, which topic are you referring to?', time: '2:35 PM' },
        { id: 'm3', sender: 'student', text: 'The Laplace transform section in lecture 8. I couldn\'t follow the inverse transform step.', time: '2:38 PM' },
        { id: 'm4', sender: 'teacher', text: 'Good question. The key is to use partial fraction decomposition before applying the inverse. I\'ll cover it again in tomorrow\'s session.', time: '2:42 PM' },
        { id: 'm5', sender: 'student', text: 'Can you clarify Laplace lec 8?', time: '2:45 PM' }
      ]
    },
    {
      id: '2',
      studentName: 'Priya Kulkarni',
      initials: 'PK',
      bg: '#EAF3DE',
      color: '#27500A',
      course: 'Engineering Mathematics III',
      lastMessage: 'Thank you for the feedback!',
      lastTime: '1h ago',
      unread: 0,
      messages: [
        { id: 'm1', sender: 'student', text: 'Professor, I submitted my assignment yesterday. Could you review it when you get a chance?', time: '11:00 AM' },
        { id: 'm2', sender: 'teacher', text: 'I\'ve reviewed it, Priya. Very well written. A few minor corrections in Q3 and Q7 — I\'ve left comments.', time: '11:30 AM' },
        { id: 'm3', sender: 'student', text: 'Thank you for the feedback!', time: '11:35 AM' }
      ]
    },
    {
      id: '3',
      studentName: 'Arjun Mishra',
      initials: 'AM',
      bg: '#FAEEDA',
      color: '#633806',
      course: 'Discrete Mathematics',
      lastMessage: 'When is the next live session?',
      lastTime: '1d ago',
      unread: 1,
      messages: [
        { id: 'm1', sender: 'student', text: 'Hi Professor, will there be a live session this week?', time: 'Yesterday' },
        { id: 'm2', sender: 'student', text: 'When is the next live session?', time: 'Yesterday' }
      ]
    },
    {
      id: '4',
      studentName: 'Nisha Joshi',
      initials: 'NJ',
      bg: '#FCEBEB',
      color: '#791F1F',
      course: 'Engineering Mathematics III',
      lastMessage: 'Got it, thanks!',
      lastTime: '2d ago',
      unread: 0,
      messages: [
        { id: 'm1', sender: 'student', text: 'Professor, I missed the last lecture. Is there a recording available?', time: '2 days ago' },
        { id: 'm2', sender: 'teacher', text: 'Yes, I\'ve uploaded it to the course portal. Check the Video Lectures section.', time: '2 days ago' },
        { id: 'm3', sender: 'student', text: 'Got it, thanks!', time: '2 days ago' }
      ]
    },
    {
      id: '5',
      studentName: 'Vikram Desai',
      initials: 'VD',
      bg: '#E6F1FB',
      color: '#0A5279',
      course: 'Discrete Mathematics',
      lastMessage: 'Sir, please share the notes for chapter 4.',
      lastTime: '3d ago',
      unread: 0,
      messages: [
        { id: 'm1', sender: 'student', text: 'Sir, please share the notes for chapter 4.', time: '3 days ago' },
        { id: 'm2', sender: 'teacher', text: 'I\'ve uploaded them to the Notes section. Let me know if you have trouble accessing them.', time: '3 days ago' }
      ]
    },
    {
      id: '6',
      studentName: 'Sneha Patil',
      initials: 'SP',
      bg: '#EEEDFE',
      color: '#534AB7',
      course: 'Statistics for Engineers',
      lastMessage: 'The project topic looks great!',
      lastTime: '4d ago',
      unread: 0,
      messages: [
        { id: 'm1', sender: 'student', text: 'Professor, I wanted to discuss my project topic idea.', time: '4 days ago' },
        { id: 'm2', sender: 'teacher', text: 'Go ahead, what do you have in mind?', time: '4 days ago' },
        { id: 'm3', sender: 'student', text: 'I was thinking of analyzing rainfall patterns using regression analysis.', time: '4 days ago' },
        { id: 'm4', sender: 'teacher', text: 'The project topic looks great! That\'s a solid application of the concepts we covered.', time: '4 days ago' }
      ]
    }
  ];

  selectedConversation: Conversation = this.conversations[0];

  get filteredConversations(): Conversation[] {
    let list = this.conversations;
    if (this.activeFilter === 'unread') {
      list = list.filter(c => c.unread > 0);
    }
    if (this.searchText) {
      const q = this.searchText.toLowerCase();
      list = list.filter(c =>
        c.studentName.toLowerCase().includes(q) ||
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

  sendMessage(): void {
    const text = this.newMessage.trim();
    if (!text) return;

    this.selectedConversation.messages.push({
      id: 'm' + Date.now(),
      sender: 'teacher',
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
