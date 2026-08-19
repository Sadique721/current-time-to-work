import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AudioPromptsAddEditComponent } from './audio-prompts-add-edit.component';

describe('AudioPromptsAddEditComponent', () => {
  let component: AudioPromptsAddEditComponent;
  let fixture: ComponentFixture<AudioPromptsAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AudioPromptsAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AudioPromptsAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
