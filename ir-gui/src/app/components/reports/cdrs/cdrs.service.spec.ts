import { TestBed } from '@angular/core/testing';

import { CdrsService } from './cdrs.service';

describe('CdrsService', () => {
  let service: CdrsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CdrsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
