import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { CourseFeedback, CourseRatingSummary } from '@core/models/course.model';

@Injectable({
  providedIn: 'root'
})
export class FeedbackService {

  constructor(private api: ApiService) {}

  submitFeedback(courseId: number, rating: number, message?: string): Observable<CourseFeedback> {
    return this.api.post<CourseFeedback>(`/courses/${courseId}/feedback`, { rating, message });
  }

  getCourseFeedback(courseId: number): Observable<CourseFeedback[]> {
    return this.api.get<CourseFeedback[]>(`/courses/${courseId}/feedback`);
  }

  getAverageRating(courseId: number): Observable<CourseRatingSummary> {
    return this.api.get<CourseRatingSummary>(`/courses/${courseId}/feedback/avg`);
  }

  getFeedbackById(id: number): Observable<CourseFeedback> {
    return this.api.get<CourseFeedback>(`/feedback/${id}`);
  }

  deleteFeedback(id: number): Observable<void> {
    return this.api.delete<void>(`/feedback/${id}`);
  }
}
