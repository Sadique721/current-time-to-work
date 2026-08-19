import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LeadStatusComponent } from './lead-status.component';

describe('LeadStatusComponent', () => {
  let component: LeadStatusComponent;
  let fixture: ComponentFixture<LeadStatusComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [LeadStatusComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LeadStatusComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
