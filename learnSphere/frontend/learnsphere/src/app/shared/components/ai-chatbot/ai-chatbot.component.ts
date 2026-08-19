import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '@core/services/api.service';
import { LucideAngularModule } from 'lucide-angular';

interface ChatMessage {
  id: string;
  sender: 'user' | 'bot';
  text: string;
  time: string;
}

@Component({
  selector: 'app-ai-chatbot',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './ai-chatbot.component.html',
  styleUrls: ['./ai-chatbot.component.scss']
})
export class AiChatbotComponent implements OnInit {
  isOpen = false;
  inputMessage = '';
  loading = false;

  messages: ChatMessage[] = [
    {
      id: '1',
      sender: 'bot',
      text: 'Hi there! 👋 I\'m Spherie, your LearnSphere AI assistant. Ask me anything about your courses, certificates, attendance credits, or leaderboard points!',
      time: 'Just now'
    }
  ];

  suggestions: string[] = [
    'How does attendance recovery work?',
    'How do I earn certificates?',
    'How to earn points for leaderboard?',
    'Where do I join live lectures?'
  ];

  constructor(private apiService: ApiService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {}

  toggleChat(): void {
    this.isOpen = !this.isOpen;
  }

  closeChat(): void {
    this.isOpen = false;
  }

  sendMessage(textToSend?: string): void {
    const text = (textToSend || this.inputMessage).trim();
    if (!text || this.loading) return;

    this.messages.push({
      id: Date.now().toString(),
      sender: 'user',
      text,
      time: 'Just now'
    });

    this.inputMessage = '';
    this.loading = true;
    this.cdr.markForCheck();

    this.apiService.post<{ reply: string; suggestions?: string[] }>('/chatbot/ask', { message: text }).subscribe({
      next: (res) => {
        this.messages.push({
          id: (Date.now() + 1).toString(),
          sender: 'bot',
          text: res.reply,
          time: 'Just now'
        });
        if (res.suggestions && res.suggestions.length > 0) {
          this.suggestions = res.suggestions;
        }
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.messages.push({
          id: (Date.now() + 1).toString(),
          sender: 'bot',
          text: 'I can help answer your questions about courses, tests, attendance credits, or platform rules. What would you like to know?',
          time: 'Just now'
        });
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  useSuggestion(suggestion: string): void {
    this.sendMessage(suggestion);
  }
}
