import { Component } from '@angular/core';
import {
  NavigationCancel,
  NavigationEnd,
  NavigationStart,
  Router,
  Event as RouterEvent,
} from '@angular/router';
import { CommonService } from './core.index';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  standalone: false,
})
export class AppComponent {
  constructor(
    private router: Router,
    private commonService: CommonService,
  ) {
    this.router.events.subscribe((event: RouterEvent) => {
      if (event instanceof NavigationStart) {
        this.commonService.spinnerShow();
      }
      if (event instanceof NavigationEnd || event instanceof NavigationCancel) {
        this.commonService.spinnerHide();
      }
    });
  }
}
