import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AudioPromptsComponent } from './audio-prompts.component';

describe('AudioPromptsComponent', () => {
  let component: AudioPromptsComponent;
  let fixture: ComponentFixture<AudioPromptsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AudioPromptsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AudioPromptsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
