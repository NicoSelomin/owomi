import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { UserService } from './user.service';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('calls GET /api/users/me', () => {
    service.getMe().subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/users/me');
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {}, timestamp: '2026-01-01T00:00:00Z' });
  });
});
