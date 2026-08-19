import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RatePackageDetailsComponent } from './rate-package-details.component';

describe('RatePackageDetailsComponent', () => {
  let component: RatePackageDetailsComponent;
  let fixture: ComponentFixture<RatePackageDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RatePackageDetailsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RatePackageDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
