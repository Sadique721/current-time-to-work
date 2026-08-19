import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { OCS_RATING } from "src/app/core/RadiusUtils/RadiusConstants";

@Injectable({
  providedIn: "root",
})
export class RatePackageManageService {
  baseURL: Readonly<string> = OCS_RATING + "/rate-packages";

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

  previewRateDetailsFile(formData: FormData) {
    return this.http.post(OCS_RATING + "/rate-details/preview", formData);
  }

  downloadSampleRateDetailsFile(url: string) {
    return this.http.get(OCS_RATING + url, {
      observe: "response",
      responseType: "blob",
    });
  }

  getZoneNames() {
    return this.http.get(OCS_RATING + "/zones/names");
  }

  getCurrencyCodes() {
    return this.http.get(OCS_RATING + "/countries/currencyCodes");
  }

  getPulseByServiceType(serviceType: string) {
    return this.http.get(`${OCS_RATING}/pulse/service-type?serviceType=${serviceType}`);
  }
}
