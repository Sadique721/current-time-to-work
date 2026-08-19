import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { API_GATEWAY_COMMON_MANAGEMENT } from 'src/app/core/RadiusUtils/RadiusConstants';
import { IRole } from './role-management-interface';

@Injectable({
  providedIn: 'root',
})
export class RoleManagementService {
  baseURL: Readonly<string> = API_GATEWAY_COMMON_MANAGEMENT + '/role';

  constructor(private http: HttpClient) {}

  getMethod() {
    return this.http.get(`${this.baseURL}`);
  }

  postMethod(url: 'searchRoleByProduct' | 'permissions', data: any) {
    return this.http.post(
      `${this.baseURL}/${url}?productType=BSS&isloggedInUser=true`,
      data,
    );
  }

  deleteMethod(id: number) {
    return this.http.delete(`${this.baseURL}/delete/${id}`);
  }

  getAllACLMenu() {
    return this.http.get(
      `${API_GATEWAY_COMMON_MANAGEMENT}/acl/getCommonAclMenu/BSS`,
    );
  }

  addRole(data: IRole) {
    return this.http.post(`${this.baseURL}/saveRole`, data);
  }

  updateRole(data: IRole) {
    return this.http.put(`${this.baseURL}/updateRole`, data);
  }

  getRoleById(id: number) {
    return this.http.get(`${this.baseURL}/product/${id}?productName=BSS`);
  }

  getAllRole(): any {
    return this.http.get(`${this.baseURL}/all`);
  }
}
