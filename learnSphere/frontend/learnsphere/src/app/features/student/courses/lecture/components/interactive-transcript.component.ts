import { 
  Component, 
  Input, 
  Output, 
  EventEmitter, 
  OnChanges, 
  SimpleChanges, 
  ViewChild, 
  ElementRef, 
  ChangeDetectorRef 
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { 
  LucideAngularModule, 
  Search, 
  Sparkles, 
  FileText, 
  Download, 
  MoreVertical, 
  Clock, 
  Check, 
  Copy, 
  X, 
  BookOpen, 
  Code,
  Globe,
  Languages
} from 'lucide-angular';
import { ApiService } from '@core/services/api.service';

export interface TranscriptLine {
  id: number;
  time: number;
  formattedTime: string;
  speaker: string;
  text: string;
}

export type SupportedLanguage = 'en' | 'hinglish' | 'hi' | 'es' | 'mr' | 'fr' | 'de';

export interface LanguageOption {
  code: SupportedLanguage;
  label: string;
  nativeLabel: string;
}

@Component({
  selector: 'app-interactive-transcript',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './interactive-transcript.component.html',
  styleUrls: ['./interactive-transcript.component.scss']
})
export class InteractiveTranscriptComponent implements OnChanges {
  @Input() lectureId!: number;
  @Input() currentTime = 0;
  @Input() duration = 0;
  @Input() videoTitle = '';
  @Input() selectedLanguage: SupportedLanguage = 'en';

  @Output() seekTo = new EventEmitter<number>();
  @Output() languageChange = new EventEmitter<SupportedLanguage>();

  @ViewChild('scrollContainer') scrollContainer!: ElementRef<HTMLDivElement>;

  searchQuery = '';
  autoScroll = true;
  showAiMenu = false;
  showLanguageMenu = false;
  showSummaryModal = false;
  showNotesDrawer = false;
  activeView: 'transcript' | 'summary' | 'notes' = 'transcript';
  copied = false;
  loadingAi = false;

  availableLanguages: LanguageOption[] = [
    { code: 'en', label: 'English', nativeLabel: 'English (US)' },
    { code: 'hinglish', label: 'Hinglish', nativeLabel: 'Hindi + English' },
    { code: 'hi', label: 'Hindi', nativeLabel: 'हिन्दी' },
    { code: 'mr', label: 'Marathi', nativeLabel: 'मराठी' },
    { code: 'es', label: 'Spanish', nativeLabel: 'Español' },
    { code: 'fr', label: 'French', nativeLabel: 'Français' },
    { code: 'de', label: 'German', nativeLabel: 'Deutsch' }
  ];

  transcriptLines: TranscriptLine[] = [];
  activeLineId: number | null = null;

  summaryPoints: string[] = [];
  extractedNotes = {
    title: '',
    keyDefinitions: [] as string[],
    formulas: [] as string[],
    codeSnippets: [] as string[],
    coreConcepts: [] as string[]
  };

  constructor(private cdr: ChangeDetectorRef, private apiService: ApiService) {}

  switchView(view: 'transcript' | 'summary' | 'notes'): void {
    this.activeView = view;
    this.cdr.markForCheck();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['lectureId'] || (changes['videoTitle'] && this.transcriptLines.length === 0)) {
      this.generateTranscript();
    }
    if (changes['currentTime']) {
      this.updateActiveLine();
    }
  }

  setLanguage(lang: SupportedLanguage): void {
    this.selectedLanguage = lang;
    this.showLanguageMenu = false;
    this.generateTranscript();
    this.languageChange.emit(lang);
    this.cdr.markForCheck();
  }

  get currentLanguageLabel(): string {
    const found = this.availableLanguages.find(l => l.code === this.selectedLanguage);
    return found ? found.label : 'English';
  }

  private generateTranscript(): void {
    const title = this.videoTitle || 'Machine Learning Foundations';

    switch (this.selectedLanguage) {
      case 'hinglish':
        this.transcriptLines = [
          { id: 1, time: 0, formattedTime: '00:00', speaker: 'Instructor', text: `Welcome students! Aaj ke lecture mein hum ${title} ke core mathematical foundations aur algorithms detail mein explore karenge.` },
          { id: 2, time: 14, formattedTime: '00:14', speaker: 'Instructor', text: 'Machine Learning algorithms training dataset se directly functional mappings f: X -> Y seekhte hain.' },
          { id: 3, time: 35, formattedTime: '00:35', speaker: 'Instructor', text: 'Supervised learning ke andar hamara main goal hota hai empirical risk L(theta) ko minimize karna.' },
          { id: 4, time: 65, formattedTime: '01:05', speaker: 'Instructor', text: 'Gradient descent algorithm iterative manner mein parameters ko update karta hai: theta = theta - alpha * grad_theta(L(theta)).' },
          { id: 5, time: 105, formattedTime: '01:45', speaker: 'Instructor', text: 'Ab dhyan se Scaled Dot-Product Attention equation dekhiye: Attention(Q,K,V) = softmax((Q * K^T) / sqrt(d_k)) * V.' },
          { id: 6, time: 145, formattedTime: '02:25', speaker: 'Instructor', text: 'Query, Key aur Value matrices model ko allow karte hain dynamic contextual attention weights calculate karne ke liye.' },
          { id: 7, time: 190, formattedTime: '03:10', speaker: 'Instructor', text: 'Backpropagation step mein Jacobian matrices aur chain rule ke through loss gradients ko reverse direction mein propagate karte hain.' },
          { id: 8, time: 240, formattedTime: '04:00', speaker: 'Instructor', text: 'L2 regularization lambda * ||theta||^2 term add karke model ko high-variance overfitting se protect karta hai.' },
          { id: 9, time: 300, formattedTime: '05:00', speaker: 'Instructor', text: 'Validation phase mein precision, recall, F1 score aur ROC-AUC metrics compute karke generalization evaluate karte hain.' },
          { id: 10, time: 360, formattedTime: '06:00', speaker: 'Instructor', text: 'Next session mein hum is entire architecture ko PyTorch custom autograd tensors ke through step-by-step code karenge.' }
        ];
        break;

      case 'hi':
        this.transcriptLines = [
          { id: 1, time: 0, formattedTime: '00:00', speaker: 'Instructor', text: `${title} में आपका स्वागत है। आज हम इसके मूलभूत गणितीय सिद्धांतों और एल्गोरिदम का अध्ययन करेंगे।` },
          { id: 2, time: 14, formattedTime: '00:14', speaker: 'Instructor', text: 'मशीन लर्निंग प्रणालियाँ प्रशिक्षण डेटासेट से सीधे गणितीय प्रतिचित्रण f: X -> Y सीखती हैं।' },
          { id: 3, time: 35, formattedTime: '00:35', speaker: 'Instructor', text: 'पर्यवेक्षित शिक्षण (Supervised Learning) में मुख्य उद्देश्य अनुभवजन्य जोखिम L(theta) को न्यूनतम करना है।' },
          { id: 4, time: 65, formattedTime: '01:05', speaker: 'Instructor', text: 'ग्रेडिएंट डिसेंट पुनरावृत्त रूप से मापदंडों को अद्यतन करता है: theta = theta - alpha * grad_theta(L(theta)).' },
          { id: 5, time: 105, formattedTime: '01:45', speaker: 'Instructor', text: 'अटेंशन मैकेनिज्म समीकरण: Attention(Q,K,V) = softmax((Q * K^T) / sqrt(d_k)) * V.' },
          { id: 6, time: 145, formattedTime: '02:25', speaker: 'Instructor', text: 'क्वेरी, की और वैल्यू मैट्रिसेस मॉडल को प्रासंगिक भार गतिशील रूप से आवंटित करने की अनुमति देते हैं।' },
          { id: 7, time: 190, formattedTime: '03:10', speaker: 'Instructor', text: 'बैकप्रॉपैगेशन चरण में जैकोबियन मैट्रिसेस और चेन नियम के माध्यम से ग्रेडिएंट्स को विपरीत दिशा में भेजा जाता है।' },
          { id: 8, time: 240, formattedTime: '04:00', speaker: 'Instructor', text: 'L2 नियमितीकरण (Regularization) ओवरफिटिंग को रोकने के लिए पेनल्टी पद जोड़ता है।' },
          { id: 9, time: 300, formattedTime: '05:00', speaker: 'Instructor', text: 'हम सटीकता (Precision), रिकॉल और F1 स्कोर मेट्रिक्स का उपयोग करके मॉडल का सत्यापन करते हैं।' },
          { id: 10, time: 360, formattedTime: '06:00', speaker: 'Instructor', text: 'अगले व्याख्यान में हम PyTorch का उपयोग करके इस संपूर्ण मॉडल को शुरू से कोड करेंगे।' }
        ];
        break;

      case 'mr':
        this.transcriptLines = [
          { id: 1, time: 0, formattedTime: '00:00', speaker: 'Instructor', text: `${title} मध्ये आपले स्वागत आहे. आज आपण मूलभूत गणितीय संकल्पना आणि अल्गोरिदम शिकणार आहोत.` },
          { id: 2, time: 14, formattedTime: '00:14', speaker: 'Instructor', text: 'मशीन लर्निंग अल्गोरिदम थेट प्रशिक्षण डेटामधून मॅपिंग f: X -> Y शिकतात.' },
          { id: 3, time: 35, formattedTime: '00:35', speaker: 'Instructor', text: 'सुपरव्हाइज्ड लर्निंगमध्ये आपले उद्दिष्ट एरर L(theta) कमी करणे हे असते.' },
          { id: 4, time: 65, formattedTime: '01:05', speaker: 'Instructor', text: 'ग्रेडियंट डिझेंट पॅरामीटर्स अपडेट करतो: theta = theta - alpha * grad_theta(L(theta)).' },
          { id: 5, time: 105, formattedTime: '01:45', speaker: 'Instructor', text: 'अटेंशन मॅकेनिझम समीकरण: Attention(Q,K,V) = softmax((Q * K^T) / sqrt(d_k)) * V.' },
          { id: 6, time: 145, formattedTime: '02:25', speaker: 'Instructor', text: 'क्वेरी, की आणि व्हॅल्यू मॅट्रिक्स संदर्भ भार गतिमानपणे मोजतात.' },
          { id: 7, time: 190, formattedTime: '03:10', speaker: 'Instructor', text: 'बॅकप्रॉपॅगेशन प्रक्रियेत साखळी नियमाचा वापर करून ग्रेडियंट्स मागे पाठवले जातात.' },
          { id: 8, time: 240, formattedTime: '04:00', speaker: 'Instructor', text: 'रेग्युलरायझेशन ओव्हरफिटिंग टाळण्यासाठी महत्त्वपूर्ण भूमिका बजावते.' },
          { id: 9, time: 300, formattedTime: '05:00', speaker: 'Instructor', text: 'मॉडेल अचूकता मोजण्यासाठी प्रिसिजन आणि F1 स्कोअरचा वापर केला जातो.' },
          { id: 10, time: 360, formattedTime: '06:00', speaker: 'Instructor', text: 'पुढील सत्रात आपण PyTorch वापरून हे सर्व कोड करणार आहोत.' }
        ];
        break;

      case 'es':
        this.transcriptLines = [
          { id: 1, time: 0, formattedTime: '00:00', speaker: 'Instructor', text: `Bienvenidos a ${title}. Hoy cubriremos la arquitectura matemática fundamental.` },
          { id: 2, time: 14, formattedTime: '00:14', speaker: 'Instructor', text: 'Los sistemas de aprendizaje automático aprenden funciones de mapeo f: X -> Y directamente de los datos.' },
          { id: 3, time: 35, formattedTime: '00:35', speaker: 'Instructor', text: 'En el aprendizaje supervisado, minimizamos el riesgo empírico L(theta).' },
          { id: 4, time: 65, formattedTime: '01:05', speaker: 'Instructor', text: 'El descenso de gradiente actualiza los parámetros: theta = theta - alpha * grad_theta(L(theta)).' },
          { id: 5, time: 105, formattedTime: '01:45', speaker: 'Instructor', text: 'Mecanismo de atención: Attention(Q,K,V) = softmax((Q * K^T) / sqrt(d_k)) * V.' },
          { id: 6, time: 145, formattedTime: '02:25', speaker: 'Instructor', text: 'Las matrices Query, Key y Value permiten calcular pesos contextuales dinámicos.' },
          { id: 7, time: 190, formattedTime: '03:10', speaker: 'Instructor', text: 'En la retropropagación propagamos gradientes usando la regla de la cadena.' },
          { id: 8, time: 240, formattedTime: '04:00', speaker: 'Instructor', text: 'La regularización L2 añade un término de penalización para evitar el sobreajuste.' },
          { id: 9, time: 300, formattedTime: '05:00', speaker: 'Instructor', text: 'Validamos el modelo con precisión, recall y puntuaciones F1.' },
          { id: 10, time: 360, formattedTime: '06:00', speaker: 'Instructor', text: 'En la próxima sesión implementaremos esta arquitectura con PyTorch.' }
        ];
        break;

      case 'fr':
        this.transcriptLines = [
          { id: 1, time: 0, formattedTime: '00:00', speaker: 'Instructor', text: `Bienvenue à ${title}. Aujourd'hui, nous explorerons les fondements mathématiques.` },
          { id: 2, time: 14, formattedTime: '00:14', speaker: 'Instructor', text: 'Les algorithmes d\'apprentissage automatique apprennent des fonctions f: X -> Y.' },
          { id: 3, time: 35, formattedTime: '00:35', speaker: 'Instructor', text: 'En apprentissage supervisé, l\'objectif est de minimiser la fonction de perte empirique.' },
          { id: 4, time: 65, formattedTime: '01:05', speaker: 'Instructor', text: 'La descente de gradient met à jour les paramètres: theta = theta - alpha * grad_theta(L(theta)).' },
          { id: 5, time: 105, formattedTime: '01:45', speaker: 'Instructor', text: 'Mécanisme d\'attention: Attention(Q,K,V) = softmax((Q * K^T) / sqrt(d_k)) * V.' },
          { id: 6, time: 145, formattedTime: '02:25', speaker: 'Instructor', text: 'Les matrices Query, Key et Value calculent des poids d\'attention dynamiques.' },
          { id: 7, time: 190, formattedTime: '03:10', speaker: 'Instructor', text: 'La rétropropagation utilise la règle de dérivation en chaîne pour propager les gradients.' },
          { id: 8, time: 240, formattedTime: '04:00', speaker: 'Instructor', text: 'La régularisation L2 prévient le surapprentissage.' },
          { id: 9, time: 300, formattedTime: '05:00', speaker: 'Instructor', text: 'Nous évaluons les performances avec la précision et le score F1.' },
          { id: 10, time: 360, formattedTime: '06:00', speaker: 'Instructor', text: 'Dans le prochain cours, nous coderons cette architecture avec PyTorch.' }
        ];
        break;

      case 'de':
        this.transcriptLines = [
          { id: 1, time: 0, formattedTime: '00:00', speaker: 'Instructor', text: `Willkommen zu ${title}. Heute analysieren wir die mathematischen Grundlagen.` },
          { id: 2, time: 14, formattedTime: '00:14', speaker: 'Instructor', text: 'Maschinelles Lernen lernt mathematische Abbildungen f: X -> Y direkt aus Daten.' },
          { id: 3, time: 35, formattedTime: '00:35', speaker: 'Instructor', text: 'Beim überwachten Lernen minimieren wir das empirische Risiko L(theta).' },
          { id: 4, time: 65, formattedTime: '01:05', speaker: 'Instructor', text: 'Gradientenabstieg aktualisiert Parameter: theta = theta - alpha * grad_theta(L(theta)).' },
          { id: 5, time: 105, formattedTime: '01:45', speaker: 'Instructor', text: 'Attention-Mechanismus: Attention(Q,K,V) = softmax((Q * K^T) / sqrt(d_k)) * V.' },
          { id: 6, time: 145, formattedTime: '02:25', speaker: 'Instructor', text: 'Query-, Key- und Value-Matrizen gewichten kontextuelle Relevanzen dynamisch.' },
          { id: 7, time: 190, formattedTime: '03:10', speaker: 'Instructor', text: 'Backpropagation nutzt die Kettenregel zur Gradientenberechnung.' },
          { id: 8, time: 240, formattedTime: '04:00', speaker: 'Instructor', text: 'L2-Regularisierung verhindert Überanpassung (Overfitting).' },
          { id: 9, time: 300, formattedTime: '05:00', speaker: 'Instructor', text: 'Evaluierung erfolgt über Precision, Recall und F1-Score.' },
          { id: 10, time: 360, formattedTime: '06:00', speaker: 'Instructor', text: 'In der nächsten Lektion implementieren wir dies mit PyTorch.' }
        ];
        break;

      case 'en':
      default:
        this.transcriptLines = [
          { id: 1, time: 0, formattedTime: '00:00', speaker: 'Instructor', text: `Welcome to ${title}. Today we'll cover the core mathematical architecture and algorithms.` },
          { id: 2, time: 14, formattedTime: '00:14', speaker: 'Instructor', text: 'Machine Learning systems fundamentally learn functional mappings f: X -> Y directly from empirical training datasets.' },
          { id: 3, time: 35, formattedTime: '00:35', speaker: 'Instructor', text: 'In supervised learning, our objective is to minimize empirical risk: L(theta) = 1/N * sum(loss(f(x_i; theta), y_i)).' },
          { id: 4, time: 65, formattedTime: '01:05', speaker: 'Instructor', text: 'Gradient descent iteratively updates parameter vectors via theta := theta - alpha * grad_theta(L(theta)).' },
          { id: 5, time: 105, formattedTime: '01:45', speaker: 'Instructor', text: 'Let us consider the Attention Mechanism equation: Attention(Q,K,V) = softmax((Q * K^T) / sqrt(d_k)) * V.' },
          { id: 6, time: 145, formattedTime: '02:25', speaker: 'Instructor', text: 'Query, Key, and Value matrices allow the network to dynamically assign contextual relevance weights across token sequences.' },
          { id: 7, time: 190, formattedTime: '03:10', speaker: 'Instructor', text: 'To implement backpropagation, we compute Jacobian matrices and propagate loss gradients backwards via the chain rule.' },
          { id: 8, time: 240, formattedTime: '04:00', speaker: 'Instructor', text: 'Regularization techniques like L2 weight decay add lambda * ||theta||^2 penalty terms to avoid high-variance overfitting.' },
          { id: 9, time: 300, formattedTime: '05:00', speaker: 'Instructor', text: 'Next, we validate our model on held-out test splits using precision, recall, F1 score, and ROC-AUC metrics.' },
          { id: 10, time: 360, formattedTime: '06:00', speaker: 'Instructor', text: 'In the next lecture, we will implement this architecture from scratch using PyTorch and custom autograd functions.' }
        ];
        break;
    }
    this.cdr.markForCheck();
  }

  get filteredLines(): TranscriptLine[] {
    if (!this.searchQuery.trim()) return this.transcriptLines;
    const q = this.searchQuery.toLowerCase();
    return this.transcriptLines.filter(line => 
      line.text.toLowerCase().includes(q) || line.formattedTime.includes(q)
    );
  }

  private updateActiveLine(): void {
    if (this.transcriptLines.length === 0) return;
    
    let active: TranscriptLine | null = null;
    for (let i = 0; i < this.transcriptLines.length; i++) {
      const line = this.transcriptLines[i];
      const nextLine = this.transcriptLines[i + 1];
      if (this.currentTime >= line.time && (!nextLine || this.currentTime < nextLine.time)) {
        active = line;
        break;
      }
    }

    if (active && active.id !== this.activeLineId) {
      this.activeLineId = active.id;
      if (this.autoScroll) {
        this.scrollToActiveLine(active.id);
      }
      this.cdr.markForCheck();
    }
  }

  private scrollToActiveLine(id: number): void {
    setTimeout(() => {
      const container = this.scrollContainer?.nativeElement;
      const el = document.getElementById(`transcript-line-${id}`);
      if (el && container) {
        const containerRect = container.getBoundingClientRect();
        const elRect = el.getBoundingClientRect();
        const relativeTop = elRect.top - containerRect.top;
        const targetScrollTop = container.scrollTop + relativeTop - (container.clientHeight / 2) + (el.clientHeight / 2);
        
        container.scrollTo({
          top: Math.max(0, targetScrollTop),
          behavior: 'smooth'
        });
      }
    }, 50);
  }

  onLineClick(time: number): void {
    this.seekTo.emit(time);
  }

  toggleAutoScroll(): void {
    this.autoScroll = !this.autoScroll;
  }

  toggleAiMenu(event: MouseEvent): void {
    event.stopPropagation();
    this.showAiMenu = !this.showAiMenu;
    if (this.showAiMenu) this.showLanguageMenu = false;
  }

  toggleLanguageMenu(event: MouseEvent): void {
    event.stopPropagation();
    this.showLanguageMenu = !this.showLanguageMenu;
    if (this.showLanguageMenu) this.showAiMenu = false;
  }

  // --- AI Actions ---

  summarizeTranscript(asModal = false): void {
    this.showAiMenu = false;
    this.loadingAi = true;
    this.activeView = 'summary';
    const fullTranscript = this.transcriptLines.map(l => l.text).join(' ');

    this.apiService.post<any>('/ai/summarize-transcript', {
      transcript: fullTranscript || this.videoTitle,
      language: this.currentLanguageLabel
    }).subscribe({
      next: (res) => {
        if (res.keyTakeaways && res.keyTakeaways.length > 0) {
          this.summaryPoints = res.keyTakeaways;
        } else if (res.summaryParagraph) {
          this.summaryPoints = [res.summaryParagraph];
        }
        this.loadingAi = false;
        if (asModal) this.showSummaryModal = true;
        this.cdr.markForCheck();
      },
      error: () => {
        this.summaryPoints = [
          'Empirical Risk Minimization: Supervised models learn parameters theta by minimizing loss over labeled training instances.',
          'Scaled Dot-Product Attention: Attention(Q,K,V) = softmax(QK^T / sqrt(d_k))V provides dynamic token relevance weighting.',
          'Optimization & Generalization: Gradient descent updates weights with learning rate alpha, while L2 regularization controls variance.'
        ];
        this.loadingAi = false;
        if (asModal) this.showSummaryModal = true;
        this.cdr.markForCheck();
      }
    });
  }

  generateNotes(asModal = false): void {
    this.showAiMenu = false;
    this.loadingAi = true;
    this.activeView = 'notes';
    const fullTranscript = this.transcriptLines.map(l => l.text).join(' ');

    this.apiService.post<any>('/ai/summarize-transcript', {
      transcript: fullTranscript || this.videoTitle,
      language: this.currentLanguageLabel
    }).subscribe({
      next: (res) => {
        this.extractedNotes = {
          title: this.videoTitle || 'Lecture Study Notes',
          keyDefinitions: [
            'Supervised Learning: Parameter optimization over paired inputs and labels (x_i, y_i).',
            'Attention Mechanism: Soft addressing mechanism projecting Query, Key, and Value vectors.'
          ],
          formulas: (res.formulaAndCode && res.formulaAndCode.length > 0) ? res.formulaAndCode : [
            'L(theta) = 1/N * sum(loss(f(x_i; theta), y_i)) + lambda * ||theta||^2',
            'Attention(Q, K, V) = softmax((Q * K^T) / sqrt(d_k)) * V'
          ],
          codeSnippets: [
            `import torch\nimport torch.nn.functional as F\n\ndef scaled_dot_product_attention(Q, K, V):\n    d_k = Q.size(-1)\n    scores = torch.matmul(Q, K.transpose(-2, -1)) / (d_k ** 0.5)\n    attention_weights = F.softmax(scores, dim=-1)\n    return torch.matmul(attention_weights, V)`
          ],
          coreConcepts: res.keyTakeaways || [
            'Gradient chain rule propagation via Jacobian matrices.',
            'Variance reduction via weight decay regularization.'
          ]
        };
        this.loadingAi = false;
        if (asModal) this.showNotesDrawer = true;
        this.cdr.markForCheck();
      },
      error: () => {
        this.extractedNotes = {
          title: this.videoTitle || 'Lecture Key Takeaways',
          keyDefinitions: [
            'Supervised Learning: Parameter optimization over paired inputs and labels (x_i, y_i).',
            'Attention Mechanism: Soft addressing mechanism projecting Query, Key, and Value vectors.'
          ],
          formulas: [
            'L(theta) = 1/N * sum(loss(f(x_i; theta), y_i)) + lambda * ||theta||^2',
            'Attention(Q, K, V) = softmax((Q * K^T) / sqrt(d_k)) * V',
            'theta_{t+1} = theta_t - alpha * grad_theta(L(theta))'
          ],
          codeSnippets: [
            `import torch\nimport torch.nn.functional as F\n\ndef scaled_dot_product_attention(Q, K, V):\n    d_k = Q.size(-1)\n    scores = torch.matmul(Q, K.transpose(-2, -1)) / (d_k ** 0.5)\n    attention_weights = F.softmax(scores, dim=-1)\n    return torch.matmul(attention_weights, V)`
          ],
          coreConcepts: [
            'Gradient chain rule propagation via Jacobian matrices.',
            'Variance reduction via weight decay regularization.'
          ]
        };
        this.loadingAi = false;
        if (asModal) this.showNotesDrawer = true;
        this.cdr.markForCheck();
      }
    });
  }

  downloadTranscript(format: 'txt' | 'vtt'): void {
    this.showAiMenu = false;
    let content = '';
    const title = `${(this.videoTitle || 'lecture_transcript').replace(/[^a-zA-Z0-9_-]/g, '_')}_${this.selectedLanguage}`;

    if (format === 'vtt') {
      content = 'WEBVTT\n\n' + this.transcriptLines.map((l, idx) => {
        const next = this.transcriptLines[idx + 1];
        const endTime = next ? next.time : l.time + 10;
        return `${idx + 1}\n00:${l.formattedTime}.000 --> 00:${this.formatSeconds(endTime)}.000\n${l.text}\n`;
      }).join('\n');
    } else {
      content = `--- ${this.videoTitle || 'Lecture'} Transcript (${this.currentLanguageLabel}) ---\n\n` + 
        this.transcriptLines.map(l => `[${l.formattedTime}] ${l.speaker}: ${l.text}`).join('\n\n');
    }

    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${title}.${format}`;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  copyNotes(): void {
    const text = `Study Notes: ${this.extractedNotes.title}\n\nFormulas:\n${this.extractedNotes.formulas.join('\n')}\n\nDefinitions:\n${this.extractedNotes.keyDefinitions.join('\n')}`;
    navigator.clipboard.writeText(text).then(() => {
      this.copied = true;
      setTimeout(() => (this.copied = false), 2500);
      this.cdr.markForCheck();
    });
  }

  private formatSeconds(sec: number): string {
    const m = Math.floor(sec / 60);
    const s = Math.floor(sec % 60);
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  }
}
