import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { tokenInterceptor } from './token.interceptor';
import { AuthStore } from './auth.store';

describe('tokenInterceptor (W1-06)', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authStore: AuthStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthStore,
        provideHttpClient(withInterceptors([tokenInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authStore = TestBed.inject(AuthStore);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('attaches in-memory token as Bearer and sets withCredentials: true', () => {
    authStore.setAccessToken('in-memory-jwt-token-123');

    http.get('/api/users/me').subscribe();

    const req = httpMock.expectOne('/api/users/me');
    expect(req.request.headers.get('Authorization')).toBe('Bearer in-memory-jwt-token-123');
    expect(req.request.withCredentials).toBe(true);
    req.flush({});
  });

  it('leaves Authorization absent but still sets withCredentials: true when unauthenticated', () => {
    authStore.clearSession();

    http.get('/api/courses').subscribe();

    const req = httpMock.expectOne('/api/courses');
    expect(req.request.headers.has('Authorization')).toBe(false);
    expect(req.request.withCredentials).toBe(true);
    req.flush({});
  });
});
