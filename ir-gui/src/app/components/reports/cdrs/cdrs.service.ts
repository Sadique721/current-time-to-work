import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

export interface CDRS {
  id: number;
  date: Date;
  callerIdNumber: string;
  phoneNumber: string;
  user?: string;
  duration: string;
  callMode: string;
  disposition?: string;
  hangupCause: string;
  direction: string;
  ivrGroup?: string;
  callType: string;
}

export interface CDRSListResponse {
  data: CDRS[];
  totalCount: number;
}

export interface CDRSFilters {
  cdrsArchive?: string;
  startDate?: string;
  endDate?: string;
  callerIdName?: string;
  callerIdNameCondition?: string;
  callerIdNumber?: string;
  callerIdNumberCondition?: string;
  phoneNumber?: string;
  phoneNumberCondition?: string;
  user?: string;
  ivrGroup?: string;
  duration?: string;
  durationCondition?: string;
  callMode?: string;
  hangupCause?: string;
  direction?: string;
  callType?: string;
}

@Injectable({
  providedIn: 'root'
})
export class CdrsService {
  private apiUrl = '/api/cdrs'; 

  constructor(private http: HttpClient) {}

    getAll(page: number, pageSize: number): Observable<CDRSListResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('pageSize', pageSize.toString());

    
    
    
    
    const mockData: CDRS[] = [
      { 
        id: 1, 
        date: new Date('2026-01-19 11:29:48'),
        callerIdNumber: '+919870046880',
        phoneNumber: '912717424450',
        user: undefined,
        duration: '00:05:15',
        callMode: 'PBX',
        disposition: undefined,
        hangupCause: 'CALLQUEUE_TIMEOUT',
        direction: 'Inbound',
        ivrGroup: undefined,
        callType: 'DID'
      },
      { 
        id: 2, 
        date: new Date('2026-01-19 10:15:32'),
        callerIdNumber: '+919876543210',
        phoneNumber: '912345678901',
        user: 'User1',
        duration: '00:03:45',
        callMode: 'Dialer',
        disposition: 'Sale',
        hangupCause: 'NORMAL_CLEARING',
        direction: 'Outbound',
        ivrGroup: 'IVR1',
        callType: 'External'
      },
      { 
        id: 3, 
        date: new Date('2026-01-19 09:45:12'),
        callerIdNumber: '+919123456789',
        phoneNumber: '919876543210',
        user: 'Admin',
        duration: '00:12:30',
        callMode: 'PBX',
        disposition: 'Callback',
        hangupCause: 'NORMAL_CLEARING',
        direction: 'Inbound',
        ivrGroup: 'IVR2',
        callType: 'DID'
      },
      { 
        id: 4, 
        date: new Date('2026-01-19 08:20:55'),
        callerIdNumber: '+918765432109',
        phoneNumber: '913456789012',
        user: undefined,
        duration: '00:00:18',
        callMode: 'Manual',
        disposition: undefined,
        hangupCause: 'NO_ANSWER',
        direction: 'Outbound',
        ivrGroup: undefined,
        callType: 'Extension'
      },
      { 
        id: 5, 
        date: new Date('2026-01-19 07:55:30'),
        callerIdNumber: '+917654321098',
        phoneNumber: '914567890123',
        user: 'User2',
        duration: '00:08:42',
        callMode: 'PBX',
        disposition: 'Not Interested',
        hangupCause: 'NORMAL_CLEARING',
        direction: 'Inbound',
        ivrGroup: 'IVR1',
        callType: 'DID'
      },
      { 
        id: 6, 
        date: new Date('2026-01-18 23:10:15'),
        callerIdNumber: '+916543210987',
        phoneNumber: '915678901234',
        user: undefined,
        duration: '00:01:05',
        callMode: 'Dialer',
        disposition: undefined,
        hangupCause: 'USER_BUSY',
        direction: 'Outbound',
        ivrGroup: undefined,
        callType: 'External'
      },
      { 
        id: 7, 
        date: new Date('2026-01-18 22:30:48'),
        callerIdNumber: '+915432109876',
        phoneNumber: '916789012345',
        user: 'User1',
        duration: '00:15:20',
        callMode: 'PBX',
        disposition: 'Sale',
        hangupCause: 'NORMAL_CLEARING',
        direction: 'Inbound',
        ivrGroup: 'IVR2',
        callType: 'DID'
      },
      { 
        id: 8, 
        date: new Date('2026-01-18 21:45:22'),
        callerIdNumber: '+914321098765',
        phoneNumber: '917890123456',
        user: 'Admin',
        duration: '00:04:33',
        callMode: 'Manual',
        disposition: 'Callback',
        hangupCause: 'NORMAL_CLEARING',
        direction: 'Outbound',
        ivrGroup: undefined,
        callType: 'Extension'
      },
      { 
        id: 9, 
        date: new Date('2026-01-18 20:15:55'),
        callerIdNumber: '+913210987654',
        phoneNumber: '918901234567',
        user: undefined,
        duration: '00:00:45',
        callMode: 'PBX',
        disposition: undefined,
        hangupCause: 'CALLQUEUE_TIMEOUT',
        direction: 'Inbound',
        ivrGroup: 'IVR1',
        callType: 'DID'
      },
      { 
        id: 10, 
        date: new Date('2026-01-18 19:50:10'),
        callerIdNumber: '+912109876543',
        phoneNumber: '919012345678',
        user: 'User2',
        duration: '00:06:15',
        callMode: 'Dialer',
        disposition: 'Language Barrier',
        hangupCause: 'NORMAL_CLEARING',
        direction: 'Outbound',
        ivrGroup: undefined,
        callType: 'External'
      },
    ];

    return of({
      data: mockData,
      totalCount: mockData.length
    }).pipe(delay(300));
  }

    search(searchTerm: string, page: number, pageSize: number): Observable<CDRSListResponse> {
    const params = new HttpParams()
      .set('search', searchTerm)
      .set('page', page.toString())
      .set('pageSize', pageSize.toString());

    
    
    
    
    const allMockData: CDRS[] = [
      { 
        id: 1, 
        date: new Date('2026-01-19 11:29:48'),
        callerIdNumber: '+919870046880',
        phoneNumber: '912717424450',
        user: undefined,
        duration: '00:05:15',
        callMode: 'PBX',
        disposition: undefined,
        hangupCause: 'CALLQUEUE_TIMEOUT',
        direction: 'Inbound',
        ivrGroup: undefined,
        callType: 'DID'
      },
      { 
        id: 2, 
        date: new Date('2026-01-19 10:15:32'),
        callerIdNumber: '+919876543210',
        phoneNumber: '912345678901',
        user: 'User1',
        duration: '00:03:45',
        callMode: 'Dialer',
        disposition: 'Sale',
        hangupCause: 'NORMAL_CLEARING',
        direction: 'Outbound',
        ivrGroup: 'IVR1',
        callType: 'External'
      },
    ];

    const filteredData = allMockData.filter(item => 
      item.callerIdNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      item.phoneNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (item.user && item.user.toLowerCase().includes(searchTerm.toLowerCase()))
    );

    return of({
      data: filteredData,
      totalCount: filteredData.length
    }).pipe(delay(300));
  }

    searchWithFilters(filters: CDRSFilters, page: number, pageSize: number): Observable<CDRSListResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('pageSize', pageSize.toString());

    
    Object.keys(filters).forEach(key => {
      const value = filters[key as keyof CDRSFilters];
      if (value) {
        params = params.set(key, value);
      }
    });

    
    
    
    
    const allMockData: CDRS[] = [
      { 
        id: 1, 
        date: new Date('2026-01-19 11:29:48'),
        callerIdNumber: '+919870046880',
        phoneNumber: '912717424450',
        user: undefined,
        duration: '00:05:15',
        callMode: 'PBX',
        disposition: undefined,
        hangupCause: 'CALLQUEUE_TIMEOUT',
        direction: 'Inbound',
        ivrGroup: undefined,
        callType: 'DID'
      },
    ];

    
    let filteredData = [...allMockData];

    if (filters.direction) {
      filteredData = filteredData.filter(item => item.direction === filters.direction);
    }

    if (filters.callMode) {
      filteredData = filteredData.filter(item => item.callMode === filters.callMode);
    }

    if (filters.callType) {
      filteredData = filteredData.filter(item => item.callType === filters.callType);
    }

    return of({
      data: filteredData,
      totalCount: filteredData.length
    }).pipe(delay(300));
  }

    getById(id: number): Observable<CDRS> {
    
    
    
    
    const mockCDRS: CDRS = {
      id: id,
      date: new Date('2026-01-19 11:29:48'),
      callerIdNumber: '+919870046880',
      phoneNumber: '912717424450',
      user: undefined,
      duration: '00:05:15',
      callMode: 'PBX',
      disposition: undefined,
      hangupCause: 'CALLQUEUE_TIMEOUT',
      direction: 'Inbound',
      ivrGroup: undefined,
      callType: 'DID'
    };

    return of(mockCDRS).pipe(delay(300));
  }

    export(filters: CDRSFilters, format: 'csv' | 'excel'): Observable<Blob> {
    
    
    
    
    const mockBlob = new Blob(['Mock export data'], { type: 'text/csv' });
    return of(mockBlob).pipe(delay(300));
  }


  getAllForExport(): Observable<{ data: any[], totalCount: number }> {
  
  return this.http.get<{ data: any[], totalCount: number }>(
    `${this.apiUrl}/cdrs/export`
  );
}
}