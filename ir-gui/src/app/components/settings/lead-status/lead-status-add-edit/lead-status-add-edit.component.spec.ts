import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LeadStatusAddEditComponent } from './lead-status-add-edit.component';

describe('LeadStatusAddEditComponent', () => {
  let component: LeadStatusAddEditComponent;
  let fixture: ComponentFixture<LeadStatusAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [LeadStatusAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LeadStatusAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
