import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ErrorHandlerService } from './error-handler.service';

describe('ErrorHandlerService', () => {
  let service: ErrorHandlerService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ErrorHandlerService);
  });

  function response(status: number, error: unknown): HttpErrorResponse {
    return new HttpErrorResponse({ status, error });
  }

  it('prefers the first field validation message from problem+json', () => {
    const error = response(400, {
      code: 'VALIDATION_FAILED',
      detail: 'Validation failed',
      errors: [{ field: 'email', message: 'must be a well-formed email address' }],
    });

    expect(service.extractMessage(error)).toBe('must be a well-formed email address');
  });

  it('uses the problem detail for non-validation errors', () => {
    const error = response(404, {
      code: 'NOT_FOUND',
      detail: 'Course not found with id: 7',
    });

    expect(service.extractMessage(error)).toBe('Course not found with id: 7');
  });

  it('still reads the legacy message and fieldErrors shape', () => {
    expect(service.extractMessage(response(400, {
      message: 'legacy message',
      fieldErrors: { email: 'legacy email error' },
    }))).toBe('legacy email error');

    expect(service.extractMessage(response(400, { message: 'legacy message' })))
      .toBe('legacy message');
  });

  it('falls back when the server cannot be reached', () => {
    expect(service.extractMessage(response(0, null)))
      .toBe('Cannot reach the server. Please check your connection and try again.');
  });

  it('uses the supplied fallback for an unknown error shape', () => {
    expect(service.extractMessage(response(500, { unexpected: true }), 'Try again later.'))
      .toBe('Try again later.');
  });
});
