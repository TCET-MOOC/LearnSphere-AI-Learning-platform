import { Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { StorageService } from './storage.service';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements OnDestroy {
  private stompClient: Client | null = null;
  private isConnectedSubject = new BehaviorSubject<boolean>(false);
  public isConnected$ = this.isConnectedSubject.asObservable();

  private subscriptions: Map<string, Subject<any>> = new Map();

  constructor(
    private authService: AuthService,
    private storageService: StorageService
  ) {
    // Automatically connect if user is already logged in
    this.authService.currentUser$.subscribe(user => {
      if (user) {
        this.connect();
      } else {
        this.disconnect();
      }
    });
  }

  public connect(): void {
    if (this.stompClient && this.stompClient.active) {
      return;
    }

    const token = this.storageService.getToken() || '';

    // Connect using standard native WebSockets supported by all modern browsers
    this.stompClient = new Client({
      brokerURL: 'ws://localhost:8080/ws-native',
      connectHeaders: {
        Authorization: token ? `Bearer ${token}` : ''
      },
      debug: (msg: string) => {
        // console.log('[STOMP]:', msg);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000
    });

    this.stompClient.onConnect = (frame) => {
      this.isConnectedSubject.next(true);
      // Re-subscribe to all active topics
      this.subscriptions.forEach((subject, topic) => {
        this.listenToTopic(topic, subject);
      });
    };

    this.stompClient.onDisconnect = () => {
      this.isConnectedSubject.next(false);
    };

    this.stompClient.onStompError = (frame) => {
      console.warn('[STOMP Warning]:', frame.headers['message'], frame.body);
      this.isConnectedSubject.next(false);
    };

    this.stompClient.activate();
  }

  public disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
      this.isConnectedSubject.next(false);
    }
  }

  public subscribeToTopic<T>(topic: string): Observable<T> {
    if (!this.subscriptions.has(topic)) {
      const subject = new Subject<T>();
      this.subscriptions.set(topic, subject);

      if (this.isConnectedSubject.value && this.stompClient) {
        this.listenToTopic(topic, subject);
      }
    }
    return this.subscriptions.get(topic)!.asObservable();
  }

  private listenToTopic(topic: string, subject: Subject<any>): void {
    if (!this.stompClient || !this.stompClient.connected) {
      return;
    }

    this.stompClient.subscribe(topic, (message: IMessage) => {
      try {
        const parsed = JSON.parse(message.body);
        subject.next(parsed);
      } catch (e) {
        subject.next(message.body);
      }
    });
  }

  public publish(destination: string, body: any): void {
    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.publish({
        destination,
        body: typeof body === 'string' ? body : JSON.stringify(body)
      });
    } else {
      console.warn('Cannot publish message, STOMP client is not connected.');
    }
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
