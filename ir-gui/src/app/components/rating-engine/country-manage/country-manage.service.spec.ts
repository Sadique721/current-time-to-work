import { TestBed } from "@angular/core/testing";
import { CountryManageService } from "./country-manage.service";

describe("CountryManageService", () => {
  let service: CountryManageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CountryManageService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });
});
