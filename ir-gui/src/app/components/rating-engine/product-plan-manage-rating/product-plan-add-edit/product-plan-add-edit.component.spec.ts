import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProductPlanAddEditComponent } from './product-plan-add-edit.component';

describe('ProductPlanAddEditComponent', () => {
  let component: ProductPlanAddEditComponent;
  let fixture: ComponentFixture<ProductPlanAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductPlanAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProductPlanAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
