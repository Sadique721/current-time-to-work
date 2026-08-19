import { TestBed } from "@angular/core/testing";
import { SourceConfigurationManageService } from "./source-configuration-manage-rating.service";

describe("SourceConfigurationManageService", () => {
  let service: SourceConfigurationManageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SourceConfigurationManageService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
