import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CALL_CENTER_URL } from 'src/app/core/RadiusUtils/RadiusConstants';

export interface AudioPrompt {
  id: number;
  name: string;
  status: string;
  mvnoId: number;
  userId: number | null;
  managerId: number | null;
  file: string;
  originalName: string;
  createdAt: string;
  updatedAt: string;
}

export interface AudioPromptListResponse {
  statusCode: number;
  success: boolean;
  message: string;
  data: AudioPrompt[];
  pagination: {
    totalItems: number;
    totalPages: number;
    currentPage: number;
    pageSize: number;
  } | null;
}

export interface AudioPromptResponse {
  statusCode: number;
  success: boolean;
  message: string;
  data: AudioPrompt;
  pagination: null;
}

@Injectable({
  providedIn: 'root'
})
export class AudioPromptsService {
  private apiUrl = CALL_CENTER_URL+'/greeting';

  constructor(private http: HttpClient) {}

  getAll(page: number, pageSize: number): Observable<AudioPromptListResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('limit', pageSize.toString());

    return this.http.get<AudioPromptListResponse>(`${this.apiUrl}`, { params });
  }

  search(searchTerm: string, page: number, pageSize: number): Observable<AudioPromptListResponse> {
    const params = new HttpParams()
      .set('keyword', searchTerm)
      .set('page', page.toString())
      .set('limit', pageSize.toString());

    return this.http.get<AudioPromptListResponse>(`${this.apiUrl}/search`, { params });
  }

  getById(id: number): Observable<AudioPromptResponse> {
    return this.http.get<AudioPromptResponse>(`${this.apiUrl}/${id}`);
  }

  create(formData: FormData): Observable<AudioPromptResponse> {
    return this.http.post<AudioPromptResponse>(`${this.apiUrl}`, formData);
  }

  update(id: number, formData: FormData): Observable<AudioPromptResponse> {
    return this.http.put<AudioPromptResponse>(`${this.apiUrl}/${id}`, formData);
  }

  delete(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  streamAudio(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/play/${id}`, {
      responseType: 'blob',
    });
  }

  downloadAudio(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/play/${id}`, {
      responseType: 'blob',
    });
  }
}