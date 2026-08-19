import { Injectable } from '@angular/core';
import { Router, UrlTree, CanActivate } from '@angular/router';
import { Observable } from 'rxjs';
import { CommonService, routes } from '../../core.index';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard implements CanActivate {
  constructor(private router: Router, private common: CommonService) {}

  canActivate():
    | boolean
    | UrlTree
    | Observable<boolean | UrlTree>
    | Promise<boolean | UrlTree> {
    const isTokenPresent = !!this.common.token;

    if (isTokenPresent) {
      return true;
    } else {
      return this.router.parseUrl(routes.signIn);
    }
  }
}
