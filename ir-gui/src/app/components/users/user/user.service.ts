import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

export interface User {
  id: number;
  username: string;
  email: string;
  password?: string;
  defaultTimeout: number;
  userRole: string;
  userGroup: string;
  whatsappMessagingChannel?: string;
  smsMessagingChannel?: string;
  zohoUserId?: string;
  campaignOptions?: string;
  callRecording: boolean;
  status: boolean;
  defaultSipDevice?: string;
  campaign?: string;
  selected?: boolean;
}

export interface UserListResponse {
  data: User[];
  totalCount: number;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = '/api/users'; 

  constructor(private http: HttpClient) {}

    getAll(page: number, pageSize: number): Observable<UserListResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('pageSize', pageSize.toString());

    
    
    
    
    const mockData: User[] = [
      { 
        id: 1, 
        username: 'bharat', 
        email: 'Bharat.singh@unifyxcess.ai',
        defaultTimeout: 30,
        userRole: 'Agent',
        userGroup: 'auto outbounded',
        callRecording: true,
        status: true,
        defaultSipDevice: '1050',
        campaign: 'auto lead test'
      },
      { 
        id: 2, 
        username: 'Dev', 
        email: 'devshah2367@gmail.com',
        defaultTimeout: 30,
        userRole: 'Agent',
        userGroup: 'Default',
        callRecording: true,
        status: true,
        defaultSipDevice: '102',
        campaign: 'auto lead test'
      },
      { 
        id: 3, 
        username: 'agenttest', 
        email: 'agenttest@mail.com',
        defaultTimeout: 30,
        userRole: 'Agent',
        userGroup: 'Default',
        callRecording: true,
        status: true,
        campaign: 'Sale_IC'
      },
    ];

    return of({
      data: mockData,
      totalCount: mockData.length
    }).pipe(delay(300));
  }

    search(filters: any, page: number, pageSize: number): Observable<UserListResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('pageSize', pageSize.toString());

    
    
    
    
    const mockData: User[] = [
      { 
        id: 1, 
        username: 'bharat', 
        email: 'Bharat.singh@unifyxcess.ai',
        defaultTimeout: 30,
        userRole: 'Agent',
        userGroup: 'auto outbounded',
        callRecording: true,
        status: true,
        defaultSipDevice: '1050',
        campaign: 'auto lead test'
      },
    ];

    return of({
      data: mockData,
      totalCount: mockData.length
    }).pipe(delay(300));
  }

    getById(id: number): Observable<User> {
    
    
    
    
    const mockUser: User = {
      id: id,
      username: 'bharat',
      email: 'Bharat.singh@unifyxcess.ai',
      defaultTimeout: 30,
      userRole: 'Agent',
      userGroup: 'auto outbounded',
      whatsappMessagingChannel: 'channel1',
      smsMessagingChannel: 'sms1',
      zohoUserId: 'zoho123',
      campaignOptions: 'auto lead test',
      callRecording: true,
      status: true,
    };

    return of(mockUser).pipe(delay(300));
  }

    create(user: Partial<User>): Observable<User> {
    
    
    
    
    const newUser: User = {
      ...(user as User),
      id: Math.floor(Math.random() * 1000)
    };

    return of(newUser).pipe(delay(300));
  }

    update(id: number, user: Partial<User>): Observable<User> {
    
    
    
    
    const updatedUser: User = {
      ...(user as User),
      id: id
    };

    return of(updatedUser).pipe(delay(300));
  }

    delete(id: number): Observable<void> {
    
    
    
    
    return of(void 0).pipe(delay(300));
  }

    bulkDelete(ids: number[]): Observable<void> {
    
    
    
    
    return of(void 0).pipe(delay(300));
  }

    usernameExists(username: string, excludeId?: number): Observable<boolean> {
    
    return of(false).pipe(delay(200));
  }

    emailExists(email: string, excludeId?: number): Observable<boolean> {
    
    return of(false).pipe(delay(200));
  }
}