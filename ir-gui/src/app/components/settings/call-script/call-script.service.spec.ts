import { TestBed } from '@angular/core/testing';

import { CallScriptService } from './call-script.service';

describe('CallScriptService', () => {
  let service: CallScriptService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CallScriptService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
