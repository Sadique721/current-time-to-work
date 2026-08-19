import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PartnerDetailsComponent } from './partner-details.component';

describe('PartnerDetailsComponent', () => {
  let component: PartnerDetailsComponent;
  let fixture: ComponentFixture<PartnerDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [PartnerDetailsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PartnerDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
