import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CategoryService } from './category.service';

describe('CategoryService', () => {
  let service: CategoryService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CategoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads all categories without optional type filter', () => {
    service.findAll().subscribe();

    const req = httpMock.expectOne(
      (request) =>
        request.url === 'http://localhost:8080/api/categories' &&
        !request.params.has('type')
    );
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [], timestamp: '2026-01-01T00:00:00Z' });
  });

  it('passes optional type filter', () => {
    service.findAll('EXPENSE').subscribe();

    const req = httpMock.expectOne(
      (request) =>
        request.url === 'http://localhost:8080/api/categories' &&
        request.params.get('type') === 'EXPENSE'
    );
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [], timestamp: '2026-01-01T00:00:00Z' });
  });

  it('loads category details by id', () => {
    service.findById(10).subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/categories/10');
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {}, timestamp: '2026-01-01T00:00:00Z' });
  });

  it('supports create, update and delete with exact routes and body', () => {
    const body = { name: 'Transport', icon: 'car', color: '#123456', type: 'EXPENSE' as const };

    service.create(body).subscribe();
    const createReq = httpMock.expectOne('http://localhost:8080/api/categories');
    expect(createReq.request.method).toBe('POST');
    expect(createReq.request.body).toEqual(body);
    createReq.flush({ success: true, data: {}, timestamp: '2026-01-01T00:00:00Z' });

    service.update(10, body).subscribe();
    const updateReq = httpMock.expectOne('http://localhost:8080/api/categories/10');
    expect(updateReq.request.method).toBe('PUT');
    expect(updateReq.request.body).toEqual(body);
    updateReq.flush({ success: true, data: {}, timestamp: '2026-01-01T00:00:00Z' });

    service.delete(10).subscribe();
    const deleteReq = httpMock.expectOne('http://localhost:8080/api/categories/10');
    expect(deleteReq.request.method).toBe('DELETE');
    deleteReq.flush({ success: true, data: null, timestamp: '2026-01-01T00:00:00Z' });
  });

  it('returns backend API errors to the caller', (done) => {
    service.delete(10).subscribe({
      error: (error) => {
        expect(error.status).toBe(400);
        expect(error.error.error.code).toBe('CATEGORY_HAS_TRANSACTIONS');
        done();
      },
    });

    const req = httpMock.expectOne('http://localhost:8080/api/categories/10');
    req.flush(
      {
        success: false,
        error: {
          code: 'CATEGORY_HAS_TRANSACTIONS',
          message: 'Cette catégorie contient des transactions.',
          details: [],
        },
        timestamp: '2026-01-01T00:00:00Z',
      },
      { status: 400, statusText: 'Bad Request' }
    );
  });
});
