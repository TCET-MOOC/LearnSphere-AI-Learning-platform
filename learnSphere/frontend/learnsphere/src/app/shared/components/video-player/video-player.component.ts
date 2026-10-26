import { 
  Component, 
  ElementRef, 
  EventEmitter, 
  Input, 
  OnChanges, 
  OnDestroy,
  OnInit,
  Output, 
  SimpleChanges, 
  ViewChild, 
  ChangeDetectorRef,
  HostListener,
  NgZone,
  ViewEncapsulation,
  AfterViewInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { YouTubeLoaderService } from '@core/services/youtube-loader.service';
import { 
  LucideAngularModule, 
  Play, 
  Pause, 
  RotateCcw, 
  RotateCw, 
  Volume2, 
  VolumeX, 
  Maximize, 
  Minimize, 
  PictureInPicture2,
  Sparkles,
  Subtitles,
  Settings,
  ChevronRight,
  Check,
  Clock,
  X,
  AlertTriangle,
  ExternalLink,
  RefreshCw
} from 'lucide-angular';

export interface VideoTimeUpdate {
  currentTime: number;
  duration: number;
  activeCueText?: string;
}

export interface SubtitleCue {
  start: number;
  end: number;
  text: string;
}

@Component({
  selector: 'app-video-player',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './video-player.component.html',
  styleUrls: ['./video-player.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class VideoPlayerComponent implements OnInit, AfterViewInit, OnChanges, OnDestroy {
  @Input() src: string | null | undefined;
  @Input() poster: string | null | undefined;
  @Input() videoTitle: string | null = null;
  @Input() lectureId: number | null = null;
  @Input() initialTime: number = 0;
  @Input() language: string = 'en';

  @Output() timeUpdate = new EventEmitter<VideoTimeUpdate>();
  @Output() ended = new EventEmitter<void>();

  @ViewChild('playerContainer') playerContainer!: ElementRef<HTMLDivElement>;
  @ViewChild('ytContainer') ytContainer?: ElementRef<HTMLDivElement>;
  @ViewChild('videoEl') videoEl?: ElementRef<HTMLVideoElement>;

  isPlaying = false;
  hasStartedPlaying = false;
  currentTime = 0;
  duration = 0;
  bufferedPercent = 0;
  volume = 1;
  isMuted = false;
  playbackSpeed = 1;
  showSettingsMenu = false;
  activeSettingsSubmenu: 'main' | 'speed' | 'quality' | 'subtitles' = 'main';
  isFullscreen = false;
  isControlsVisible = true;
  controlsTimeout: any = null;

  // Subtitles & Closed Captions State
  isCaptionsActive = false;
  currentSubtitleTrack = 'English (Auto)';
  currentSubtitleText = '';
  subtitleTracks = ['English (Auto)', 'Hinglish', 'Hindi', 'Marathi', 'Spanish', 'French', 'German', 'Off'];
  currentQuality = '1080p60 (HD)';
  availableQualities = ['1080p60 (HD)', '720p60', '480p', '360p (Auto)'];
  availableSpeeds = [0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0];

  // Auto-Resume Timestamp State
  showResumeBanner = false;
  resumedTime = 0;
  resumeBannerTimeout: any = null;

  // Hover Scrubber Preview
  hoverTime: number | null = null;
  hoverPosition = 0;

  // YouTube State & Error Management
  isYouTube = false;
  isYtReady = false;
  ytError: string | null = null;
  private isSeeking = false;
  private seekSuppressionTimer: any = null;
  private ytPlayer: any = null;
  private ytTimer: any = null;
  private currentYtVideoId: string | null = null;
  private captionsUnloaded = false;
  private captionRetriesScheduled = false;
  private captionRetryTimers: any[] = [];
  private loaderSub?: Subscription;
  private lastSavedTime = 0;
  private isViewInitialized = false;

  // Built-in Subtitle Cues
  private cues: SubtitleCue[] = [];

  constructor(
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone,
    private ytLoader: YouTubeLoaderService
  ) {}

  ngOnInit(): void {
    this.loadSavedVolume();
    this.generateMockCues();
  }

  ngAfterViewInit(): void {
    this.isViewInitialized = true;
    this.processSrc();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['src'] || changes['lectureId']) {
      this.hasStartedPlaying = false;
      this.showResumeBanner = false;
      this.ytError = null;
      this.generateMockCues();
      if (this.isViewInitialized) {
        this.processSrc();
      }
    }
    if (changes['language']) {
      this.generateMockCues(this.language);
    }
    if (changes['initialTime'] && !changes['initialTime'].firstChange && this.initialTime > 0) {
      this.seekTo(this.initialTime);
    }
  }

  ngOnDestroy(): void {
    this.clearTimers();
    if (this.loaderSub) {
      this.loaderSub.unsubscribe();
    }
    this.destroyYouTubePlayer();
  }

  private clearTimers(): void {
    if (this.controlsTimeout) clearTimeout(this.controlsTimeout);
    if (this.ytTimer) clearInterval(this.ytTimer);
    if (this.resumeBannerTimeout) clearTimeout(this.resumeBannerTimeout);
    if (this.seekSuppressionTimer) clearTimeout(this.seekSuppressionTimer);
    this.clearCaptionRetryTimers();
  }

  private loadSavedVolume(): void {
    try {
      const savedVol = localStorage.getItem('ls_player_volume');
      if (savedVol !== null) {
        this.volume = Math.max(0, Math.min(1, parseFloat(savedVol)));
        this.isMuted = this.volume === 0;
      }
    } catch (e) {}
  }

  private getStorageKey(): string {
    if (this.lectureId) return `ls_resume_lec_${this.lectureId}`;
    if (this.src) return `ls_resume_src_${encodeURIComponent(this.src)}`;
    return 'ls_resume_default';
  }

  private checkAutoResume(duration: number): void {
    if (this.initialTime > 0) {
      this.seekTo(this.initialTime);
      return;
    }

    try {
      const saved = localStorage.getItem(this.getStorageKey());
      if (saved) {
        const time = parseFloat(saved);
        if (time > 10 && duration > 20 && time < duration - 15) {
          this.resumedTime = Math.floor(time);
          this.showResumeBanner = true;
          this.seekTo(time);
          this.resumeBannerTimeout = setTimeout(() => {
            this.showResumeBanner = false;
            this.cdr.markForCheck();
          }, 8000);
          this.cdr.markForCheck();
        }
      }
    } catch (e) {}
  }

  restartFromBeginning(): void {
    this.showResumeBanner = false;
    this.seekTo(0);
    this.cdr.markForCheck();
  }

  dismissResumeBanner(): void {
    this.showResumeBanner = false;
    this.cdr.markForCheck();
  }

  private saveProgress(time: number): void {
    if (Math.abs(time - this.lastSavedTime) < 5) return;
    this.lastSavedTime = time;
    try {
      if (this.duration > 0 && time < this.duration - 5) {
        localStorage.setItem(this.getStorageKey(), time.toString());
      } else {
        localStorage.removeItem(this.getStorageKey());
      }
    } catch (e) {}
  }

  private processSrc(): void {
    if (!this.src) {
      this.isYouTube = false;
      this.currentYtVideoId = null;
      this.destroyYouTubePlayer();
      this.cdr.markForCheck();
      return;
    }

    const trimmed = this.src.trim();
    const ytId = this.extractYouTubeId(trimmed);

    if (ytId) {
      this.isYouTube = true;
      this.currentYtVideoId = ytId;
      this.cdr.detectChanges();
      this.initYouTubePlayer(ytId);
    } else {
      this.isYouTube = false;
      this.currentYtVideoId = null;
      this.destroyYouTubePlayer();
      if (this.videoEl?.nativeElement) {
        this.isPlaying = false;
        this.currentTime = 0;
      }
    }
    this.cdr.markForCheck();
  }

  get resolvedSrc(): string | null {
    if (!this.src || this.isYouTube) return null;
    if (/^https?:\/\//i.test(this.src)) return this.src;
    const origin = environment.apiUrl.replace(/\/api\/?$/, '');
    return this.src.startsWith('/') ? `${origin}${this.src}` : `${origin}/${this.src}`;
  }

  get effectivePoster(): string | null {
    if (this.poster) return this.poster;
    if (this.isYouTube && this.currentYtVideoId) {
      return `https://img.youtube.com/vi/${this.currentYtVideoId}/hqdefault.jpg`;
    }
    return null;
  }

  get youtubeExternalUrl(): string {
    return this.currentYtVideoId ? `https://www.youtube.com/watch?v=${this.currentYtVideoId}` : 'https://www.youtube.com';
  }

  private extractYouTubeId(url: string): string | null {
    if (!url) return null;
    const regExp = /(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/|youtube-nocookie\.com\/embed\/)([^"&?\/\s]{11})/i;
    const match = url.match(regExp);
    return match ? match[1] : null;
  }

  private initYouTubePlayer(videoId: string): void {
    this.isYtReady = false;
    this.ytError = null;
    this.captionsUnloaded = false;
    this.captionRetriesScheduled = false;
    this.clearCaptionRetryTimers();

    if (this.loaderSub) {
      this.loaderSub.unsubscribe();
    }

    this.loaderSub = this.ytLoader.loadApi().subscribe({
      next: () => {
        this.ngZone.runOutsideAngular(() => {
          // If a player instance is already active, reuse it via cueVideoById
          if (this.ytPlayer && typeof this.ytPlayer.cueVideoById === 'function') {
            try {
              this.ytPlayer.cueVideoById(videoId);
              this.ngZone.run(() => {
                this.isYtReady = true;
                this.currentTime = 0;
                this.bufferedPercent = 0;
                this.isPlaying = false;
                this.hasStartedPlaying = false;
                if (this.initialTime > 0) {
                  this.seekTo(this.initialTime);
                }
                this.cdr.markForCheck();
              });
              return;
            } catch (e) {
              this.destroyYouTubePlayer();
            }
          }

          const containerEl = this.ytContainer?.nativeElement;
          if (!containerEl) return;

          // Dynamically mount a fresh placeholder element inside ytContainer
          containerEl.innerHTML = '';
          const targetEl = document.createElement('div');
          targetEl.className = 'yt-frame';
          containerEl.appendChild(targetEl);

          try {
            this.ytPlayer = new window.YT.Player(targetEl, {
              width: '100%',
              height: '100%',
              videoId: videoId,
              playerVars: {
                autoplay: 0,
                controls: 1,
                disablekb: 0,
                enablejsapi: 1,
                fs: 1,
                modestbranding: 0,
                rel: 0,
                playsinline: 1,
                origin: window.location.origin
              },
              events: {
                onReady: (event: any) => {
                  this.ngZone.run(() => {
                    this.isYtReady = true;
                    this.duration = event.target.getDuration() || 0;
                    this.checkAutoResume(this.duration);

                    if (this.isMuted) {
                      this.ytPlayer.mute();
                    } else {
                      this.ytPlayer.setVolume(this.volume * 100);
                    }

                    if (this.playbackSpeed !== 1 && typeof this.ytPlayer.setPlaybackRate === 'function') {
                      this.ytPlayer.setPlaybackRate(this.playbackSpeed);
                    }

                    this.cdr.markForCheck();
                  });
                },
                onStateChange: (event: any) => {
                  this.ngZone.run(() => {
                    if (event.data === window.YT.PlayerState.PLAYING) {
                      this.isPlaying = true;
                      this.hasStartedPlaying = true;
                      this.isSeeking = false;
                      this.startYtProgressTracker();
                    } else if (event.data === window.YT.PlayerState.PAUSED) {
                      this.isPlaying = false;
                      this.stopYtProgressTracker();
                    } else if (event.data === window.YT.PlayerState.ENDED) {
                      this.isPlaying = false;
                      this.stopYtProgressTracker();
                      this.ended.emit();
                    } else if (event.data === window.YT.PlayerState.BUFFERING) {
                      this.isSeeking = false;
                    } else if (event.data === window.YT.PlayerState.CUED || event.data === -1) {
                      const dur = this.ytPlayer?.getDuration();
                      if (dur && dur > 0) {
                        this.duration = dur;
                      }
                    }
                    this.cdr.markForCheck();
                  });
                },

                onPlaybackRateChange: (event: any) => {
                  this.ngZone.run(() => {
                    if (event.data) {
                      this.playbackSpeed = event.data;
                      this.cdr.markForCheck();
                    }
                  });
                },
                onError: (event: any) => {
                  this.ngZone.run(() => {
                    this.handleYouTubeError(event.data);
                    this.cdr.markForCheck();
                  });
                }
              }
            });
          } catch (err) {
            this.ngZone.run(() => {
              this.ytError = 'Unable to mount video player. Please refresh the page.';
              this.cdr.markForCheck();
            });
          }
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.ytError = 'Failed to load YouTube streaming service. Please check your internet connection or ad-blocker.';
          this.cdr.markForCheck();
        });
      }
    });
  }

  private suppressNativeCaptions(): void {
    const p = this.ytPlayer;
    if (!p) return;
    try {
      // Explicitly deselect any active caption track. unloadModule alone
      // races against late-attaching auto-captions and often loses.
      if (typeof p.setOption === 'function') {
        p.setOption('captions', 'track', {});
      }
    } catch (e) {}
    try {
      if (typeof p.unloadModule === 'function') {
        p.unloadModule('captions');
      }
    } catch (e) {}
    this.captionsUnloaded = true;
  }

  private scheduleCaptionSuppressionRetries(): void {
    [400, 1200, 3000].forEach(delay => {
      this.captionRetryTimers.push(setTimeout(() => {
        if (this.ytPlayer && !this.ytError && this.isPlaying) {
          this.suppressNativeCaptions();
        }
      }, delay));
    });
  }

  private clearCaptionRetryTimers(): void {
    this.captionRetryTimers.forEach(t => clearTimeout(t));
    this.captionRetryTimers = [];
  }

  private handleYouTubeError(errorCode: number): void {
    switch (errorCode) {
      case 2:
        this.ytError = 'Invalid video ID or playback parameters.';
        break;
      case 5:
        this.ytError = 'HTML5 player playback error occurred.';
        break;
      case 100:
        this.ytError = 'This video has been removed or marked as private on YouTube.';
        break;
      case 101:
      case 150:
        this.ytError = 'Playback is restricted by the content owner on embedded players.';
        break;
      default:
        this.ytError = 'An error occurred while streaming this video.';
        break;
    }
  }

  retryPlayback(): void {
    this.ytError = null;
    this.destroyYouTubePlayer();
    if (this.currentYtVideoId) {
      this.initYouTubePlayer(this.currentYtVideoId);
    }
    this.cdr.markForCheck();
  }

  private destroyYouTubePlayer(): void {
    this.stopYtProgressTracker();
    this.isYtReady = false;
    if (this.ytPlayer) {
      try {
        if (typeof this.ytPlayer.destroy === 'function') {
          this.ytPlayer.destroy();
        }
      } catch (e) {}
      this.ytPlayer = null;
    }
    if (this.ytContainer?.nativeElement) {
      this.ytContainer.nativeElement.innerHTML = '';
    }
  }

  private startYtProgressTracker(): void {
    this.stopYtProgressTracker();
    this.ngZone.runOutsideAngular(() => {
      this.ytTimer = setInterval(() => {
        if (!this.ytPlayer || typeof this.ytPlayer.getCurrentTime !== 'function' || this.isSeeking) {
          return;
        }

        const time = this.ytPlayer.getCurrentTime() || 0;
        const dur = this.ytPlayer.getDuration() || this.duration;
        const frac = this.ytPlayer.getVideoLoadedFraction() || 0;

        this.ngZone.run(() => {
          this.currentTime = time;
          if (dur > 0 && dur !== this.duration) {
            this.duration = dur;
          }
          this.bufferedPercent = frac * 100;
          this.updateSubtitle(time);
          this.saveProgress(time);
          this.timeUpdate.emit({ currentTime: this.currentTime, duration: this.duration, activeCueText: this.getActiveCueText(time) });
          this.cdr.markForCheck();
        });
      }, 250);
    });
  }

  private stopYtProgressTracker(): void {
    if (this.ytTimer) {
      clearInterval(this.ytTimer);
      this.ytTimer = null;
    }
  }

  // --- Synchronized Subtitles Engine ---

  private generateMockCues(lang: string = this.language): void {
    const title = this.videoTitle || 'this lecture';

    switch (lang) {
      case 'hinglish':
        this.cues = [
          { start: 0, end: 14, text: `Welcome students! Aaj ke lecture mein hum ${title} ke core concepts aur algorithms detail mein explore karenge.` },
          { start: 14, end: 35, text: 'Machine Learning algorithms training dataset se directly functional mapping f: X -> Y seekhte hain.' },
          { start: 35, end: 65, text: 'Supervised learning ke andar hamara objective function empirical risk L(theta) ko minimize karna hota hai.' },
          { start: 65, end: 105, text: 'Gradient descent algorithm iterative updates ke through optimal parameters compute karta hai.' },
          { start: 105, end: 145, text: 'Attention Mechanism equation: Attention(Q,K,V) = softmax((Q * K^T) / sqrt(d_k)) * V token relevance allocate karta hai.' },
          { start: 145, end: 190, text: 'Query, Key aur Value matrices dynamic contextual relevance weights maintain karte hain.' },
          { start: 190, end: 240, text: 'Backpropagation step mein Jacobian matrices aur chain rule ke through loss gradients reverse propagate hote hain.' },
          { start: 240, end: 300, text: 'L2 regularization lambda * ||theta||^2 penalty term add karke overfitting prevent karta hai.' },
          { start: 300, end: 360, text: 'Model evaluation ke liye precision, recall, F1 score aur ROC-AUC metrics compute kiye jaate hain.' },
          { start: 360, end: 450, text: 'Next session mein hum is entire architecture ko PyTorch autograd tensors ke through step-by-step code karenge.' }
        ];
        break;

      case 'hi':
        this.cues = [
          { start: 0, end: 14, text: `${title} में आपका स्वागत है। आज हम इसके मूलभूत गणितीय सिद्धांतों और एल्गोरिदम का अध्ययन करेंगे।` },
          { start: 14, end: 35, text: 'मशीन लर्निंग प्रणालियाँ प्रशिक्षण डेटासेट से सीधे गणितीय प्रतिचित्रण f: X -> Y सीखती हैं।' },
          { start: 35, end: 65, text: 'पर्यवेक्षित शिक्षण में मुख्य उद्देश्य अनुभवजन्य जोखिम L(theta) को न्यूनतम करना है।' },
          { start: 65, end: 105, text: 'ग्रेडिएंट डिसेंट पुनरावृत्त रूप से मापदंडों को अद्यतन करता है।' },
          { start: 105, end: 145, text: 'अटेंशन मैकेनिज्म समीकरण: Attention(Q,K,V) = softmax((Q * K^T) / sqrt(d_k)) * V.' },
          { start: 145, end: 190, text: 'क्वेरी, की और वैल्यू मैट्रिसेस मॉडल को प्रासंगिक भार गतिशील रूप से आवंटित करने की अनुमति देते हैं।' },
          { start: 190, end: 240, text: 'बैकप्रॉपैगेशन चरण में जैकोबियन मैट्रिसेस और चेन नियम से ग्रेडिएंट्स को विपरीत दिशा में भेजा जाता है।' },
          { start: 240, end: 300, text: 'L2 नियमितीकरण (Regularization) ओवरफिटिंग को रोकने के लिए पेनल्टी पद जोड़ता है।' },
          { start: 300, end: 360, text: 'हम सटीकता (Precision), रिकॉल और F1 स्कोर मेट्रिक्स का उपयोग करके मॉडल का सत्यापन करते हैं।' },
          { start: 360, end: 450, text: 'अगले व्याख्यान में हम PyTorch का उपयोग करके इस संपूर्ण मॉडल को कोड करेंगे।' }
        ];
        break;

      case 'mr':
        this.cues = [
          { start: 0, end: 14, text: `${title} मध्ये आपले स्वागत आहे. आज आपण मूलभूत गणितीय संकल्पना आणि अल्गोरिदम शिकणार आहोत.` },
          { start: 14, end: 35, text: 'मशीन लर्निंग अल्गोरिदम थेट प्रशिक्षण डेटामधून मॅपिंग f: X -> Y शिकतात.' },
          { start: 35, end: 65, text: 'सुपरव्हाइज्ड लर्निंगमध्ये आपले उद्दिष्ट एरर L(theta) कमी करणे हे असते.' },
          { start: 65, end: 105, text: 'ग्रेडियंट डिझेंट पॅरामीटर्स अपडेट करतो: theta = theta - alpha * grad_theta(L(theta)).' },
          { start: 105, end: 145, text: 'अटेंशन मॅकेनिझम समीकरण: Attention(Q,K,V) = softmax((Q * K^T) / sqrt(d_k)) * V.' },
          { start: 145, end: 190, text: 'क्वेरी, की आणि व्हॅल्यू मॅट्रिक्स संदर्भ भार गतिमानपणे मोजतात.' },
          { start: 190, end: 240, text: 'बॅकप्रॉपॅगेशन प्रक्रियेत साखळी नियमाचा वापर करून ग्रेडियंट्स मागे पाठवले जातात.' },
          { start: 240, end: 300, text: 'रेग्युलरायझेशन ओव्हरफिटिंग टाळण्यासाठी महत्त्वपूर्ण भूमिका बजावते.' },
          { start: 300, end: 360, text: 'मॉडेल अचूकता मोजण्यासाठी प्रिसिजन आणि F1 स्कोअरचा वापर केला जातो.' },
          { start: 360, end: 450, text: 'पुढील सत्रात आपण PyTorch वापरून हे सर्व कोड करणार आहोत.' }
        ];
        break;

      case 'es':
        this.cues = [
          { start: 0, end: 14, text: `Bienvenidos a ${title}. Hoy cubriremos la arquitectura matemática fundamental.` },
          { start: 14, end: 35, text: 'Los sistemas de aprendizaje automático aprenden funciones de mapeo f: X -> Y directamente de los datos.' },
          { start: 35, end: 65, text: 'En el aprendizaje supervisado, minimizamos el riesgo empírico L(theta).' },
          { start: 65, end: 105, text: 'El descenso de gradiente actualiza los parámetros en forma iterativa.' },
          { start: 105, end: 145, text: 'Mecanismo de atención: Attention(Q,K,V) = softmax((Q * K^T) / sqrt(d_k)) * V.' },
          { start: 145, end: 190, text: 'Las matrices Query, Key y Value permiten calcular pesos contextuales dinámicos.' },
          { start: 190, end: 240, text: 'En la retropropagación propagamos gradientes usando la regla de la cadena.' },
          { start: 240, end: 300, text: 'La regularización L2 añade un término de penalización para evitar el sobreajuste.' },
          { start: 300, end: 360, text: 'Validamos el modelo con precisión, recall y puntuaciones F1.' },
          { start: 360, end: 450, text: 'En la próxima sesión implementaremos esta arquitectura con PyTorch.' }
        ];
        break;

      case 'en':
      default:
        this.cues = [
          { start: 0, end: 14, text: `Welcome to ${title}. Today we'll cover the core mathematical architecture and algorithms.` },
          { start: 14, end: 35, text: 'Machine Learning systems fundamentally learn functional mappings f: X -> Y directly from empirical training datasets.' },
          { start: 35, end: 65, text: 'In supervised learning, our objective is to minimize empirical risk: L(theta) = 1/N * sum(loss(f(x_i; theta), y_i)).' },
          { start: 65, end: 105, text: 'Gradient descent iteratively updates parameter vectors via theta := theta - alpha * grad_theta(L(theta)).' },
          { start: 105, end: 145, text: 'Let us consider the Attention Mechanism equation: Attention(Q,K,V) = softmax((Q * K^T) / sqrt(d_k)) * V.' },
          { start: 145, end: 190, text: 'Query, Key, and Value matrices allow the network to dynamically assign contextual relevance weights across token sequences.' },
          { start: 190, end: 240, text: 'To implement backpropagation, we compute Jacobian matrices and propagate loss gradients backwards via the chain rule.' },
          { start: 240, end: 300, text: 'Regularization techniques like L2 weight decay add lambda * ||theta||^2 penalty terms to avoid high-variance overfitting.' },
          { start: 300, end: 360, text: 'Next, we validate our model on held-out test splits using precision, recall, F1 score, and ROC-AUC metrics.' },
          { start: 360, end: 450, text: 'In the next lecture, we will implement this architecture from scratch using PyTorch and custom autograd functions.' }
        ];
        break;
    }
  }

  private getActiveCueText(time: number): string {
    const active = this.cues.find(c => time >= c.start && time < c.end);
    return active ? active.text : '';
  }

  private updateSubtitle(time: number): void {
    this.currentSubtitleText = (this.isCaptionsActive && this.currentSubtitleTrack !== 'Off')
      ? this.getActiveCueText(time)
      : '';
  }

  toggleCaptions(): void {
    this.isCaptionsActive = !this.isCaptionsActive;
    if (this.isCaptionsActive && this.currentSubtitleTrack === 'Off') {
      this.currentSubtitleTrack = 'English (Auto)';
    }
    this.updateSubtitle(this.currentTime);
    this.cdr.markForCheck();
  }

  // --- Settings Popover & Submenus ---

  toggleSettings(event: MouseEvent): void {
    event.stopPropagation();
    this.showSettingsMenu = !this.showSettingsMenu;
    this.activeSettingsSubmenu = 'main';
  }

  setSettingsSubmenu(menu: 'main' | 'speed' | 'quality' | 'subtitles', event?: MouseEvent): void {
    if (event) event.stopPropagation();
    this.activeSettingsSubmenu = menu;
  }

  setQuality(q: string, event?: MouseEvent): void {
    if (event) event.stopPropagation();
    this.currentQuality = q;
    this.activeSettingsSubmenu = 'main';
    this.showSettingsMenu = false;
  }

  setSubtitleTrack(track: string, event?: MouseEvent): void {
    if (event) event.stopPropagation();
    this.currentSubtitleTrack = track;
    this.isCaptionsActive = track !== 'Off';
    this.updateSubtitle(this.currentTime);
    this.activeSettingsSubmenu = 'main';
    this.showSettingsMenu = false;
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    if (this.showSettingsMenu) {
      this.showSettingsMenu = false;
      this.activeSettingsSubmenu = 'main';
      this.cdr.markForCheck();
    }
  }

  // --- Unified Player Controls ---

  togglePlay(): void {
    if (this.ytError) return;

    if (this.isYouTube) {
      if (!this.isYtReady || !this.ytPlayer) return;

      try {
        const state = typeof this.ytPlayer.getPlayerState === 'function' ? this.ytPlayer.getPlayerState() : -1;
        if (state === window.YT?.PlayerState?.PLAYING) {
          this.ytPlayer.pauseVideo();
        } else {
          this.ytPlayer.playVideo();
          this.startControlsHideTimer();
        }
      } catch (err) {}
    } else {
      const video = this.videoEl?.nativeElement;
      if (!video) return;
      if (video.paused) {
        video.play().catch(() => {});
        this.isPlaying = true;
      } else {
        video.pause();
        this.isPlaying = false;
      }
      this.cdr.markForCheck();
    }
  }

  play(): void {
    if (this.isYouTube && this.isYtReady && this.ytPlayer) {
      try {
        this.ytPlayer.playVideo();
      } catch (e) {}
    } else if (this.videoEl?.nativeElement) {
      this.videoEl.nativeElement.play().catch(() => {});
    }
  }

  pause(): void {
    if (this.isYouTube && this.isYtReady && this.ytPlayer) {
      try {
        this.ytPlayer.pauseVideo();
      } catch (e) {}
    } else if (this.videoEl?.nativeElement) {
      this.videoEl.nativeElement.pause();
    }
  }

  onPlay(): void {
    this.isPlaying = true;
    this.hasStartedPlaying = true;
    this.startControlsHideTimer();
    this.cdr.markForCheck();
  }

  onPause(): void {
    this.isPlaying = false;
    this.showControls();
    this.cdr.markForCheck();
  }

  onLoadedMetadata(): void {
    const video = this.videoEl?.nativeElement;
    if (video) {
      this.duration = video.duration || 0;
      video.volume = this.volume;
      video.muted = this.isMuted;
      this.checkAutoResume(this.duration);
      this.cdr.markForCheck();
    }
  }

  onTimeUpdate(): void {
    const video = this.videoEl?.nativeElement;
    if (!video || this.isSeeking) return;
    this.currentTime = video.currentTime;
    this.duration = video.duration || this.duration;

    if (video.buffered.length > 0) {
      const bufferedEnd = video.buffered.end(video.buffered.length - 1);
      this.bufferedPercent = (bufferedEnd / this.duration) * 100;
    }

    this.updateSubtitle(this.currentTime);
    this.saveProgress(this.currentTime);
    this.timeUpdate.emit({ currentTime: this.currentTime, duration: this.duration, activeCueText: this.getActiveCueText(this.currentTime) });
    this.cdr.markForCheck();
  }

  onEnded(): void {
    this.isPlaying = false;
    this.showControls();
    this.ended.emit();
    this.cdr.markForCheck();
  }

  onSeek(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input) return;
    const value = Number(input.value);
    this.seekTo(value);
  }

  seekRelative(seconds: number): void {
    const target = Math.max(0, Math.min(this.currentTime + seconds, this.duration));
    this.seekTo(target);
  }

  seekTo(seconds: number): void {
    const target = Math.max(0, Math.min(seconds, this.duration || seconds));
    this.currentTime = target;
    this.isSeeking = true;

    if (this.seekSuppressionTimer) clearTimeout(this.seekSuppressionTimer);
    this.seekSuppressionTimer = setTimeout(() => {
      this.isSeeking = false;
    }, 500);

    if (this.isYouTube && this.isYtReady && this.ytPlayer) {
      try {
        this.ytPlayer.seekTo(target, true);
      } catch (e) {}
    } else if (this.videoEl?.nativeElement) {
      this.videoEl.nativeElement.currentTime = target;
    }

    this.updateSubtitle(target);
    this.timeUpdate.emit({ currentTime: target, duration: this.duration, activeCueText: this.getActiveCueText(target) });
    this.showControls();
    this.cdr.markForCheck();
  }

  setVolume(event: Event): void {
    const input = event.target as HTMLInputElement;
    const val = parseFloat(input.value);
    this.volume = Math.max(0, Math.min(1, val));
    this.isMuted = this.volume === 0;

    try {
      localStorage.setItem('ls_player_volume', this.volume.toString());
    } catch (e) {}

    if (this.isYouTube && this.isYtReady && this.ytPlayer) {
      try {
        if (this.isMuted) {
          this.ytPlayer.mute();
        } else {
          this.ytPlayer.unMute();
          this.ytPlayer.setVolume(this.volume * 100);
        }
      } catch (e) {}
    } else if (this.videoEl?.nativeElement) {
      this.videoEl.nativeElement.volume = this.volume;
      this.videoEl.nativeElement.muted = this.isMuted;
    }
  }

  toggleMute(): void {
    this.isMuted = !this.isMuted;
    if (this.isYouTube && this.isYtReady && this.ytPlayer) {
      try {
        if (this.isMuted) {
          this.ytPlayer.mute();
        } else {
          this.ytPlayer.unMute();
          this.ytPlayer.setVolume((this.volume || 0.5) * 100);
        }
      } catch (e) {}
    } else if (this.videoEl?.nativeElement) {
      this.videoEl.nativeElement.muted = this.isMuted;
    }
  }

  setPlaybackSpeed(speed: number, event?: MouseEvent): void {
    if (event) event.stopPropagation();
    this.playbackSpeed = speed;
    if (this.isYouTube && this.isYtReady && this.ytPlayer && typeof this.ytPlayer.setPlaybackRate === 'function') {
      try {
        this.ytPlayer.setPlaybackRate(speed);
      } catch (e) {}
    } else if (this.videoEl?.nativeElement) {
      this.videoEl.nativeElement.playbackRate = speed;
    }
    this.activeSettingsSubmenu = 'main';
    this.showSettingsMenu = false;
  }

  // --- Picture-in-Picture (PiP) ---
  async togglePiP(): Promise<void> {
    try {
      if ('documentPictureInPicture' in window && (window as any).documentPictureInPicture) {
        const docPiP = (window as any).documentPictureInPicture;
        if (docPiP.window) {
          docPiP.window.close();
          return;
        }

        const pipWindow = await docPiP.requestWindow({
          width: 640,
          height: 360
        });

        [...document.styleSheets].forEach((styleSheet) => {
          try {
            const cssRules = [...styleSheet.cssRules].map((rule) => rule.cssText).join('');
            const style = document.createElement('style');
            style.textContent = cssRules;
            pipWindow.document.head.appendChild(style);
          } catch (e) {
            const link = document.createElement('link');
            if (styleSheet.href) {
              link.rel = 'stylesheet';
              link.type = styleSheet.type;
              link.href = styleSheet.href;
              pipWindow.document.head.appendChild(link);
            }
          }
        });

        const container = this.playerContainer.nativeElement;
        pipWindow.document.body.appendChild(container);
        pipWindow.document.body.style.margin = '0';
        pipWindow.document.body.style.background = '#09090b';

        pipWindow.addEventListener('pagehide', () => {
          const hostEl = document.querySelector('app-video-player');
          if (hostEl) {
            hostEl.appendChild(container);
          }
          this.cdr.markForCheck();
        });
        return;
      }

      const video = this.videoEl?.nativeElement;
      if (video) {
        if (document.pictureInPictureElement) {
          await document.exitPictureInPicture();
        } else if (document.pictureInPictureEnabled) {
          await video.requestPictureInPicture();
        }
      }
    } catch (err) {
      console.warn('PiP error', err);
    }
  }

  toggleFullscreen(): void {
    const container = this.playerContainer?.nativeElement;
    if (!container) return;

    if (!document.fullscreenElement) {
      container.requestFullscreen().catch(() => {});
      this.isFullscreen = true;
    } else {
      document.exitFullscreen().catch(() => {});
      this.isFullscreen = false;
    }
    this.cdr.markForCheck();
  }

  // --- Scrubber Hover Preview Tooltip ---
  onScrubberMouseMove(event: MouseEvent): void {
    if (!this.duration) return;
    const target = event.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();
    const percent = Math.max(0, Math.min((event.clientX - rect.left) / rect.width, 1));
    this.hoverTime = percent * this.duration;
    this.hoverPosition = percent * 100;
  }

  onScrubberMouseLeave(): void {
    this.hoverTime = null;
  }

  // --- Dynamic Controls Auto-Hide ---
  onMouseMove(): void {
    this.showControls();
    if (this.isPlaying) {
      this.startControlsHideTimer();
    }
  }

  private showControls(): void {
    this.isControlsVisible = true;
    if (this.controlsTimeout) {
      clearTimeout(this.controlsTimeout);
    }
    this.cdr.markForCheck();
  }

  private startControlsHideTimer(): void {
    if (this.controlsTimeout) {
      clearTimeout(this.controlsTimeout);
    }
    this.controlsTimeout = setTimeout(() => {
      if (this.isPlaying && !this.showSettingsMenu) {
        this.isControlsVisible = false;
        this.cdr.markForCheck();
      }
    }, 2500);
  }

  // --- Global Keyboard Shortcuts ---
  @HostListener('window:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent): void {
    const tag = (event.target as HTMLElement)?.tagName?.toLowerCase();
    if (tag === 'input' || tag === 'textarea') return;

    switch (event.code) {
      case 'Space':
      case 'KeyK':
        event.preventDefault();
        this.togglePlay();
        break;
      case 'ArrowLeft':
      case 'KeyJ':
        event.preventDefault();
        this.seekRelative(-10);
        break;
      case 'ArrowRight':
      case 'KeyL':
        event.preventDefault();
        this.seekRelative(10);
        break;
      case 'KeyM':
        event.preventDefault();
        this.toggleMute();
        break;
      case 'KeyF':
        event.preventDefault();
        this.toggleFullscreen();
        break;
      case 'KeyC':
        event.preventDefault();
        this.toggleCaptions();
        break;
    }
  }

  formatTime(seconds: number): string {
    if (!isFinite(seconds) || seconds < 0) return '00:00';
    const hrs = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = Math.floor(seconds % 60);

    if (hrs > 0) {
      return `${hrs}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }
}
