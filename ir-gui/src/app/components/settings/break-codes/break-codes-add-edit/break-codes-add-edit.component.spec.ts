import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BreakCodesAddEditComponent } from './break-codes-add-edit.component';

describe('BreakCodesAddEditComponent', () => {
  let component: BreakCodesAddEditComponent;
  let fixture: ComponentFixture<BreakCodesAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [BreakCodesAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BreakCodesAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
