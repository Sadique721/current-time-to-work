import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

export interface LeadStatus {
  id: number;
  statusName: string;
  code: string;
  status: boolean;
}

export interface LeadStatusListResponse {
  data: LeadStatus[];
  totalCount: number;
}

@Injectable({
  providedIn: 'root'
})
export class LeadStatusService {
  private apiUrl = '/api/lead-status'; 

  constructor(private http: HttpClient) {}

    getAll(page: number, pageSize: number): Observable<LeadStatusListResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('pageSize', pageSize.toString());

    
    
    
    
    const mockData: LeadStatus[] = [
      { id: 1, statusName: 'SURVEY', code: 'SURVEY', status: true },
      { id: 2, statusName: 'BUSY', code: 'BUSY', status: true },
      { id: 3, statusName: 'SALE', code: 'SALE', status: true },
      { id: 4, statusName: 'LOW', code: 'LOW', status: true },
      { id: 5, statusName: 'HIGH', code: 'HIGH', status: true },
      { id: 6, statusName: 'CALLBACK', code: 'CALLBACK', status: true },
      { id: 7, statusName: 'NOT_INTERESTED', code: 'NOT_INTERESTED', status: false },
      { id: 8, statusName: 'WRONG_NUMBER', code: 'WRONG_NUMBER', status: true },
      { id: 9, statusName: 'DO_NOT_CALL', code: 'DO_NOT_CALL', status: false },
      { id: 10, statusName: 'QUALIFIED', code: 'QUALIFIED', status: true },
    ];

    return of({
      data: mockData,
      totalCount: mockData.length
    }).pipe(delay(300));
  }

    search(searchTerm: string, page: number, pageSize: number): Observable<LeadStatusListResponse> {
    const params = new HttpParams()
      .set('search', searchTerm)
      .set('page', page.toString())
      .set('pageSize', pageSize.toString());

    
    
    
    
    const mockData: LeadStatus[] = [
      { id: 1, statusName: 'SURVEY', code: 'SURVEY', status: true },
      { id: 2, statusName: 'BUSY', code: 'BUSY', status: true },
      { id: 3, statusName: 'SALE', code: 'SALE', status: true },
    ];

    const filteredData = mockData.filter(item => 
      item.statusName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      item.code.toLowerCase().includes(searchTerm.toLowerCase())
    );

    return of({
      data: filteredData,
      totalCount: filteredData.length
    }).pipe(delay(300));
  }

    searchWithFilters(filters: any, page: number, pageSize: number): Observable<LeadStatusListResponse> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('pageSize', pageSize.toString());

    Object.keys(filters).forEach(key => {
      const value = filters[key];
      if (value) {
        params = params.set(key, value);
      }
    });

    
    
    
    
    const mockData: LeadStatus[] = [
      { id: 1, statusName: 'SURVEY', code: 'SURVEY', status: true },
    ];

    return of({
      data: mockData,
      totalCount: mockData.length
    }).pipe(delay(300));
  }

    getById(id: number): Observable<LeadStatus> {
    
    
    
    
    const mockLeadStatus: LeadStatus = {
      id: id,
      statusName: 'SURVEY',
      code: 'SURVEY',
      status: true,
    };

    return of(mockLeadStatus).pipe(delay(300));
  }

    create(leadStatus: Partial<LeadStatus>): Observable<LeadStatus> {
    
    
    
    
    const newLeadStatus: LeadStatus = {
      ...(leadStatus as LeadStatus),
      id: Math.floor(Math.random() * 1000)
    };

    return of(newLeadStatus).pipe(delay(300));
  }

    update(id: number, leadStatus: Partial<LeadStatus>): Observable<LeadStatus> {
    
    
    
    
    const updatedLeadStatus: LeadStatus = {
      ...(leadStatus as LeadStatus),
      id: id
    };

    return of(updatedLeadStatus).pipe(delay(300));
  }

    delete(id: number): Observable<void> {
    
    
    
    
    return of(void 0).pipe(delay(300));
  }

    bulkDelete(ids: number[]): Observable<void> {
    
    
    
    
    return of(void 0).pipe(delay(300));
  }

    exists(statusName: string, code: string, excludeId?: number): boolean {
    
    
    return false;
  }
}