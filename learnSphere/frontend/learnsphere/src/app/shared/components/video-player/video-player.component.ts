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
  ViewEncapsulation
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { environment } from '../../../../environments/environment';
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
  X
} from 'lucide-angular';

declare global {
  interface Window {
    YT: any;
    onYouTubeIframeAPIReady: () => void;
    documentPictureInPicture?: {
      requestWindow: (options?: { width?: number; height?: number }) => Promise<Window>;
      window: Window | null;
    };
  }
}

export interface VideoTimeUpdate {
  currentTime: number;
  duration: number;
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
export class VideoPlayerComponent implements OnInit, OnChanges, OnDestroy {
  @Input() src: string | null | undefined;
  @Input() poster: string | null | undefined;
  @Input() videoTitle: string | null = null;
  @Input() lectureId: number | null = null;
  @Input() initialTime: number = 0;
  @Input() language: string = 'en';

  @Output() timeUpdate = new EventEmitter<VideoTimeUpdate>();
  @Output() ended = new EventEmitter<void>();

  @ViewChild('playerContainer') playerContainer!: ElementRef<HTMLDivElement>;
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

  isYouTube = false;
  isYtReady = false;
  private isPendingPlay = false;
  private ytPlayer: any = null;
  private ytTimer: any = null;
  private currentYtVideoId: string | null = null;
  private lastSavedTime = 0;

  // Built-in Subtitle Cues
  private cues: SubtitleCue[] = [];

  constructor(
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    this.loadSavedVolume();
    this.ensureYouTubeApiLoaded();
    this.generateMockCues();
    this.processSrc();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['src'] || changes['lectureId']) {
      this.hasStartedPlaying = false;
      this.showResumeBanner = false;
      this.generateMockCues();
      this.processSrc();
    }
    if (changes['language']) {
      this.generateMockCues(this.language);
    }
  }

  ngOnDestroy(): void {
    this.clearTimers();
    if (this.ytPlayer && typeof this.ytPlayer.destroy === 'function') {
      try {
        this.ytPlayer.destroy();
      } catch (e) {}
    }
  }

  private clearTimers(): void {
    if (this.controlsTimeout) clearTimeout(this.controlsTimeout);
    if (this.ytTimer) clearInterval(this.ytTimer);
    if (this.resumeBannerTimeout) clearTimeout(this.resumeBannerTimeout);
  }

  private loadSavedVolume(): void {
    try {
      const savedVol = localStorage.getItem('ls_player_volume');
      if (savedVol !== null) {
        this.volume = parseFloat(savedVol);
        this.isMuted = this.volume === 0;
      }
    } catch (e) {}
  }

  private ensureYouTubeApiLoaded(): void {
    if (typeof window !== 'undefined' && !window.YT) {
      const tag = document.createElement('script');
      tag.src = 'https://www.youtube.com/iframe_api';
      document.body.appendChild(tag);
    }
  }

  private getStorageKey(): string {
    if (this.lectureId) return `ls_resume_lec_${this.lectureId}`;
    if (this.src) return `ls_resume_src_${encodeURIComponent(this.src)}`;
    return 'ls_resume_default';
  }

  private checkAutoResume(duration: number): void {
    // Auto-resume is completely stopped per user request. Always start from 00:00.
    this.showResumeBanner = false;
    try {
      localStorage.removeItem(this.getStorageKey());
    } catch (e) {}
  }

  restartFromBeginning(): void {
    this.showResumeBanner = false;
    this.seekTo(0);
    this.cdr.markForCheck();
  }

  dismissResumeBanner(): void {
    this.showResumeBanner = false;
  }

  private saveProgress(time: number): void {
    // Auto-save progress disabled per user request
  }

  private processSrc(): void {
    if (!this.src) {
      this.isYouTube = false;
      this.currentYtVideoId = null;
      return;
    }

    const trimmed = this.src.trim();
    const ytId = this.extractYouTubeId(trimmed);

    if (ytId) {
      this.isYouTube = true;
      this.currentYtVideoId = ytId;
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

  private extractYouTubeId(url: string): string | null {
    if (!url) return null;
    const regExp = /(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/|youtube-nocookie\.com\/embed\/)([^"&?\/\s]{11})/i;
    const match = url.match(regExp);
    return match ? match[1] : null;
  }

  private initYouTubePlayer(videoId: string): void {
    this.isYtReady = false;

    const checkApiAndInit = () => {
      if (window.YT && window.YT.Player) {
        this.ngZone.runOutsideAngular(() => {
          if (this.ytPlayer && typeof this.ytPlayer.loadVideoById === 'function') {
            try {
              this.ytPlayer.cueVideoById(videoId);
              return;
            } catch (e) {}
          }

          const elem = document.getElementById('yt-player-target');
          if (!elem) {
            setTimeout(checkApiAndInit, 100);
            return;
          }

          this.ytPlayer = new window.YT.Player('yt-player-target', {
            width: '100%',
            height: '100%',
            videoId: videoId,
            playerVars: {
              autoplay: 0,
              controls: 0,
              disablekb: 1,
              enablejsapi: 1,
              fs: 0,
              modestbranding: 1,
              rel: 0,
              iv_load_policy: 3,
              playsinline: 1,
              showinfo: 0,
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

                  if (this.isPendingPlay) {
                    this.isPendingPlay = false;
                    this.ytPlayer.playVideo();
                  }
                  this.cdr.markForCheck();
                });
              },
              onStateChange: (event: any) => {
                this.ngZone.run(() => {
                  if (event.data === window.YT.PlayerState.PLAYING) {
                    this.isPlaying = true;
                    this.hasStartedPlaying = true;
                    this.startYtProgressTracker();
                    this.startControlsHideTimer();
                  } else if (event.data === window.YT.PlayerState.PAUSED) {
                    this.isPlaying = false;
                    this.stopYtProgressTracker();
                    this.showControls();
                  } else if (event.data === window.YT.PlayerState.ENDED) {
                    this.isPlaying = false;
                    this.stopYtProgressTracker();
                    this.showControls();
                    this.ended.emit();
                  }
                  this.cdr.markForCheck();
                });
              }
            }
          });
        });
      } else {
        setTimeout(checkApiAndInit, 100);
      }
    };

    setTimeout(checkApiAndInit, 50);
  }

  private destroyYouTubePlayer(): void {
    this.stopYtProgressTracker();
    this.isYtReady = false;
    if (this.ytPlayer) {
      try {
        this.ytPlayer.destroy();
      } catch (e) {}
      this.ytPlayer = null;
    }
  }

  private startYtProgressTracker(): void {
    this.stopYtProgressTracker();
    this.ytTimer = setInterval(() => {
      if (this.ytPlayer && typeof this.ytPlayer.getCurrentTime === 'function') {
        const time = this.ytPlayer.getCurrentTime() || 0;
        const dur = this.ytPlayer.getDuration() || this.duration;
        const frac = this.ytPlayer.getVideoLoadedFraction() || 0;

        this.ngZone.run(() => {
          this.currentTime = time;
          this.duration = dur;
          this.bufferedPercent = frac * 100;
          this.updateSubtitle(time);
          this.saveProgress(time);
          this.timeUpdate.emit({ currentTime: this.currentTime, duration: this.duration });
          this.cdr.markForCheck();
        });
      }
    }, 250);
  }

  private stopYtProgressTracker(): void {
    if (this.ytTimer) {
      clearInterval(this.ytTimer);
      this.ytTimer = null;
    }
  }

  private sendYtCommand(func: string, args: any = ''): void {
    try {
      const iframe = document.querySelector('#yt-player-target') as HTMLIFrameElement;
      if (iframe?.contentWindow) {
        iframe.contentWindow.postMessage(JSON.stringify({
          event: 'command',
          func: func,
          args: args
        }), '*');
      }
    } catch (e) {}
  }

  // --- Subtitles Engine ---

  private generateMockCues(lang: string = this.language): void {
    const title = this.videoTitle || 'this lecture';

    switch (lang) {
      case 'hinglish':
        this.cues = [
          { start: 0, end: 6, text: `Welcome students! Aaj ke lecture mein hum ${title} ke concepts explore karenge.` },
          { start: 6, end: 14, text: 'Machine Learning algorithms training dataset se functional mapping f: X -> Y seekhte hain.' },
          { start: 14, end: 24, text: 'Supervised, unsupervised aur reinforcement learning ke key differences ko analyze karte hain.' },
          { start: 24, end: 35, text: 'Supervised learning mein har training sample ke sath ground truth label y attached hota hai.' },
          { start: 35, end: 50, text: 'Model empirical loss function L(theta) = sum(loss(f(x_i), y_i)) ko minimize karta hai.' },
          { start: 50, end: 70, text: 'Gradient descent weight vectors ke respect mein loss ka partial derivative compute karta hai.' },
          { start: 70, end: 95, text: 'Overfitting prevent karne ke liye hum cross-validation techniques apply karte hain.' },
          { start: 95, end: 130, text: 'Decision boundaries aur support vectors ke mathematical intuition ko analyze kijiye.' },
          { start: 130, end: 180, text: 'Non-linear transformations features ko higher-dimensional spaces mein project karte hain.' },
          { start: 180, end: 240, text: 'Ye foundational principle neural network backpropagation equations ko drive karega.' }
        ];
        break;

      case 'hi':
        this.cues = [
          { start: 0, end: 6, text: `${title} में आपका स्वागत है। आज हम इसके मूलभूत सिद्धांतों का अध्ययन करेंगे।` },
          { start: 6, end: 14, text: 'मशीन लर्निंग प्रणालियाँ प्रशिक्षण डेटासेट से सीधे गणितीय प्रतिचित्रण सीखती हैं।' },
          { start: 14, end: 24, text: 'हम पर्यवेक्षित और अपर्यवेक्षित शिक्षण प्रणालियों का विश्लेषण करते हैं।' },
          { start: 24, end: 35, text: 'पर्यवेक्षित शिक्षण में प्रत्येक प्रशिक्षण नमूने के साथ एक सही लेबल जुड़ा होता है।' },
          { start: 35, end: 50, text: 'मॉडल हानि फलन (Loss Function) को न्यूनतम करने का प्रयास करता है।' },
          { start: 50, end: 70, text: 'ग्रेडिएंट डिसेंट प्रत्येक भार के सापेक्ष आंशिक अवकलज की गणना करता है।' },
          { start: 70, end: 95, text: 'ओवरफिटिंग रोकने के लिए हम क्रॉस-वैलिडेशन तकनीकों का उपयोग करते हैं।' },
          { start: 95, end: 130, text: 'निर्णय सीमाओं और सपोर्ट वेक्टर्स के पीछे के गणितीय अंतर्ज्ञान को समझें।' },
          { start: 130, end: 180, text: 'गैर-रैखिक परिवर्तन डेटा को उच्च-आयामी स्पेस में प्रोजेक्ट करते हैं।' },
          { start: 180, end: 240, text: 'यह सिद्धांत हमारे न्यूरल नेटवर्क बैकप्रॉपैगेशन समीकरणों का मार्गदर्शन करेगा।' }
        ];
        break;

      case 'mr':
        this.cues = [
          { start: 0, end: 6, text: `${title} मध्ये आपले स्वागत आहे. आज आपण मूलभूत संकल्पना शिकणार आहोत.` },
          { start: 6, end: 14, text: 'मशीन लर्निंग अल्गोरिदम थेट प्रशिक्षण डेटामधून मॅपिंग शिकतात.' },
          { start: 14, end: 24, text: 'सुपरव्हाइज्ड आणि अनसुपरव्हाइज्ड लर्निंगमधील फरक समजून घेऊया.' },
          { start: 24, end: 35, text: 'सुपरव्हाइज्ड लर्निंगमध्ये प्रत्येक सॅम्पलसोबत लेबल जोडलेले असते.' },
          { start: 35, end: 50, text: 'मॉडेल एरर फंक्शन L(theta) कमी करण्याचा प्रयत्न करते.' },
          { start: 50, end: 70, text: 'ग्रेडियंट डिझेंट अल्गोरिदम प्रत्येक वेटसाठी आंशिक डेरिव्हेटिव्ह काढतो.' },
          { start: 70, end: 95, text: 'ओव्हरफिटिंग टाळण्यासाठी क्रॉस-व्हॅलिडेशन पद्धती वापरल्या जातात.' },
          { start: 95, end: 130, text: 'डिसिजन बाउंड्रीज आणि सपोर्ट वेक्टर्सचे गणितीय विश्लेषण करा.' },
          { start: 130, end: 180, text: 'नॉन-लिनियर ट्रान्सफॉर्मेशन डेटाला उच्च डायमेंशनमध्ये प्रोजेक्ट करतात.' },
          { start: 180, end: 240, text: 'हा मूलभूत नियम न्यूरल नेटवर्क बॅकप्रॉपॅगेशनसाठी मार्गदर्शक ठरेल.' }
        ];
        break;

      case 'es':
        this.cues = [
          { start: 0, end: 6, text: `Bienvenidos a ${title}. Hoy exploraremos los conceptos fundamentales.` },
          { start: 6, end: 14, text: 'Los algoritmos de aprendizaje automático aprenden directamente de los datos.' },
          { start: 14, end: 24, text: 'Distinguimos entre aprendizaje supervisado, no supervisado y por refuerzo.' },
          { start: 24, end: 35, text: 'En el aprendizaje supervisado, cada muestra tiene una etiqueta real y.' },
          { start: 35, end: 50, text: 'El modelo minimiza una función de pérdida empírica: L(theta).' },
          { start: 50, end: 70, text: 'El descenso de gradiente calcula la derivada parcial con respecto a cada peso.' },
          { start: 70, end: 95, text: 'Analizamos la validación cruzada para evitar el sobreajuste.' },
          { start: 95, end: 130, text: 'Examinemos la intuición matemática de los límites de decisión.' },
          { start: 130, end: 180, text: 'Las transformaciones no lineales proyectan datos a espacios de mayor dimensión.' },
          { start: 180, end: 240, text: 'Este principio guiará nuestras ecuaciones de retropropagación.' }
        ];
        break;

      case 'en':
      default:
        this.cues = [
          { start: 0, end: 6, text: `Welcome to ${title}. Today we'll explore foundational concepts.` },
          { start: 6, end: 14, text: 'Machine Learning algorithms learn patterns and rules directly from training data.' },
          { start: 14, end: 24, text: 'We distinguish between supervised, unsupervised, and reinforcement learning paradigms.' },
          { start: 24, end: 35, text: 'In supervised learning, every training sample has an associated ground truth label y.' },
          { start: 35, end: 50, text: 'The model minimizes an empirical loss function: L(theta) = sum(loss(f(x_i), y_i)).' },
          { start: 50, end: 70, text: 'Gradient descent computes the partial derivative with respect to each weight.' },
          { start: 70, end: 95, text: 'Next, we analyze cross-validation techniques to prevent overfitting on test sets.' },
          { start: 95, end: 130, text: 'Let us examine the mathematical intuition behind decision boundaries and support vectors.' },
          { start: 130, end: 180, text: 'Notice how non-linear transformations project data into higher-dimensional feature spaces.' },
          { start: 180, end: 240, text: 'This foundational principle will guide our neural network backpropagation equations.' }
        ];
        break;
    }
  }

  private updateSubtitle(time: number): void {
    if (!this.isCaptionsActive || this.currentSubtitleTrack === 'Off') {
      this.currentSubtitleText = '';
      return;
    }
    const active = this.cues.find(c => time >= c.start && time < c.end);
    this.currentSubtitleText = active ? active.text : '';
  }

  toggleCaptions(): void {
    this.isCaptionsActive = !this.isCaptionsActive;
    if (this.isCaptionsActive) {
      if (this.currentSubtitleTrack === 'Off') {
        this.currentSubtitleTrack = 'English (Auto)';
      }
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
    this.hasStartedPlaying = true;

    if (this.isYouTube) {
      if (!this.isYtReady || !this.ytPlayer) {
        this.isPendingPlay = !this.isPlaying;
        this.isPlaying = !this.isPlaying;
        this.cdr.markForCheck();
        return;
      }

      try {
        const state = typeof this.ytPlayer.getPlayerState === 'function' ? this.ytPlayer.getPlayerState() : -1;
        if (state === window.YT?.PlayerState?.PLAYING) {
          this.ytPlayer.pauseVideo();
          this.isPlaying = false;
        } else {
          this.ytPlayer.playVideo();
          this.isPlaying = true;
          this.startYtProgressTracker();
          this.startControlsHideTimer();
        }
      } catch (err) {
        this.isPlaying = !this.isPlaying;
        this.sendYtCommand(this.isPlaying ? 'playVideo' : 'pauseVideo');
      }
      this.cdr.markForCheck();
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
    if (!video) return;
    this.currentTime = video.currentTime;
    this.duration = video.duration || this.duration;

    if (video.buffered.length > 0) {
      const bufferedEnd = video.buffered.end(video.buffered.length - 1);
      this.bufferedPercent = (bufferedEnd / this.duration) * 100;
    }

    this.updateSubtitle(this.currentTime);
    this.saveProgress(this.currentTime);
    this.timeUpdate.emit({ currentTime: this.currentTime, duration: this.duration });
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

    if (this.isYouTube && this.ytPlayer) {
      try {
        this.ytPlayer.seekTo(value, true);
      } catch (e) {
        this.sendYtCommand('seekTo', [value, true]);
      }
      this.currentTime = value;
    } else if (this.videoEl?.nativeElement) {
      this.videoEl.nativeElement.currentTime = value;
      this.currentTime = value;
    }
  }

  seekRelative(seconds: number): void {
    const target = Math.max(0, Math.min(this.currentTime + seconds, this.duration));
    if (this.isYouTube && this.ytPlayer) {
      try {
        this.ytPlayer.seekTo(target, true);
      } catch (e) {
        this.sendYtCommand('seekTo', [target, true]);
      }
      this.currentTime = target;
    } else if (this.videoEl?.nativeElement) {
      this.videoEl.nativeElement.currentTime = target;
      this.currentTime = target;
    }
    this.showControls();
  }

  seekTo(seconds: number): void {
    if (this.isYouTube && this.ytPlayer) {
      try {
        this.ytPlayer.seekTo(seconds, true);
      } catch (e) {
        this.sendYtCommand('seekTo', [seconds, true]);
      }
      this.currentTime = seconds;
    } else if (this.videoEl?.nativeElement) {
      this.videoEl.nativeElement.currentTime = seconds;
      this.currentTime = seconds;
    }
    this.showControls();
  }

  setVolume(event: Event): void {
    const input = event.target as HTMLInputElement;
    const val = parseFloat(input.value);
    this.volume = val;
    this.isMuted = val === 0;

    try {
      localStorage.setItem('ls_player_volume', val.toString());
    } catch (e) {}

    if (this.isYouTube && this.ytPlayer) {
      try {
        if (this.isMuted) {
          this.ytPlayer.mute();
        } else {
          this.ytPlayer.unMute();
          this.ytPlayer.setVolume(val * 100);
        }
      } catch (e) {}
    } else if (this.videoEl?.nativeElement) {
      this.videoEl.nativeElement.volume = val;
      this.videoEl.nativeElement.muted = this.isMuted;
    }
  }

  toggleMute(): void {
    this.isMuted = !this.isMuted;
    if (this.isYouTube && this.ytPlayer) {
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
    if (this.isYouTube && this.ytPlayer && typeof this.ytPlayer.setPlaybackRate === 'function') {
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
      if ('documentPictureInPicture' in window && window.documentPictureInPicture) {
        if (window.documentPictureInPicture.window) {
          window.documentPictureInPicture.window.close();
          return;
        }

        const pipWindow = await window.documentPictureInPicture.requestWindow({
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
