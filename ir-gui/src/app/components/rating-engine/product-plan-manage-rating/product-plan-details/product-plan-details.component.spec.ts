import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProductPlanDetailsComponent } from './product-plan-details.component';

describe('ProductPlanDetailsComponent', () => {
  let component: ProductPlanDetailsComponent;
  let fixture: ComponentFixture<ProductPlanDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductPlanDetailsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProductPlanDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
