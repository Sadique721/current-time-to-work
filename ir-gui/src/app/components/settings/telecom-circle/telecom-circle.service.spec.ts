import { TestBed } from '@angular/core/testing';

import { TelecomCircleService } from './telecom-circle.service';

describe('TelecomCircleService', () => {
  let service: TelecomCircleService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TelecomCircleService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
