import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProductPlanManageRatingComponent } from './product-plan-manage-rating.component';

describe('ProductPlanManageRatingComponent', () => {
  let component: ProductPlanManageRatingComponent;
  let fixture: ComponentFixture<ProductPlanManageRatingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductPlanManageRatingComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProductPlanManageRatingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
