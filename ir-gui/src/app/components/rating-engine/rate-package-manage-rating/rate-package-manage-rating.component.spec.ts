import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RatePackageManageRatingComponent } from './rate-package-manage-rating.component';

describe('RatePackageManageRatingComponent', () => {
  let component: RatePackageManageRatingComponent;
  let fixture: ComponentFixture<RatePackageManageRatingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RatePackageManageRatingComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RatePackageManageRatingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
