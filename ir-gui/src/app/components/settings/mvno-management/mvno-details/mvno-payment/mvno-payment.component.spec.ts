import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MvnoPaymentComponent } from './mvno-payment.component';

describe('MvnoPaymentComponent', () => {
  let component: MvnoPaymentComponent;
  let fixture: ComponentFixture<MvnoPaymentComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MvnoPaymentComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MvnoPaymentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
