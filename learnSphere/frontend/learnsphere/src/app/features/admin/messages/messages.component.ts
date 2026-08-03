import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

// Admin messages page — mirrors teacher/student messages component structure.
// Admin can message both teachers and students (platform-level oversight).
// The admin dashboard uses the same light theme as teacher/student — the dark
// theme is only applied to the admin sidebar, not the content area.

interface Message {
  id: string;
  sender: 'admin' | 'other';
  text: string;
  time: string;
}

interface Conversation {
  id: string;
  personName: string;
  role: 'teacher' | 'student';
  initials: string;
  bg: string;
  color: string;
  context: string;
  lastMessage: string;
  lastTime: string;
  unread: number;
  messages: Message[];
}

@Component({
  selector: 'app-admin-messages',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './messages.component.html',
  styleUrls: ['./messages.component.scss']
})
export class MessagesComponent {
  searchText = '';
  activeFilter: 'all' | 'unread' = 'all';
  newMessage = '';

  // Mock conversations — admin can message both teachers and students.
  // In production these come from MessagingService.getConversations().
  conversations: Conversation[] = [
    {
      id: '1',
      personName: 'Prof. Rakesh Sharma',
      role: 'teacher',
      initials: 'RS',
      bg: '#EEEDFE',
      color: '#534AB7',
      context: 'Course approval query',
      lastMessage: 'When will the Statistics course be approved?',
      lastTime: '15m ago',
      unread: 2,
      messages: [
        { id: 'm1', sender: 'other', text: 'Hi, I submitted my Statistics course for review 3 days ago. Any update?', time: '2:30 PM' },
        { id: 'm2', sender: 'admin', text: 'Hi Prof. Sharma, we\'re reviewing it now. Should be approved by end of day.', time: '2:35 PM' },
        { id: 'm3', sender: 'other', text: 'When will the Statistics course be approved?', time: '2:40 PM' }
      ]
    },
    {
      id: '2',
      personName: 'Priya Kulkarni',
      role: 'student',
      initials: 'PK',
      bg: '#EAF3DE',
      color: '#27500A',
      context: 'Payment issue',
      lastMessage: 'The payment didn\'t go through for the ML course.',
      lastTime: '1h ago',
      unread: 1,
      messages: [
        { id: 'm1', sender: 'other', text: 'Hello, I tried purchasing the ML course but the payment failed.', time: '11:00 AM' },
        { id: 'm2', sender: 'admin', text: 'I\'m sorry to hear that. Can you share the transaction ID?', time: '11:05 AM' },
        { id: 'm3', sender: 'other', text: 'The payment didn\'t go through for the ML course.', time: '11:10 AM' }
      ]
    },
    {
      id: '3',
      personName: 'Dr. Priya Nair',
      role: 'teacher',
      initials: 'PN',
      bg: '#FAEEDA',
      color: '#633806',
      context: 'Royalty payout',
      lastMessage: 'Thank you for processing the payout.',
      lastTime: '2d ago',
      unread: 0,
      messages: [
        { id: 'm1', sender: 'other', text: 'Hi, my royalty for May hasn\'t been credited yet.', time: '2 days ago' },
        { id: 'm2', sender: 'admin', text: 'We\'ve processed it now. You should see it in your account within 24 hours.', time: '2 days ago' },
        { id: 'm3', sender: 'other', text: 'Thank you for processing the payout.', time: '2 days ago' }
      ]
    },
    {
      id: '4',
      personName: 'Arjun Mishra',
      role: 'student',
      initials: 'AM',
      bg: '#FCEBEB',
      color: '#791F1F',
      context: 'Certificate issue',
      lastMessage: 'Got it, I\'ll check the portal.',
      lastTime: '3d ago',
      unread: 0,
      messages: [
        { id: 'm1', sender: 'other', text: 'My certificate for DSA course isn\'t showing up.', time: '3 days ago' },
        { id: 'm2', sender: 'admin', text: 'It was issued yesterday. Please refresh the Certificates page — it should appear now.', time: '3 days ago' },
        { id: 'm3', sender: 'other', text: 'Got it, I\'ll check the portal.', time: '3 days ago' }
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
        c.personName.toLowerCase().includes(q) ||
        c.context.toLowerCase().includes(q) ||
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
      sender: 'admin',
      text,
      time: 'Just now'
    });
    this.selectedConversation.lastMessage = text;
    this.selectedConversation.lastTime = 'Just now';
    this.newMessage = '';
  }

  setFilter(filter: 'all' | 'unread'): void {
    this.activeFilter = filter;
  }
}
