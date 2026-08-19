import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MvnoInvoiceComponent } from './mvno-invoice.component';

describe('MvnoInvoiceComponent', () => {
  let component: MvnoInvoiceComponent;
  let fixture: ComponentFixture<MvnoInvoiceComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MvnoInvoiceComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MvnoInvoiceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
