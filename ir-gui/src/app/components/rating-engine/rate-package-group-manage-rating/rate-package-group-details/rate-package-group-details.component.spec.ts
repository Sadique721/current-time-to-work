import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RatePackageGroupDetailsComponent } from './rate-package-group-details.component';

describe('RatePackageGroupDetailsComponent', () => {
  let component: RatePackageGroupDetailsComponent;
  let fixture: ComponentFixture<RatePackageGroupDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RatePackageGroupDetailsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RatePackageGroupDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
