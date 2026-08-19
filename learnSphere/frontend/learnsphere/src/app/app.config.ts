import { ApplicationConfig, importProvidersFrom, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { routes } from './app.routes';
import { tokenInterceptor } from '@core/auth/token.interceptor';
import { errorInterceptor } from '@core/interceptors/error.interceptor';

import {
  LucideAngularModule,
  Play, Pause, Users, BookOpen, ShieldAlert, GraduationCap, Award, DollarSign, Clock, Folder, BarChart, Radio, Calendar, Target, Clapperboard, Megaphone, Square, Medal, PartyPopper, Flame, MessageCircle, CornerDownLeft, Trash2, CheckCircle2, Search, Plus, Video, Trophy, Star, Rocket, CheckCircle, FileText, Globe, Monitor, Ruler, User, Settings, Flag, Siren, Banknote, Zap, Hourglass, MapPin, Send, MessageSquare, X,
  RotateCcw, RotateCw, Volume2, VolumeX, Maximize, Minimize, PictureInPicture2, Sparkles,
  Subtitles, MoreVertical, Download, ChevronRight, ChevronDown, SlidersHorizontal, Layers, Share2, Bookmark, Code, Check,
  Sun, Moon, SunMoon, Languages, Bell, BellRing, HelpCircle, CheckCheck, Loader2, Menu,
  CreditCard, ArrowUpRight, Printer, ShoppingBag
} from 'lucide-angular';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([tokenInterceptor, errorInterceptor])),
    provideAnimationsAsync(),
    importProvidersFrom(
      MatSnackBarModule,
      LucideAngularModule.pick({
        Play, Pause, Users, BookOpen, ShieldAlert, GraduationCap, Award, DollarSign, Clock, Folder, BarChart, Radio, Calendar, Target, Clapperboard, Megaphone, Square, Medal, PartyPopper, Flame, MessageCircle, CornerDownLeft, Trash2, CheckCircle2, Search, Plus, Video, Trophy, Star, Rocket, CheckCircle, FileText, Globe, Monitor, Ruler, User, Settings, Flag, Siren, Banknote, Zap, Hourglass, MapPin, Send, MessageSquare, X,
        RotateCcw, RotateCw, Volume2, VolumeX, Maximize, Minimize, PictureInPicture2, Sparkles,
        Subtitles, MoreVertical, Download, ChevronRight, ChevronDown, SlidersHorizontal, Layers, Share2, Bookmark, Code, Check,
        Sun, Moon, SunMoon, Languages, Bell, BellRing, HelpCircle, CheckCheck, Loader2, Menu,
        CreditCard, ArrowUpRight, Printer, ShoppingBag
      })
    ),
  ]
};