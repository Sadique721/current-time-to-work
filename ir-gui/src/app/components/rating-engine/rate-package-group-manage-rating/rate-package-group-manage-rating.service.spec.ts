import { TestBed } from "@angular/core/testing";
import { RatePackageGroupManageService } from "./rate-package-group-manage-rating.service";

describe("RatePackageGroupManageService", () => {
  let service: RatePackageGroupManageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(RatePackageGroupManageService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
