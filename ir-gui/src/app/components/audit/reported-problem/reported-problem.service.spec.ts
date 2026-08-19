import { TestBed } from '@angular/core/testing';

import { ReportedProblemService } from './reported-problem.service';

describe('ReportedProblemService', () => {
  let service: ReportedProblemService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ReportedProblemService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
