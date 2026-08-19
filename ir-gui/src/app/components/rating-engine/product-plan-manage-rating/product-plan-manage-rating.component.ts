import { Component, OnDestroy, OnInit } from "@angular/core";
import { Iproductplan } from "./product-plan-manage-rating.interface";
import { FormControl } from "@angular/forms";
import { MatDialog } from "@angular/material/dialog";
import { Router } from "@angular/router";
import { debounceTime, Subject, takeUntil } from "rxjs";
import {
  routes,
  IPermission,
  PaginationService,
  SidebarService,
  CommonService,
  pageSelection,
  tablePageSize,
} from "src/app/core.index";
import { MenuEnum, ChildMenuEnum } from "src/app/core/enums/sidebar-menu.enum";
import { RatePackageGroupManageService } from "../rate-package-group-manage-rating/rate-package-group-manage-rating.service";
import { ProductPlanManageService } from "./product-plan-manage-rating.service";
import { ConfirmDialogComponent } from "src/app/core/shared/confirm-dialog/confirm-dialog.component";

@Component({
  selector: "app-product-plan-manage-rating",
  templateUrl: "./product-plan-manage-rating.component.html",
  styleUrl: "./product-plan-manage-rating.component.scss",
  standalone: false,
})
export class ProductPlanManageRatingComponent implements OnInit, OnDestroy {
  tableData: Iproductplan[] = [];
  public routes = routes;
  pageSize = 10;
  totalData = 0;
  selectedProductPlan: any = null;
  serialNumberArray: number[] = [];
  paginationSkip = 0;
  searchDataValue = new FormControl("");
  searchpackageType = new FormControl("");
  isCollapsed = false;
  private destroy$ = new Subject<void>();
  private lastPayload: string = "";
  permission: IPermission;
  typeOptions = [
    { label: "BUYING", value: "BUYING" },
    { label: "SELLING", value: "SELLING" },
  ];
  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private productPlanManageService: ProductPlanManageService,
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
        const searchpackageType = this.searchpackageType.value?.trim() || "";
        const currentPayload = JSON.stringify({
          skip: res.skip,
          limit: res.pageSize,
          search: searchValue,
          packageType: searchpackageType || null,
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
    const packageType = this.searchpackageType.value?.trim() || null;

    const hasCriteria = !!searchValue || !!packageType;

    const payload: any = hasCriteria
      ? {
          page,
          pageSize: pageOption.limit,
          searchCriteria: {
            searchTerm: searchValue || null,
            packageType: packageType,
          },
        }
      : {
          page,
          pageSize: pageOption.limit,
        };

    this.commonService.spinnerShow();
    this.productPlanManageService
      .postPaginated(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.commonService.spinnerHide();
          const responseList: Iproductplan[] = res.content || [];
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
    this.searchpackageType.setValue("");
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
        title: "Delete Product Plan",
        message: "Are you sure you want to delete this product plan?",
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

    this.productPlanManageService
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

          this.commonService.toastSuccess("Product Plan deleted successfully");
        },
        error: (error: any) => {
          this.commonService.toastError("There is error deleting Product Plan");
        },
      });
  }

  onCloseAddEdit(isReload: boolean): void {
    this.selectedProductPlan = null;
    isReload ? this.searchData() : null;
  }

  openProductPlanRatingAddEdit(item: any = null): void {
    const state: any = { isAddEdit: true };
    if (item?.productPlanId) {
      state.id = item.productPlanId;
    }
    this.router.navigate([routes.ratingproductplan + "/add-edit"], {
      state,
    });
  }
}
