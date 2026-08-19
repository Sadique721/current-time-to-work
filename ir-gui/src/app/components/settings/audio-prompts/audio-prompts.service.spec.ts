import { TestBed } from '@angular/core/testing';

import { AudioPromptsService } from './audio-prompts.service';

describe('AudioPromptsService', () => {
  let service: AudioPromptsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AudioPromptsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
