import { TestBed } from "@angular/core/testing";
import { ProductPlanManageService } from "./product-plan-manage-rating.service";

describe("ProductPlanManageService", () => {
  let service: ProductPlanManageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ProductPlanManageService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
