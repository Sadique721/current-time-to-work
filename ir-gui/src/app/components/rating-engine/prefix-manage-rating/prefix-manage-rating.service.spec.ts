import { TestBed } from "@angular/core/testing";
import { PrefixManageService } from "./prefix-manage-rating.service";

describe("PrefixManageService", () => {
  let service: PrefixManageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PrefixManageService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
