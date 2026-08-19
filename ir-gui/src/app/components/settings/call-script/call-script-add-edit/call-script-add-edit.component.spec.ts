import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CallScriptAddEditComponent } from './call-script-add-edit.component';

describe('CallScriptAddEditComponent', () => {
  let component: CallScriptAddEditComponent;
  let fixture: ComponentFixture<CallScriptAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CallScriptAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CallScriptAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
