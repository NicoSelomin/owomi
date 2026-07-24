import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiService } from './api.service';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('builds GET requests with query params and skips null values', () => {
    service.get('/api/test', { page: 0, size: 20, empty: null }).subscribe();

    const req = httpMock.expectOne(
      (request) =>
        request.method === 'GET' &&
        request.url === 'http://localhost:8080/api/test' &&
        request.params.get('page') === '0' &&
        request.params.get('size') === '20' &&
        !request.params.has('empty')
    );
    expect(req.request.method).toBe('GET');
    req.flush({ success: true });
  });

  it('supports POST, PUT and DELETE methods', () => {
    service.post('/api/test', { name: 'A' }).subscribe();
    const postReq = httpMock.expectOne('http://localhost:8080/api/test');
    expect(postReq.request.method).toBe('POST');
    postReq.flush({});

    service.put('/api/test/1', { name: 'B' }).subscribe();
    const putReq = httpMock.expectOne('http://localhost:8080/api/test/1');
    expect(putReq.request.method).toBe('PUT');
    putReq.flush({});

    service.delete('/api/test/1').subscribe();
    const deleteReq = httpMock.expectOne('http://localhost:8080/api/test/1');
    expect(deleteReq.request.method).toBe('DELETE');
    deleteReq.flush({});
  });

  it('requests Blob responses with observe response', () => {
    service.getBlobResponse('/api/export', { type: 'EXPENSE' }).subscribe((response) => {
      expect(response.body?.type).toBe('text/csv');
    });

    const req = httpMock.expectOne(
      (request) =>
        request.method === 'GET' &&
        request.url === 'http://localhost:8080/api/export' &&
        request.params.get('type') === 'EXPENSE'
    );
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['csv'], { type: 'text/csv' }));
  });
});
