import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CommonService, routes } from '../../core.index';
import { Router } from '@angular/router';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private router: Router, private common: CommonService) {}

  intercept(
    req: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    const token = !!this.common.token;
    if (token) {
      let token = this.common.token;
      let header = {
        Authorization: `${token}`,
      };

      Object.assign(
        header,
        localStorage.getItem('agentId') === '1' ? {} : { rf: 'pw' }
      );
      let newRequest = req.clone({
        setHeaders: header,
      });
      return next.handle(newRequest);
    } else if (this.router.url.includes(routes.signIn)) {
      return next.handle(req);
    } else {
      this.common.toastError('Please login First.');
      this.common.clearUserData();
      this.router.parseUrl(routes.signIn);
      return new Observable();
    }
  }
}
