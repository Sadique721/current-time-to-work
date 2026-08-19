import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { BranchManagementService } from "src/app/components/master-management/branch-management/branch-management.service";
import { CountryManagementService } from "src/app/components/master-management/country-management/country-management.service";
import { ServiceAreaService } from "src/app/components/master-management/service-area/service-area.service";

@Injectable({
  providedIn: "root",
})
export class MasterManagementService {
  constructor(
    private countryService: CountryManagementService,
    private serviceAreaService: ServiceAreaService,
    private branchService: BranchManagementService
  ) {}

  getServiceAreaById(serviceAreaId: number): Observable<any> {
    return this.serviceAreaService.getMethod(`serviceArea/${serviceAreaId}`);
  }

  getAllActiveServiceAreas(): Observable<any> {
    return this.serviceAreaService.postMethod("list", {
      status: "Active",
    });
  }

  getAllActiveLocation(): Observable<any> {
    return this.serviceAreaService.getAllActiveLocation();
  }

  getBranchesByServiceAreaIds(serviceAreaIds: number[]): Observable<any> {
    return this.branchService.postMethod("search", {
      serviceAreaIds: serviceAreaIds,
    });
  }

  getAllActiveBranches(): Observable<any> {
    return this.branchService.postMethod("list", {
      status: "Active",
    });
  }

  getBranchById(branchId: number): Observable<any> {
    return this.branchService.getMethod(`/branchManagement/${branchId}`);
  }

  getAreaByPincodeId(pincodeId: number): Observable<any> {
    return this.serviceAreaService.getMethod(`pincode/${pincodeId}/areas`);
  }

  getAreaDetailsById(areaId: number): Observable<any> {
    return this.serviceAreaService.getMethod(`area/${areaId}`);
  }

  getAllCountries(): Observable<any> {
    return this.countryService.getMethod();
  }

  searchCountries(searchData: any): Observable<any> {
    return this.countryService.postMethod("search", searchData);
  }

  getCountryById(countryId: number): Observable<any> {
    return this.countryService.postMethod("search", {
      id: countryId,
    });
  }

  getLocationHierarchy(): Observable<any> {
    return this.serviceAreaService.getAllActiveLocation();
  }
}
