import { HttpHeaders, HttpResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TransactionExportService } from './transaction-export.service';

describe('TransactionExportService', () => {
  let service: TransactionExportService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TransactionExportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('requests CSV as Blob response with filters and extracts Content-Disposition filename', () => {
    let filename = '';
    service.exportCsv({ type: 'EXPENSE', startDate: '2026-01-01', endDate: '2026-01-31' }).subscribe((result) => {
      filename = result.filename;
      expect(result.blob.type).toBe('text/csv');
    });

    const req = httpMock.expectOne(
      (request) =>
        request.url === 'http://localhost:8080/api/transactions/export/csv' &&
        request.params.get('type') === 'EXPENSE' &&
        request.responseType === 'blob'
    );
    req.flush(new Blob(['csv'], { type: 'text/csv' }), {
      headers: new HttpHeaders({
        'Content-Disposition': 'attachment; filename="owomi-transactions-2026-01.csv"',
      }),
    });

    expect(filename).toBe('owomi-transactions-2026-01.csv');
  });

  it('uses a safe fallback filename', () => {
    const response = new HttpResponse<Blob>({ body: new Blob(['csv']) });

    expect(service.extractFilename(response)).toBe('owomi-transactions.csv');
  });

  it('downloads without injecting HTML and revokes the object URL', () => {
    const createUrlSpy = spyOn(URL, 'createObjectURL').and.returnValue('blob:owomi');
    const revokeUrlSpy = spyOn(URL, 'revokeObjectURL');
    const clickSpy = jasmine.createSpy('click');
    const anchor = document.createElement('a');
    spyOn(document, 'createElement').and.returnValue(anchor);
    spyOn(anchor, 'click').and.callFake(clickSpy);

    service.downloadCsv().subscribe();
    const req = httpMock.expectOne('http://localhost:8080/api/transactions/export/csv');
    req.flush(new Blob(['csv'], { type: 'text/csv' }));

    expect(createUrlSpy).toHaveBeenCalled();
    expect(clickSpy).toHaveBeenCalled();
    expect(anchor.innerHTML).toBe('');
    expect(revokeUrlSpy).toHaveBeenCalledWith('blob:owomi');
  });
});
