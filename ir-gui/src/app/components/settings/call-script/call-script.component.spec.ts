import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CallScriptComponent } from './call-script.component';

describe('CallScriptComponent', () => {
  let component: CallScriptComponent;
  let fixture: ComponentFixture<CallScriptComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CallScriptComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CallScriptComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
