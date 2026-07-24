import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CurrencyService } from './currency.service';

describe('CurrencyService', () => {
  let service: CurrencyService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CurrencyService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('calls GET /api/currencies', () => {
    service.findAll().subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/currencies');
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [], timestamp: '2026-01-01T00:00:00Z' });
  });
});
