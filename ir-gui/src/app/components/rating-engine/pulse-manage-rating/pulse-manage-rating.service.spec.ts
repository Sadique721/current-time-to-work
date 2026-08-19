import { TestBed } from "@angular/core/testing";
import { PulseManageService } from "./pulse-manage-rating.service";

describe("PulseManageService", () => {
  let service: PulseManageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PulseManageService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
