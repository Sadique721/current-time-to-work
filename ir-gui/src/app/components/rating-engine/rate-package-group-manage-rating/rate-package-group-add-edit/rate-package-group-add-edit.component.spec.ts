import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RatePackageGroupAddEditComponent } from './rate-package-group-add-edit.component';

describe('RatePackageGroupAddEditComponent', () => {
  let component: RatePackageGroupAddEditComponent;
  let fixture: ComponentFixture<RatePackageGroupAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RatePackageGroupAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RatePackageGroupAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
