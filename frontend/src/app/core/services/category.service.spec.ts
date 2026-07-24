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

  it('supports create, update and delete', () => {
    const body = { name: 'Transport', icon: 'car', color: '#123456', type: 'EXPENSE' as const };

    service.create(body).subscribe();
    const createReq = httpMock.expectOne('http://localhost:8080/api/categories');
    expect(createReq.request.method).toBe('POST');
    createReq.flush({ success: true, data: {}, timestamp: '2026-01-01T00:00:00Z' });

    service.update(10, body).subscribe();
    const updateReq = httpMock.expectOne('http://localhost:8080/api/categories/10');
    expect(updateReq.request.method).toBe('PUT');
    updateReq.flush({ success: true, data: {}, timestamp: '2026-01-01T00:00:00Z' });

    service.delete(10).subscribe();
    const deleteReq = httpMock.expectOne('http://localhost:8080/api/categories/10');
    expect(deleteReq.request.method).toBe('DELETE');
    deleteReq.flush({ success: true, data: null, timestamp: '2026-01-01T00:00:00Z' });
  });
});
