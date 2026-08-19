import { Injectable } from "@angular/core";
import { BehaviorSubject, Observable } from "rxjs";
import { Router } from "@angular/router";
import { ConfirmationService, MessageService } from "primeng/api";
import { ToastService } from "./toasters.service";
import { IPermission, routes } from "../../core.index";
import { IAclMenu } from "src/app/components/settings/role-management/role-management-interface";
import { MenuEnum } from "../enums/sidebar-menu.enum";
import { HttpClient } from "@angular/common/http";
@Injectable({
  providedIn: "root",
})
export class CommonService extends ToastService {
  private _loading = new BehaviorSubject<boolean>(false);
  public readonly loading$ = this._loading.asObservable();

  public base: BehaviorSubject<string> = new BehaviorSubject<string>("");
  public page: BehaviorSubject<string> = new BehaviorSubject<string>("");
  public last: BehaviorSubject<string> = new BehaviorSubject<string>("");

  private _currency = new BehaviorSubject<string>("USD");
  public readonly currency$ = this._currency.asObservable();
  private rolePermission: Map<string, IAclMenu> = new Map();

  constructor(
    private msg: MessageService,
    private conf: ConfirmationService,
    private router: Router,
    private http:HttpClient
  ) {
    super(msg, conf);
  }

  spinnerShow() {
    this._loading.next(true);
  }

  spinnerHide() {
    this._loading.next(false);
  }

  getUserData(): any {
    const data = localStorage.getItem("loggedInUserData");
    return data ? JSON.parse(data) : null;
  }

  setUserData(key: string, value: string): void {
    let data = this.getUserData();
    if (!data) {
      data = {};
    }

    data[key] = value;
    localStorage.setItem("loggedInUserData", data);
  }

  clearUserData(): void {
    this.rolePermission.clear();
    localStorage.removeItem("rolePermission");
    localStorage.removeItem("loggedInUserData");
    this.router.navigate([routes.signIn]);
  }

  get fullName(): string | null {
    return this.getUserData()?.fullName || null;
  }

  get userId(): string | null {
    return this.getUserData()?.userId || null;
  }

  get agentId(): string | null {
    return this.getUserData()?.agentId || null;
  }

  get mvnoId(): string | null {
    return this.getUserData()?.mvnoId || null;
  }

  get userRoles(): any {
    return this.getUserData()?.userRoles || null;
  }

  get serviceArea(): any {
    return this.getUserData()?.serviceArea || null;
  } 

  get token(): string | null {
    return this.getUserData()?.token || null;
  }

  get mvnoName(): string | null {
    return this.getUserData()?.mvnoName || null;
  }

  get loginUserName(): string | null {
    return this.getUserData()?.loginUserName || null;
  }

  get loginProfile(): string | null {
    return this.getUserData()?.loginProfile || null;
  }

  setRolePermission(data: IAclMenu[]): void {
    if (data && Array.isArray(data) && data.length) {
      data.push({
        data: {
          name: "Logout",
          code: MenuEnum.LOGOUT,
          id: 0,
          isSelected: true,
        },
      } as IAclMenu);

      const filterList = data.filter(
        (i) => i.data.isSelected || i.data.code == MenuEnum.DASHBOARD
      );

      filterList.forEach((i) => {
        if (i.children && Array.isArray(i.children)) {
          i.children = i.children.filter((j) => i.data.isSelected);
        }
      });

      const dataStr = JSON.stringify(filterList);
      localStorage.setItem("rolePermission", dataStr);
    }
  }

  private getPermissionData(): Map<string, IAclMenu> {
    if (this.rolePermission.size > 0) {
      return this.rolePermission;
    } else if (localStorage.hasOwnProperty("rolePermission")) {
      const menuList = JSON.parse(
        localStorage.getItem("rolePermission") || "[]"
      );

      menuList.forEach((i: IAclMenu) => {
        if (i?.data?.code) {
          this.rolePermission.set(i.data.code, i);
        }
      });

      return this.rolePermission;
    } else {
      this.toastError("Sidebar menu detail not found");
      this.clearUserData();
      this.router.navigate([routes.signIn]);
      return this.rolePermission;
    }
  }

  hasPermission(codes: string[], onyView: boolean = false): IPermission {
    const falseData = {
      create: false,
      edit: false,
      delete: false,
      view: false,
    };

    const invalidCode = codes.find(
      (i) => i == "undefined" || i == "null" || i?.trim() != i
    );
    if (invalidCode) {
      return falseData;
    }

    const menuList: Map<string, IAclMenu> = this.getPermissionData();
    let parentMenu!: IAclMenu;
    const parentCode = codes[0];

    if (parentCode) {
      if (parentCode == MenuEnum.DASHBOARD) {
        return { ...falseData, view: true };
      }

      parentMenu = menuList.get(parentCode) as IAclMenu;
      if (parentMenu == undefined) {
        return falseData;
      }

      if (parentMenu.data.isSelected == false) {
        return falseData;
      }
      codes.shift();
    }

    const findMenu = (menu: IAclMenu, codeArr: string[]): any => {
      if (menu && codeArr.length > 0 && codeArr[0]) {
        menu = menu?.children?.find(
          (i: any) => i?.data?.code == codeArr[0]
        ) as IAclMenu;
        if (menu == undefined) {
          return falseData;
        }

        if (menu.data.isSelected == false) {
          return falseData;
        }

        codeArr.shift();
        return findMenu(menu, codeArr);
      }
      return menu;
    };

    const permission = findMenu(parentMenu, codes);

    if (onyView) {
      return { ...falseData, view: permission?.data?.isSelected };
    }

    return {
      create: this.findPermission(permission, "Create"),
      edit: this.findPermission(permission, "Edit"),
      delete: this.findPermission(permission, "Delete"),
      view: permission?.data?.isSelected,
    };
  }

  findPermission(data: any, option: "Create" | "Delete" | "Edit"): boolean {
    if (!data?.children?.length || !data?.data?.code) return false;

    const expectedCode = `${data.data.code}_${option.toLowerCase()}`;

    return data.children.some(
      (child: any) =>
        child?.data?.name === option &&
        child?.data?.code === expectedCode &&
        child?.data?.isSelected
    );
  }

get<T>(url: string): Observable<T> {
  return this.http.get<T>(url);
}

}
