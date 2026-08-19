import { Component, OnDestroy } from '@angular/core';
import {
  CommonService,
  IChildMenu,
  IMenuItem,
  ISubMenu,
  SidebarService,
  routes,
  url,
} from 'src/app/core.index';
import { NavigationEnd, Router, Event as RouterEvent } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-sidebar-one',
  templateUrl: './sidebar-one.component.html',
  styleUrls: ['./sidebar-one.component.scss'],
  standalone: false,
})
export class SidebarOneComponent implements OnDestroy {
  routes = routes;
  currentUrl: string[] = [];
  sidebarData: Array<IMenuItem> = [];
  destroy$: Subject<void>;
  loggedInUserData!: any;

  constructor(
    private sidebar: SidebarService,
    private router: Router,
    private common: CommonService,
  ) {
    this.destroy$ = new Subject();
    router.events
      .pipe(takeUntil(this.destroy$))
      .subscribe((event: RouterEvent) => {
        if (event instanceof NavigationEnd) {
          this.getRoutes(event);
        }
      });
    this.getRoutes(this.router);
    this.sidebarData = this.sidebar.getSidebarData();
    this.loggedInUserData = this.common.getUserData();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private getRoutes(route: url): void {
    const splitVal = route.url.split('/');
    this.currentUrl = splitVal.slice(1);
  }

  public miniSideBarMouseHover(position: string): void {
    if (position == 'over') {
      this.sidebar.expandSideBar.next(true);
    } else {
      this.sidebar.expandSideBar.next(false);
    }
  }

  expandMenus(menu: IMenuItem): void {
    this.sidebarData.forEach((mainMenus: IMenuItem) => {
      if (mainMenus.name === menu.name) {
        menu.showSubRoute = !menu.showSubRoute;
      } else {
        mainMenus.showSubRoute = false;
      }
    });
  }

  expandSubMenus(menu: IMenuItem, subMenuCode: string): void {
    menu.subMenus.forEach((subMenu: IChildMenu) => {
      subMenu.showSubRoute =
        subMenu.code == subMenuCode && !subMenu.showSubRoute;
    });
  }

  public toggleSidebar(): void {
    this.sidebar.switchSideMenuPosition();
  }
}
