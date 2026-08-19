import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

export interface UserRole {
  id: number;
  name: string;
  status: boolean;
  pbxMode: boolean;
  cdrReport: boolean;
  loginLogoutReport: boolean;
  callCenterMode: boolean;
  recording: boolean;
  followUp: boolean;
  stickyAgent: boolean;
  numberMasking: boolean;
  setting: boolean;
  breakcode: boolean;
  allowBlackList: boolean;
  whatsapp: boolean;
  selected?: boolean;
}

export interface UserRoleListResponse {
  data: UserRole[];
  totalCount: number;
}

@Injectable({
  providedIn: 'root'
})
export class UserRoleService {
  private apiUrl = '/api/user-roles';

  constructor(private http: HttpClient) {}
  getAll(page: number, pageSize: number): Observable<UserRoleListResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('pageSize', pageSize.toString());
    const mockData: UserRole[] = [
      { 
        id: 1, 
        name: 'test role for agent',
        status: true,
        pbxMode: false,
        cdrReport: false,
        loginLogoutReport: false,
        callCenterMode: false,
        recording: false,
        followUp: false,
        stickyAgent: false,
        numberMasking: false,
        setting: false,
        breakcode: false,
        allowBlackList: false,
        whatsapp: false,
        selected: false
      },
      { 
        id: 2, 
        name: 'Default',
        status: true,
        pbxMode: true,
        cdrReport: true,
        loginLogoutReport: true,
        callCenterMode: true,
        recording: true,
        followUp: true,
        stickyAgent: false,
        numberMasking: true,
        setting: true,
        breakcode: true,
        allowBlackList: false,
        whatsapp: true,
        selected: false
      },
      { 
        id: 3, 
        name: 'Admin',
        status: true,
        pbxMode: true,
        cdrReport: true,
        loginLogoutReport: true,
        callCenterMode: true,
        recording: true,
        followUp: true,
        stickyAgent: true,
        numberMasking: true,
        setting: true,
        breakcode: true,
        allowBlackList: true,
        whatsapp: true,
        selected: false
      },
    ];

    return of({
      data: mockData,
      totalCount: mockData.length
    }).pipe(delay(300));
  }

  search(filters: any, page: number, pageSize: number): Observable<UserRoleListResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('pageSize', pageSize.toString());
    const mockData: UserRole[] = [
      { 
        id: 1, 
        name: 'test role for agent',
        status: true,
        pbxMode: false,
        cdrReport: false,
        loginLogoutReport: false,
        callCenterMode: false,
        recording: false,
        followUp: false,
        stickyAgent: false,
        numberMasking: false,
        setting: false,
        breakcode: false,
        allowBlackList: false,
        whatsapp: false,
        selected: false
      },
    ];

    return of({
      data: mockData,
      totalCount: mockData.length
    }).pipe(delay(300));
  }

  getById(id: number): Observable<UserRole> {
    const mockUserRole: UserRole = {
      id: id,
      name: 'test role for agent',
      status: true,
      pbxMode: false,
      cdrReport: false,
      loginLogoutReport: false,
      callCenterMode: false,
      recording: false,
      followUp: false,
      stickyAgent: false,
      numberMasking: false,
      setting: false,
      breakcode: false,
      allowBlackList: false,
      whatsapp: false,
    };

    return of(mockUserRole).pipe(delay(300));
  }

  create(userRole: Partial<UserRole>): Observable<UserRole> {
    const newUserRole: UserRole = {
      ...(userRole as UserRole),
      id: Math.floor(Math.random() * 1000)
    };

    return of(newUserRole).pipe(delay(300));
  }

  update(id: number, userRole: Partial<UserRole>): Observable<UserRole> {
    const updatedUserRole: UserRole = {
      ...(userRole as UserRole),
      id: id
    };

    return of(updatedUserRole).pipe(delay(300));
  }

  delete(id: number): Observable<void> {
    return of(void 0).pipe(delay(300));
  }

  bulkDelete(ids: number[]): Observable<void> {
    return of(void 0).pipe(delay(300));
  }

  nameExists(name: string, excludeId?: number): Observable<boolean> {
    return of(false).pipe(delay(200));
  }
}