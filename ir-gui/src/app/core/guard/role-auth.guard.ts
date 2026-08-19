import { Injectable, OnDestroy } from '@angular/core';
import {
  Router,
  UrlTree,
  CanActivate,
  Event as RouterEvent,
  NavigationEnd,
  NavigationStart,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
} from '@angular/router';
import { Observable, Subject, takeUntil } from 'rxjs';
import { CommonService, routes, SidebarService, url } from '../../core.index';

@Injectable({
  providedIn: 'root',
})
export class RoleAuthGuard implements CanActivate {
  constructor(
    private router: Router,
    private sidebar: SidebarService,
    private common: CommonService,
  ) {}

  private checkValidRedirection(menuList: any, urlSplit: string[]): boolean {
    if (urlSplit.length <= 0 || menuList.length <= 0) {
      return false;
    }

    const list = menuList?.find((i: any) => i?.routeKey == urlSplit[0]);

    if (list == undefined || list == null || !list) {
      return false;
    }

    urlSplit.shift();

    if (urlSplit.length > 0) {
      if (list.subMenus?.length > 0) {
        return this.checkValidRedirection(list.subMenus, urlSplit);
      } else {
        return false;
      }
    }
    return true;
  }

  canActivate(
    next: ActivatedRouteSnapshot,
    state: RouterStateSnapshot,
  ): boolean | UrlTree {
    const splitVal = state.url.split('/');
    const currentUrl = splitVal.slice(1);

    return true;
    const allowRoutes = ['add-edit', 'location', 'mystaff'];

    if (currentUrl.includes('mvno-management')) {
      return this.common.mvnoId == '1';
    }

    const filteredURL = currentUrl.filter((i) => !allowRoutes.includes(i));

    const menuList: any = this.sidebar.getSidebarData(true);
    const isAllow = this.checkValidRedirection(menuList, filteredURL);

    if (!isAllow) {
      return this.router.parseUrl(routes.errorPages);
    }
    return isAllow;
  }
}
