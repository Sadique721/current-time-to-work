import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import {
  API_GATEWAY_COMMON_MANAGEMENT,
  OCS_RATING,
} from "src/app/core/RadiusUtils/RadiusConstants";

@Injectable({
  providedIn: "root",
})
export class RatePackageGroupManageService {
  baseURL: Readonly<string> = OCS_RATING + "/rate-package-groups";

  constructor(private http: HttpClient) {}

  getMethod() {
    return this.http.get(`${this.baseURL}`);
  }

  putMethod(prefixId: number, data: any) {
    return this.http.put(`${this.baseURL}/${prefixId}`, data);
  }
  postMethod(data: any) {
    return this.http.post(`${this.baseURL}`, data);
  }

  deleteMethod(prefixId: number) {
    return this.http.delete(`${this.baseURL}/${prefixId}`);
  }

  updateMethod(id: number, data: any) {
    return this.http.put(`${this.baseURL}/${id}`, data);
  }

  
  postPaginated(data: any) {
    return this.http.post(`${this.baseURL}/paginated`, data);
  }

  
  getById(id: number) {
    return this.http.get(`${this.baseURL}/${id}`);
  }
  getPulse(url: string) {
    return this.http.get(OCS_RATING + url);
  }
  addRatePackages(url: string, data: any) {
    return this.http.post(OCS_RATING + url, data);
  }
  updateRatePackages(url: string, data: any) {
    return this.http.put(OCS_RATING + url, data);
  }
  addRateDetailsPackages(url: string, data: any) {
    return this.http.post(OCS_RATING + url, data);
  }
  uploadRateDetailsFile(url: string, formdata: any) {
    return this.http.post(OCS_RATING + url, formdata);
  }
  downloadSampleRateDetailsFile(url: string) {
    return this.http.get(OCS_RATING + url);
  }

  
  getRatePackagesList() {
    return this.http.get(OCS_RATING + "/rate-packages");
  }

  getRatePackagesNamesByType(type: string, lineOfBusiness: string, serviceType: string) {
    return this.http.get(OCS_RATING + `/rate-packages/names?type=${type}&lineOfBusiness=${lineOfBusiness}&serviceType=${serviceType}`);
  }

  
  addRatePackagesGroup(url: string, data: any) {
    return this.http.post(OCS_RATING + url, data);
  }
  updateRatePackagesGroup(url: string, data: any) {
    return this.http.put(OCS_RATING + url, data);
  }

  updatePackagePriorities(groupId: number, packages: { ratePackageId: number; priority: number; isFallback: boolean }[]) {
    return this.http.put(`${this.baseURL}/${groupId}/packages`, { packages });
  }
}
