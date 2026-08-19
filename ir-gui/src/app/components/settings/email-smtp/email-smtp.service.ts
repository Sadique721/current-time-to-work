import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

export interface EmailSMTPConfig {
  id?: number;
  host: string;
  port: number;
  from: string;
  username: string;
  password: string;
}

@Injectable({
  providedIn: 'root'
})
export class EmailSMTPService {
  private apiUrl = '/api/email-smtp'; 

  constructor(private http: HttpClient) {}

    getConfig(): Observable<EmailSMTPConfig> {
    
    
    
    
    const mockConfig: EmailSMTPConfig = {
      id: 1,
      host: 'smtp.gmail.com',
      port: 587,
      from: 'noreply@example.com',
      username: 'admin@example.com',
      password: '',
    };

    return of(mockConfig).pipe(delay(300));
  }

    updateConfig(config: EmailSMTPConfig): Observable<EmailSMTPConfig> {
    
    
    
    
    return of(config).pipe(delay(300));
  }

    testConnection(config: EmailSMTPConfig): Observable<{ success: boolean; message: string }> {
    
    
    
    
    return of({
      success: true,
      message: 'SMTP connection test successful'
    }).pipe(delay(1000));
  }
}