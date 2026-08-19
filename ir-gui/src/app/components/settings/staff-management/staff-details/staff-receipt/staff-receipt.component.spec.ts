import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StaffReceiptComponent } from './staff-receipt.component';

describe('StaffReceiptComponent', () => {
  let component: StaffReceiptComponent;
  let fixture: ComponentFixture<StaffReceiptComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StaffReceiptComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StaffReceiptComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
