import { TestBed } from "@angular/core/testing";
import { PartnerManageService } from "./partner-manage-rating.service";

describe("PartnerManageService", () => {
  let service: PartnerManageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PartnerManageService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
