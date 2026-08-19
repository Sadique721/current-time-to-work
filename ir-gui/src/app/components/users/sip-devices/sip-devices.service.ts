import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { CALL_CENTER_URL } from 'src/app/core/RadiusUtils/RadiusConstants';

export interface SipDevice {
  id: number;
  deviceName: string;
  username: string;
  password: string;
  callerIdName: string;
  callerIdNumber: string;
  userId: number;
  status: string; 
  extensionStatus: number; 
  recording: number; 
  dnd: number; 
  isAssigned: number; 
  mailTo: string | null;
  callForward: any | null;
  voicemail: any | null;
  followMe: any | null;
  speedDial: any | null;
  selected?: boolean;
}

export interface SipDeviceListResponse {
  data: SipDevice[];
  totalCount: number;
  pagination?: {
    totalRecords: number;
    page: number;
    totalPages: number;
    limit: number;
  };
}

export interface ApiResponse<T> {
  statusCode: number;
  success: boolean;
  message: string;
  data: T;
  pagination?: {
    totalRecords: number;
    page: number;
    totalPages: number;
    limit: number;
  };
}

@Injectable({
  providedIn: 'root'
})
export class SipDevicesService {
  private readonly apiUrl = CALL_CENTER_URL+'/extensions';

  constructor(private http: HttpClient) {}
getAll(page: number, pageSize: number): Observable<SipDeviceListResponse> {
  const params = new HttpParams()
    .set('page', (page - 1).toString()) 
    .set('limit', pageSize.toString());

  return this.http.get<ApiResponse<SipDevice[]>>(`${this.apiUrl}`, { params }).pipe(
    map(response => ({
      data: response.data || [],
      totalCount: response.pagination?.totalRecords || 0,
      pagination: response.pagination
    })),
    catchError(error => {
            return of({ data: [], totalCount: 0 });
    })
  );
}

search(keyword: string, page: number, pageSize: number): Observable<SipDeviceListResponse> {
  const params = new HttpParams()
    .set('keyword', keyword)
    .set('page', (page - 1).toString()) 
    .set('limit', pageSize.toString());

  return this.http.get<ApiResponse<SipDevice[]>>(`${this.apiUrl}/search`, { params }).pipe(
    map(response => ({
      data: response.data || [],
      totalCount: response.pagination?.totalRecords || 0,
      pagination: response.pagination
    })),
    catchError(error => {
            return of({ data: [], totalCount: 0 });
    })
  );
}

    getById(id: number): Observable<SipDevice> {
    return this.http.get<ApiResponse<SipDevice>>(`${this.apiUrl}/${id}`).pipe(
      map(response => response.data),
      catchError(error => {
                throw error;
      })
    );
  }

    create(device: Partial<SipDevice>): Observable<SipDevice> {
    const payload = this.mapToApiPayload(device);
    
    return this.http.post<ApiResponse<SipDevice>>(this.apiUrl, payload).pipe(
      map(response => response.data),
      catchError(error => {
                throw error;
      })
    );
  }

    update(id: number, device: Partial<SipDevice>): Observable<SipDevice> {
    const payload = this.mapToApiPayload(device);
    
    return this.http.put<ApiResponse<SipDevice>>(`${this.apiUrl}/${id}`, payload).pipe(
      map(response => response.data),
      catchError(error => {
                throw error;
      })
    );
  }

    delete(id: number): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`).pipe(
      map(() => void 0),
      catchError(error => {
                throw error;
      })
    );
  }

    bulkDelete(ids: number[]): Observable<void> {
    
    const deletePromises = ids.map(id => 
      this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`).toPromise()
    );

    return new Observable(observer => {
      Promise.all(deletePromises)
        .then(() => {
          observer.next();
          observer.complete();
        })
        .catch(error => {
          observer.error(error);
        });
    });
  }

    getActiveSipDevices(): Observable<Array<{ label: string; value: string }>> {
    return this.http.get<ApiResponse<SipDevice[]>>(`${this.apiUrl}/active`).pipe(
      map(response => {
        const devices = response.data || [];
        return devices.map(device => ({
          label: device.deviceName,
          value: device.id.toString()
        }));
      }),
      catchError(error => {
                return of([]);
      })
    );
  }

    private mapToApiPayload(device: Partial<SipDevice>): any {
    return {
      deviceName: device.deviceName,
      username: device.username,
      password: device.password,
      userId: device.userId,
      status: device.status || "0",
      extensionStatus: device.extensionStatus ?? 1,
      recording: device.recording ?? 0,
      dnd: device.dnd ?? 0,
      isAssigned: device.isAssigned ?? 0,
      callerIdName: device.callerIdName || null,
      callerIdNumber: device.callerIdNumber || null,
      mailTo: device.mailTo || null,
      callForward: device.callForward || null,
      voicemail: device.voicemail || null,
      followMe: device.followMe || null,
      speedDial: device.speedDial || null
    };
  }

    getUsers(): Observable<Array<{ label: string; value: number }>> {
    
    const mockUsers = [
      { label: 'Bharat', value: 25 },
      { label: 'Agent Test', value: 26 },
      { label: 'Tester 16', value: 27 },
    ];

    return of(mockUsers);
  }

    getCallerIdNumbers(): Observable<Array<{ label: string; value: string }>> {
    
    const mockNumbers = [
      { label: '101', value: '101' },
      { label: '102', value: '102' },
      { label: '103', value: '103' },
    ];

    return of(mockNumbers);
  }

    getSipDevicesForRouting(): Observable<Array<{ label: string; value: string }>> {
    
    return this.getAll(1, 100).pipe(
      map(response => 
        response.data.map(device => ({
          label: device.deviceName,
          value: device.id.toString()
        }))
      )
    );
  }

    generatePassword(length: number = 12): string {
    const charset = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*';
    let password = '';
    for (let i = 0; i < length; i++) {
      password += charset.charAt(Math.floor(Math.random() * charset.length));
    }
    return password;
  }
}