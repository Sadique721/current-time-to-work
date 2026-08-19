import { TestBed } from '@angular/core/testing';

import { BreakCodesService } from './break-codes.service';

describe('BreakCodesService', () => {
  let service: BreakCodesService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(BreakCodesService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
