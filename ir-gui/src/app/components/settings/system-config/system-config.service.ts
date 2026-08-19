import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { API_GATEWAY_COMMON_MANAGEMENT } from 'src/app/core/RadiusUtils/RadiusConstants';

@Injectable({
  providedIn: 'root',
})
export class SystemConfigService {
  constructor(private http: HttpClient) {}

  getMethod(url: string) {
    return this.http.get(API_GATEWAY_COMMON_MANAGEMENT + url);
  }

  postMethod(url: string, data: any) {
    return this.http.post(API_GATEWAY_COMMON_MANAGEMENT + url, data);
  }

  deleteMethod(url: string) {
    return this.http.delete(API_GATEWAY_COMMON_MANAGEMENT + url);
  }

  updateMethod(url: string, data: any) {
    return this.http.put(API_GATEWAY_COMMON_MANAGEMENT + url, data);
  }

  searchTax(url: string, data: any) {
    return this.http.post(API_GATEWAY_COMMON_MANAGEMENT + url, data);
  }

  getConfigurationByName(name: any) {
    return this.http.get(
      API_GATEWAY_COMMON_MANAGEMENT +
        `/system/configuration/getConfigurationByName?name=${name}`
    );
  }
}
