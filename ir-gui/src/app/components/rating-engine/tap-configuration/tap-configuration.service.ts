import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { OCS_RATING } from 'src/app/core/RadiusUtils/RadiusConstants';

@Injectable({
  providedIn: 'root'
})
export class TapConfigurationService {
  private readonly baseUrl = OCS_RATING + '/v1/roaming/tap-fields';
  private readonly profilesUrl = OCS_RATING + '/v1/roaming/tap-profiles';

  constructor(private http: HttpClient) {}

  // TAP Fields (Master Dictionary) API
  getTapFields(page: number, size: number, search?: string): Observable<any> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);
    
    if (search) {
      params = params.set('search', search);
    }
    return this.http.get(this.baseUrl, { params });
  }

  postPaginatedFields(payload: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/paginated`, payload);
  }

  getAllTapFields(): Observable<any> {
    // Fetch all for dropdown override selections
    return this.http.post(`${this.baseUrl}/paginated`, { page: 1, pageSize: 1000 });
  }

  getTapFieldsDropdown(): Observable<any> {
    return this.http.get(`${this.baseUrl}/dropdown`);
  }

  getTapFieldById(id: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/${id}`);
  }

  createTapField(payload: any): Observable<any> {
    return this.http.post(this.baseUrl, payload);
  }

  updateTapField(id: number, payload: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/${id}`, payload);
  }

  deleteTapField(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/${id}`);
  }

  // TAP Profiles API (Assuming standard CRUD paths if needed)
  getTapProfiles(page: number, size: number, search?: string): Observable<any> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);
    
    if (search) {
      params = params.set('search', search);
    }
    return this.http.get(this.profilesUrl, { params });
  }

  postPaginatedProfiles(payload: any): Observable<any> {
    return this.http.post(`${this.profilesUrl}/paginated`, payload);
  }

  getTapProfileById(id: number): Observable<any> {
    return this.http.get(`${this.profilesUrl}/${id}`);
  }

  createTapProfile(payload: any): Observable<any> {
    return this.http.post(this.profilesUrl, payload);
  }

  updateTapProfile(id: number, payload: any): Observable<any> {
    return this.http.put(`${this.profilesUrl}/${id}`, payload);
  }

  deleteTapProfile(id: number): Observable<any> {
    return this.http.delete(`${this.profilesUrl}/${id}`);
  }

  // TAP Profile Groups API
  private readonly profileGroupsUrl = OCS_RATING + '/v1/roaming/tap-profile-groups';

  getTapProfileGroups(): Observable<any> {
    return this.http.get(this.profileGroupsUrl);
  }

  getTapProfileGroupById(id: number): Observable<any> {
    return this.http.get(`${this.profileGroupsUrl}/${id}`);
  }

  getProfilesByServiceType(): Observable<any> {
    return this.http.get(`${this.profilesUrl}/by-service-type`);
  }

  createTapProfileGroup(payload: any): Observable<any> {
    return this.http.post(this.profileGroupsUrl, payload);
  }

  updateTapProfileGroup(id: number, payload: any): Observable<any> {
    return this.http.put(`${this.profileGroupsUrl}/${id}`, payload);
  }

  deleteTapProfileGroup(id: number): Observable<any> {
    return this.http.delete(`${this.profileGroupsUrl}/${id}`);
  }
}
