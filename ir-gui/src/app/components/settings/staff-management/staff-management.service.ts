import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  API_GATEWAY_COMMON_MANAGEMENT,
  COMMON_BASE_URL,
} from 'src/app/core/RadiusUtils/RadiusConstants';

@Injectable({
  providedIn: 'root',
})
export class StaffManagementService {
  baseURL: Readonly<string> = API_GATEWAY_COMMON_MANAGEMENT + '/staffuser';
  passwordBaseURL: Readonly<string> = API_GATEWAY_COMMON_MANAGEMENT + '/password';

  constructor(private http: HttpClient) {}

  commonGetMethod(url: string) {
    return this.http.get(API_GATEWAY_COMMON_MANAGEMENT + url);
  }

  getAllStaffList(data: any) {
    const url = `${this.baseURL}/list?product=BSS`;
    return this.http.post(url, data);
  }

  staffSearch(data: any) {
    const url = `${this.baseURL}/search`;
    return this.http.post(url, data);
  }

  staffReceiptSearch(receptNumber: any, prefix: any, data: any) {
    const url = `/staff/searchbyReciept?recieptNo=${receptNumber}&prefix=${prefix}`;
    return this.http.post(COMMON_BASE_URL + url, data);
  }

  postApiFromCMS(url: string, data: any) {
    return this.http.post(COMMON_BASE_URL + url, data);
  }

  
    generateOtp(username: string) {
    const url = `${this.passwordBaseURL}/generate-otp?username=${username}`;
    return this.http.post(url, {}, { responseType: 'text' });
  }

    validateOtp(data: { username: string; otp: string }) {
    const url = `${this.passwordBaseURL}/validate-otp`;
    return this.http.post(url, data, { responseType: 'text' });
  }

    resetPassword(data: { username: string; newPassword: string; confirmPassword: string }) {
    const url = `${this.passwordBaseURL}/reset`;
    return this.http.post(url, data, { responseType: 'text' });
  }

  
  changePassword(data: any) {
    return this.http.put(`${this.baseURL}/changepassword`, data);
  }

  deleteStaff(staffId: number) {
    const url = `${this.baseURL}/${staffId}`;
    return this.http.delete(url);
  }

  addNewReceipt(data: any) {
    return this.http.post(`${COMMON_BASE_URL}/staff/Reciept`, data);
  }

  getAllRoleDataForLoggedInUser() {
    const url = '/role/byLoggedInUser/?productType=BSS';
    return this.http.get(API_GATEWAY_COMMON_MANAGEMENT + url);
  }

  getTeamsData() {
    const url = '/teams/all';
    return this.http.get(API_GATEWAY_COMMON_MANAGEMENT + url);
  }

  getAllAgent() {
    const url = '/agent/all';
    return this.http.get(API_GATEWAY_COMMON_MANAGEMENT + url);
  }

  getserviceAreaListForCafCustomer() {
    const url = '/serviceArea/all/caf/customer';
    return this.http.get(API_GATEWAY_COMMON_MANAGEMENT + url);
  }

  getBusinessUnitFromStaff() {
    const url = '/businessUnit/getBUFromStaff';
    return this.http.get(API_GATEWAY_COMMON_MANAGEMENT + url);
  }

  getAllStaff() {
    const url = '/staffuser/allActive';
    return this.http.get(API_GATEWAY_COMMON_MANAGEMENT + url);
  }

  branchByServiceAreaID(data: any) {
    const url = '/branchManagement/getAllBranchesByServiceAreaId';
    return this.http.post(API_GATEWAY_COMMON_MANAGEMENT + url, data);
  }

  addStaff(data: any) {
    return this.http.post(this.baseURL, data);
  }

  updateStaff(data: any, staffId: number) {
    return this.http.put(`${this.baseURL}/${staffId}`, data);
  }

  uploadStaffProfileImage(path: string, data: any) {
    return this.http.post(API_GATEWAY_COMMON_MANAGEMENT + path, data);
  }

  getConfigurationByName(name: string) {
    return this.http.get(
      API_GATEWAY_COMMON_MANAGEMENT +
        `/system/configuration/getConfigurationByName?name=${name}`,
    );
  }

  getStaff(id: string) {
    return this.http.get(`${this.baseURL}/${id}`);
  }

  getStaffReceiptDataByStaffId(id: string) {
    return this.http.get(`${COMMON_BASE_URL}/staffReceipt/` + id);
  }

  getFromCMS(url: string) {
    return this.http.get(COMMON_BASE_URL + url);
  }

  withDrawAmount(path: string, data: any) {
    return this.http.post(API_GATEWAY_COMMON_MANAGEMENT + path, data);
  }
}