import { Component, OnDestroy } from '@angular/core';
import { NavigationEnd, Router, Event as RouterEvent } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { IMenuItem, SidebarService, url } from 'src/app/core.index';

@Component({
  selector: 'app-sidebar-three',
  templateUrl: './sidebar-three.component.html',
  styleUrls: ['./sidebar-three.component.scss'],
  standalone: false,
})
export class SidebarThreeComponent implements OnDestroy {
  currentUrl: string[] = [];
  sidebarData: Array<IMenuItem> = [];
  destroy$: Subject<void>;

  constructor(
    private sidebar: SidebarService,
    private router: Router,
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
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private getRoutes(route: url): void {
    const splitVal = route.url.split('/');
    this.currentUrl = splitVal.slice(1);
  }

  showMenu(menu: any): void {
    if (menu?.hasSubRoute == false) {
      this.router.navigate([menu?.route]);
    } else if (menu?.subRoutes?.length > 0 && menu.subRoutes[0]?.route) {
      this.router.navigate([menu.subRoutes[0].route]);
    }
  }
}
