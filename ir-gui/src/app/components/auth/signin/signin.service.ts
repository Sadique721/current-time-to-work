import { HttpClient, HttpHeaders } from '@angular/common/http';
import * as RadiusConstants from 'src/app/core/RadiusUtils/RadiusConstants';
import { Injectable } from '@angular/core';
import { jwtDecode } from 'jwt-decode';
import { Observable } from 'rxjs';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' }),
};

@Injectable({
  providedIn: 'root',
})
export class SigninService {
  baseUrl = RadiusConstants.API_GATEWAY_COMMON_MANAGEMENT;

  constructor(private http: HttpClient) {}

  generateOtp(username: string, password: string): Observable<any> {
    const OTPGenerateDTO = {
      username: username,
      password: password,
      otpForStaff: true,
    };
    return this.http.post(
      `${this.baseUrl}/otp/generate`,
      OTPGenerateDTO,
      httpOptions,
    );
  }

  generateToken(data: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/login`, data, httpOptions);
  }

  refreshToken() {
    this.http.get(`${this.baseUrl}/refreshtoken`).subscribe(
      (response: any) => {
        localStorage.setItem('token', response.accessToken);
      },
      (error: any) => {
      },
    );
  }

  isLoggedIn() {
    const token = localStorage.getItem('token') as string;
    const data = this.getDecodedAccessToken(token);
    if (data != null) {
      let mvno = data.sub
        .substring(data.sub.indexOf('mvnoId":'))
        .split(',')[0]
        .split(':')[1];
      localStorage.setItem('mvnoId', mvno);
    }
    if (token == undefined || token === '' || token == null) {
      return false;
    } else {
      return true;
    }
  }

  getDecodedAccessToken(token: string): any {
    try {
      return jwtDecode(token);
    } catch (Error) {
      return null;
    }
  }

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('loggedInUser');
    localStorage.removeItem('mvnoId');
    localStorage.removeItem('demographic');
    localStorage.clear();
    return true;
  }
}
