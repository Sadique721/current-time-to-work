import { TestBed } from '@angular/core/testing';

import { SipDevicesService } from './sip-devices.service';

describe('SipDevicesService', () => {
  let service: SipDevicesService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SipDevicesService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
