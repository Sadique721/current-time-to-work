import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MvnoLedgerComponent } from './mvno-ledger.component';

describe('MvnoLedgerComponent', () => {
  let component: MvnoLedgerComponent;
  let fixture: ComponentFixture<MvnoLedgerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MvnoLedgerComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MvnoLedgerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
