import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RatePackageAddEditComponent } from './rate-package-add-edit.component';

describe('RatePackageAddEditComponent', () => {
  let component: RatePackageAddEditComponent;
  let fixture: ComponentFixture<RatePackageAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RatePackageAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RatePackageAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
