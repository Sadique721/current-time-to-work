import { TestBed } from "@angular/core/testing";
import { RatePackageManageService } from "./rate-package-manage-rating.service";

describe("RatePackageManageService", () => {
  let service: RatePackageManageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(RatePackageManageService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
