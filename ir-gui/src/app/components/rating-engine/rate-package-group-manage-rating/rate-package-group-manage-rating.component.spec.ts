import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RatePackageGroupManageRatingComponent } from './rate-package-group-manage-rating.component';

describe('RatePackageGroupManageRatingComponent', () => {
  let component: RatePackageGroupManageRatingComponent;
  let fixture: ComponentFixture<RatePackageGroupManageRatingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RatePackageGroupManageRatingComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RatePackageGroupManageRatingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
