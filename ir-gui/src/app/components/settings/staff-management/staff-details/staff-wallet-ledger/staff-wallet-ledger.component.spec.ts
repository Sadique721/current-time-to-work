import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StaffWalletLedgerComponent } from './staff-wallet-ledger.component';

describe('StaffWalletLedgerComponent', () => {
  let component: StaffWalletLedgerComponent;
  let fixture: ComponentFixture<StaffWalletLedgerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StaffWalletLedgerComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(StaffWalletLedgerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
