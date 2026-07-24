import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TransactionService } from './transaction.service';

describe('TransactionService', () => {
  let service: TransactionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TransactionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('passes list filters and pagination params', () => {
    service
      .findAll({ type: 'EXPENSE', categoryId: 10, startDate: '2026-01-01', endDate: '2026-01-31', page: 0, size: 20 })
      .subscribe();

    const req = httpMock.expectOne(
      (request) =>
        request.url === 'http://localhost:8080/api/transactions' &&
        request.params.get('type') === 'EXPENSE' &&
        request.params.get('categoryId') === '10' &&
        request.params.get('startDate') === '2026-01-01' &&
        request.params.get('endDate') === '2026-01-31' &&
        request.params.get('page') === '0' &&
        request.params.get('size') === '20'
    );
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: { content: [] }, timestamp: '2026-01-01T00:00:00Z' });
  });

  it('does not send empty optional filters', () => {
    service
      .findAll({ type: undefined, categoryId: undefined, startDate: undefined, endDate: undefined, page: 0, size: 20 })
      .subscribe();

    const req = httpMock.expectOne(
      (request) =>
        request.url === 'http://localhost:8080/api/transactions' &&
        request.params.get('page') === '0' &&
        request.params.get('size') === '20'
    );
    expect(req.request.params.has('type')).toBeFalse();
    expect(req.request.params.has('categoryId')).toBeFalse();
    expect(req.request.params.has('startDate')).toBeFalse();
    expect(req.request.params.has('endDate')).toBeFalse();
    req.flush({ success: true, data: { content: [] }, timestamp: '2026-01-01T00:00:00Z' });
  });

  it('supports detail, create, update and delete', () => {
    const body = { amount: 10, type: 'EXPENSE' as const, categoryId: 10, date: '2026-01-01', note: 'note' };

    service.findById(1).subscribe();
    const detailReq = httpMock.expectOne('http://localhost:8080/api/transactions/1');
    expect(detailReq.request.method).toBe('GET');
    detailReq.flush({ success: true, data: {}, timestamp: '2026-01-01T00:00:00Z' });

    service.create(body).subscribe();
    const createReq = httpMock.expectOne('http://localhost:8080/api/transactions');
    expect(createReq.request.method).toBe('POST');
    createReq.flush({ success: true, data: {}, timestamp: '2026-01-01T00:00:00Z' });

    service.update(1, body).subscribe();
    const updateReq = httpMock.expectOne('http://localhost:8080/api/transactions/1');
    expect(updateReq.request.method).toBe('PUT');
    updateReq.flush({ success: true, data: {}, timestamp: '2026-01-01T00:00:00Z' });

    service.delete(1).subscribe();
    const deleteReq = httpMock.expectOne('http://localhost:8080/api/transactions/1');
    expect(deleteReq.request.method).toBe('DELETE');
    deleteReq.flush({ success: true, data: null, timestamp: '2026-01-01T00:00:00Z' });
  });
});
