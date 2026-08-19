import { Component, OnDestroy, OnInit } from "@angular/core";
import { Irate } from "./rate-package-manage-rating.interface";
import { FormControl } from "@angular/forms";
import { MatDialog } from "@angular/material/dialog";
import { debounceTime, Subject, takeUntil } from "rxjs";
import {
  IPermission,
  PaginationService,
  SidebarService,
  CommonService,
  pageSelection,
  tablePageSize,
  routes,
} from "src/app/core.index";
import { MenuEnum, ChildMenuEnum } from "src/app/core/enums/sidebar-menu.enum";
import { RatePackageManageService } from "./rate-package-manage-rating.service";
import { ConfirmDialogComponent } from "src/app/core/shared/confirm-dialog/confirm-dialog.component";
import { Router } from "@angular/router";
declare var bootstrap: any;

@Component({
  selector: "app-rate-package-manage-rating",
  templateUrl: "./rate-package-manage-rating.component.html",
  styleUrl: "./rate-package-manage-rating.component.scss",
  standalone: false,
})
export class RatePackageManageRatingComponent implements OnInit, OnDestroy {
  tableData: Irate[] = [];
  public routes = routes;
  pageSize = 10;
  totalData = 0;
  selectedRatePackage: any = null;
  serialNumberArray: number[] = [];
  paginationSkip = 0;
  searchDataValue = new FormControl("");
  searchServiceType = new FormControl("");
  isCollapsed = false;
  private destroy$ = new Subject<void>();
  private lastPayload: string = "";
  permission: IPermission;
  serviceTypeOptions = [
    { label: "VOICE", value: "VOICE" },
    { label: "SMS", value: "SMS" },
    { label: "USAGE", value: "USAGE" },
  ];
  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private ratePackageManageService: RatePackageManageService,
    private commonService: CommonService,
    private dialog: MatDialog,
    private router: Router
  ) {
    this.permission = this.commonService.hasPermission([
      MenuEnum.MASTER,
      ChildMenuEnum.COUNTRY,
    ]);
  }
  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        const searchValue = this.searchDataValue.value?.trim() || "";
        const searchServiceType = this.searchServiceType.value?.trim() || "";
        const currentPayload = JSON.stringify({
          skip: res.skip,
          limit: res.pageSize,
          search: searchValue,
          serviceType: searchServiceType || null,
        });

        
        if (this.lastPayload !== currentPayload) {
          this.lastPayload = currentPayload;
          this.getTableData({ skip: res.skip, limit: res.pageSize });
          this.pageSize = res.pageSize;
        }
      });
  }

  private getTableData(pageOption: pageSelection): void {
    const page = pageOption.skip / pageOption.limit + 1;
    const searchValue = this.searchDataValue.value?.trim() || "";
    const serviceType = this.searchServiceType.value?.trim() || null;

    const hasCriteria = !!searchValue || !!serviceType;

    const payload: any = hasCriteria
      ? {
          page,
          pageSize: pageOption.limit,
          searchCriteria: {
            searchTerm: searchValue || null,
            serviceType: serviceType,
          },
        }
      : {
          page,
          pageSize: pageOption.limit,
        };

    this.commonService.spinnerShow();
    this.ratePackageManageService
      .postPaginated(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.commonService.spinnerHide();
          const responseList: Irate[] = res.content || [];
          this.totalData = res.pageDetails?.totalRecords || 0;
          this.tableData = responseList;
          this.serialNumberArray = this.tableData.map((_, i) => i + 1);
          this.pagination.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: this.pageSize,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });
        },
        error: (error: any) => {
          this.commonService.spinnerHide();
          if (error.status === 404 && searchValue) {
            this.tableData = [];
            this.totalData = 0;
            this.pagination.calculatePageSize.next({
              totalData: 0,
              pageSize: this.pageSize,
              tableData: [],
              serialNumberArray: [],
            });
            this.commonService.toastInfo(error?.error?.msg);
          } else {
            this.commonService.toastError(
              error?.error?.msg || "Something went wrong while fetching data"
            );
          }
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  public searchData(): void {
    
    this.lastPayload = "";
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  public clearSearch(): void {
    this.searchDataValue.setValue("");
    this.searchServiceType.setValue("");
    this.lastPayload = "";
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  public confirmDelete(id: number): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: "420px",
      disableClose: true,
      data: {
        title: "Delete Rate Package",
        message: "Are you sure you want to delete this rate package?",
        confirmButtonText: "Yes Delete",
        cancelButtonText: "Cancel",
        iconClass: "ti ti-trash fs-24 text-danger",
      },
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.deleteAccount(id);
      }
    });
  }

  public deleteAccount(id: number): void {
    if (!id) return;

    this.ratePackageManageService
      .deleteMethod(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          if (
            this.totalData % this.pageSize === 1 &&
            this.totalData > this.pageSize
          ) {
            const previousPageSkip =
              (Math.ceil(this.totalData / this.pageSize) - 2) * this.pageSize;
            this.lastPayload = ""; 
            this.pagination.tablePageSize.next({
              skip: previousPageSkip,
              limit: this.pageSize,
              pageSize: this.pageSize,
            });
          } else {
            this.lastPayload = ""; 
            this.pagination.tablePageSize.next({
              skip: 0,
              limit: this.pageSize,
              pageSize: this.pageSize,
            });
          }

          this.commonService.toastSuccess("Rate Package deleted successfully");
        },
        error: (error: any) => {
          this.commonService.toastError("There is error deleting Rate Package");
        },
      });
  }

  onCloseAddEdit(isReload: boolean): void {
    this.selectedRatePackage = null;
    isReload ? this.searchData() : null;
  }

  openRatePackageRatingAddEdit(item: any = null): void {
    const state: any = { isAddEdit: true };
    if (item?.ratePackageId) {
      state.id = item.ratePackageId;
    }
    this.router.navigate([routes.ratingratepackage + "/add-edit"], {
      state,
    });
  }
}
