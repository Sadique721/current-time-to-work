import { TestBed } from '@angular/core/testing';

import { EmailSmtpService } from './email-smtp.service';

describe('EmailSmtpService', () => {
  let service: EmailSmtpService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EmailSmtpService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
