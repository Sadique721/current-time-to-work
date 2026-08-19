import { Injectable } from "@angular/core";
import { BehaviorSubject } from "rxjs";
import { CommonService } from "./common.service";
import { IMenuItem } from "../models/models";
import { ChildMenuEnum, MenuEnum } from "../enums/sidebar-menu.enum";
import { routes } from "../helpers/routes";

@Injectable({
  providedIn: "root",
})
export class SidebarService {
  private collapseSubject = new BehaviorSubject<boolean>(false);
  collapse$ = this.collapseSubject.asObservable();
  permittedSidebarData: IMenuItem[] = [];

  constructor(private common: CommonService) {}

  toggleCollapse() {
    this.collapseSubject.next(!this.collapseSubject.value);
  }

  public sideBarPosition: BehaviorSubject<string> = new BehaviorSubject<string>(
    localStorage.getItem("sideBarPosition") || "false",
  );

  public toggleMobileSideBar: BehaviorSubject<string> =
    new BehaviorSubject<string>(
      localStorage.getItem("isMobileSidebar") || "false",
    );

  public expandSideBar: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(
    false,
  );

  public switchSideMenuPosition(): void {
    if (localStorage.getItem("sideBarPosition")) {
      this.sideBarPosition.next("false");
      this.expandSideBar.next(true);
      localStorage.removeItem("sideBarPosition");
    } else {
      this.sideBarPosition.next("true");
      this.expandSideBar.next(false);
      localStorage.setItem("sideBarPosition", "true");
    }
  }

  public switchMobileSideBarPosition(): void {
    if (localStorage.getItem("isMobileSidebar")) {
      this.toggleMobileSideBar.next("false");
      localStorage.removeItem("isMobileSidebar");
    } else {
      this.toggleMobileSideBar.next("true");
      localStorage.setItem("isMobileSidebar", "true");
    }
  }

  getSidebarData(isCashAllow: boolean = false): IMenuItem[] {
    if (this.permittedSidebarData.length > 0 && isCashAllow) {
      return this.permittedSidebarData;
    }
    const menu = this.sidebarData;

    const parentPermitedMenu = menu
      .filter((i) => !!i?.code)
      .filter((i) => this.common.hasPermission([i?.code], true)?.view)
      .map((i) => ({ ...i, routeKey: i.route.split("/")[1] }));

    parentPermitedMenu.forEach((i) => {
      if (i.subMenus && Array.isArray(i.subMenus)) {
        i.subMenus = i.subMenus
          .filter((j) => !!j?.code)
          .filter(
            (j) =>
              (j.code == ChildMenuEnum.MVNO_MANAGEMENT &&
                this.common.mvnoId == "1") ||
              this.common.hasPermission([i.code, j?.code], true)?.view,
          )
          .map((j) => ({ ...j, routeKey: j.route.split("/")[2] }));

        i.subMenus.forEach((j) => {
          if (j.hasSubRoute && Array.isArray(j.subMenus)) {
            j.subMenus = j.subMenus
              .filter((k) => !!k?.code)
              .filter(
                (k) =>
                  this.common.hasPermission([i.code, j?.code, k.code], true)
                    ?.view,
              )
              .map((k) => ({ ...k, routeKey: k.route.split("/")[3] }));
          }
        });
      }
    });

    this.permittedSidebarData = parentPermitedMenu;

    return parentPermitedMenu;
  }

  private sidebarData: IMenuItem[] = [
    {
      name: "Dashboard",
      code: MenuEnum.DASHBOARD,
      hasSubRoute: false,
      showSubRoute: false,
      icon2: "layout-dashboard",
      route: routes.dashboard,
      subMenus: [],
    },

    {
      name: "Rating Engine",
      code: MenuEnum.MASTER,
      hasSubRoute: true,
      showSubRoute: false,
      icon2: "star",
      route: routes.rating,
      subMenus: [
        {
          name: "Organization Management",
          route: routes.ratingorganization,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Country Management",
          route: routes.ratingcountry,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Prefix Management",
          route: routes.ratingprefix,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Pulse Management",
          route: routes.ratingpulse,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Partner Management",
          route: routes.ratingpartner,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Zone Management",
          route: routes.ratingzone,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Rate Package Management",
          route: routes.ratingratepackage,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Rate Package Group Management",
          route: routes.ratingratepackagegroup,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Product Plan Management",
          route: routes.ratingproductplan,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Accounts Management",
          route: routes.ratingaccount,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Invoice Template Management",
          route: routes.ratinginvoicetemplatemanage,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Tax Configuration Management",
          route: routes.ratingtax,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Agreement Management",
          route: routes.ratingagreement,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Source Configuration Management",
          route: routes.ratingsourceconfiguration,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Scheduler Configuration",
          route: routes.ratingscheduler,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Scheduler Audit Logs",
          route: routes.ratingscheduleraudit,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Download CDRs",
          route: routes.ratingdownloadcdrs,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Clearing House Management",
          route: routes.ratingclearinghouse,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "TAP Configuration",
          route: routes.ratingtapconfiguration,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "TAP Management",
          route: routes.ratingtaprecords,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "TAP Summary",
          route: routes.ratingtapsummary,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Invoice Records",
          route: routes.ratinginvoices,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Exchange Rate Management",
          route: routes.ratingexchangerate,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Error Rate Requests",
          route: routes.errorRateRequests,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "ReRate Requests",
          route: routes.rerateRequests,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        },
        {
          name: "Error Analysis",
          route: routes.errorAnalysis,
          code: ChildMenuEnum.COUNTRY,
          showSubRoute: false,
          hasSubRoute: false,
          subMenus: [],
        }
      ],
    },
    {
      name: "Logout",
      icon2: "logout",
      code: MenuEnum.LOGOUT,
      route: routes.signIn,
      hasSubRoute: false,
      showSubRoute: false,
      subMenus: [],
    },
  ];
}