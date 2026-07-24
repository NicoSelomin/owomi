import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DashboardService } from './dashboard.service';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('calls summary with optional dates', () => {
    service.getSummary({ startDate: '2026-01-01', endDate: '2026-01-31' }).subscribe();

    const req = httpMock.expectOne(
      (request) =>
        request.url === 'http://localhost:8080/api/dashboard/summary' &&
        request.params.get('startDate') === '2026-01-01' &&
        request.params.get('endDate') === '2026-01-31'
    );
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: {}, timestamp: '2026-01-01T00:00:00Z' });
  });

  it('calls monthly balances and category expenses', () => {
    service.getMonthlyBalances().subscribe();
    const monthlyReq = httpMock.expectOne('http://localhost:8080/api/dashboard/monthly-balances');
    expect(monthlyReq.request.method).toBe('GET');
    monthlyReq.flush({ success: true, data: [], timestamp: '2026-01-01T00:00:00Z' });

    service.getCategoryExpenses().subscribe();
    const categoryReq = httpMock.expectOne('http://localhost:8080/api/dashboard/category-expenses');
    expect(categoryReq.request.method).toBe('GET');
    categoryReq.flush({ success: true, data: [], timestamp: '2026-01-01T00:00:00Z' });
  });

  it('does not send empty optional date filters', () => {
    service.getSummary({ startDate: undefined, endDate: undefined }).subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/dashboard/summary');

    expect(req.request.params.keys()).toEqual([]);
    req.flush({
      success: true,
      data: {
        incomeTotal: 0,
        expenseTotal: 0,
        balance: 0,
        transactionCount: 0,
        startDate: '2026-07-01',
        endDate: '2026-07-31',
      },
      timestamp: '2026-01-01T00:00:00Z',
    });
  });
});
