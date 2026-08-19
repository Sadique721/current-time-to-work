import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StaffAddEditComponent } from './staff-add-edit.component';

describe('StaffAddEditComponent', () => {
  let component: StaffAddEditComponent;
  let fixture: ComponentFixture<StaffAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StaffAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StaffAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
