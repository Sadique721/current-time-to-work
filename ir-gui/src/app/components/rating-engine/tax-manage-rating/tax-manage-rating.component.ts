import { Component, OnDestroy, OnInit } from "@angular/core";
import { ITaxConfig } from "./tax-manage-rating.interface";
import { MatDialog } from "@angular/material/dialog";
import {
  PaginationService,
  SidebarService,
  CommonService,
  IPermission,
  tablePageSize,
  pageSelection,
  routes,
} from "src/app/core.index";
import { TaxManageService } from "./tax-manage-rating.service";
import { FormControl } from "@angular/forms";
import { debounceTime, Subject, takeUntil } from "rxjs";
import { ChildMenuEnum, MenuEnum } from "src/app/core/enums/sidebar-menu.enum";
import { ConfirmDialogComponent } from "src/app/core/shared/confirm-dialog/confirm-dialog.component";
import { Router } from "@angular/router";

@Component({
  selector: "app-tax-manage-rating",
  templateUrl: "./tax-manage-rating.component.html",
  standalone: false,
})
export class TaxManageRatingComponent implements OnInit, OnDestroy {
  public routes = routes;
  tableData: ITaxConfig[] = [];
  pageSize = 10;
  totalData = 0;
  selectedTaxRating: any = null;
  serialNumberArray: number[] = [];
  paginationSkip = 0;
  searchDataValue = new FormControl("");
  isCollapsed = false;
  private destroy$ = new Subject<void>();
  private lastPayload: string = "";
  permission: IPermission;

  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private taxManageService: TaxManageService,
    private commonService: CommonService,
    private dialog: MatDialog,
    private router: Router
  ) {
    this.permission = this.commonService.hasPermission([
      MenuEnum.MASTER,
      ChildMenuEnum.TAX_CONFIG, // Assuming this exists or will be added
    ]);
  }

  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        const searchValue = this.searchDataValue.value?.trim() || "";
        const currentPayload = JSON.stringify({
          skip: res.skip,
          limit: res.pageSize,
          search: searchValue,
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

    const payload: any = {
      page,
      pageSize: pageOption.limit,
      searchCriteria: searchValue ? { searchTerm: searchValue } : {},
    };

    this.commonService.spinnerShow();
    this.taxManageService
      .postPaginated(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.commonService.spinnerHide();
          this.tableData = res.content || [];
          this.totalData = res.pageDetails?.totalRecords || 0;
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
          this.commonService.toastError(
            error?.error?.errorMessage || "Failed to load tax configurations"
          );
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
    this.searchData();
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  confirmDelete(id: number | undefined): void {
    if (id === undefined) return;
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: "420px",
      data: {
        title: "Delete Tax Configuration",
        message: "Are you sure you want to delete this tax configuration?",
        confirmButtonText: "Yes Delete",
        cancelButtonText: "Cancel",
        iconClass: "ti ti-trash fs-24 text-danger",
      },
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.deleteTax(id);
      }
    });
  }

  deleteTax(id: number): void {
    this.taxManageService.deleteMethod(id).subscribe({
      next: (res: any) => {
        this.commonService.toastSuccess("Tax configuration deleted successfully");
        this.searchData();
      },
      error: (err: any) => {
        this.commonService.toastError(err?.error?.errorMessage || "Delete failed");
      },
    });
  }

  openTaxRatingAddEdit(item: any = null): void {
    if (item) {
      this.router.navigate([routes.ratingtax + "/add-edit"], { state: item });
    } else {
      this.router.navigate([routes.ratingtax + "/add-edit"]);
    }
  }
}
