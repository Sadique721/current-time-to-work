import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BreakCodesComponent } from './break-codes.component';

describe('BreakCodesComponent', () => {
  let component: BreakCodesComponent;
  let fixture: ComponentFixture<BreakCodesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [BreakCodesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BreakCodesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
