import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PartnerRatingAddEditComponent } from './partner-rating-add-edit.component';

describe('PartnerRatingAddEditComponent', () => {
  let component: PartnerRatingAddEditComponent;
  let fixture: ComponentFixture<PartnerRatingAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PartnerRatingAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PartnerRatingAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
