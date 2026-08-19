import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PartnerManageRatingComponent } from './partner-manage-rating.component';

describe('PartnerManageRatingComponent', () => {
  let component: PartnerManageRatingComponent;
  let fixture: ComponentFixture<PartnerManageRatingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PartnerManageRatingComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PartnerManageRatingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
