import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MvnoDunningAuditComponent } from './mvno-dunning-audit.component';

describe('MvnoDunningAuditComponent', () => {
  let component: MvnoDunningAuditComponent;
  let fixture: ComponentFixture<MvnoDunningAuditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MvnoDunningAuditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MvnoDunningAuditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
