import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MvnoPaymentAddComponent } from './mvno-payment-add.component';

describe('MvnoPaymentAddComponent', () => {
  let component: MvnoPaymentAddComponent;
  let fixture: ComponentFixture<MvnoPaymentAddComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MvnoPaymentAddComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MvnoPaymentAddComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
