import { Component, OnDestroy, OnInit } from "@angular/core";
import { ZoneRating } from "./zone-manage-rating.interface";
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
import { ZoneManageService } from "./zone-manage-rating.service";
import { ConfirmDialogComponent } from "src/app/core/shared/confirm-dialog/confirm-dialog.component";
import { Router } from "@angular/router";

@Component({
  selector: "app-zone-manage-rating",
  templateUrl: "./zone-manage-rating.component.html",
  styleUrl: "./zone-manage-rating.component.scss",
  standalone: false,
})
export class ZoneManageRatingComponent implements OnInit, OnDestroy {
  tableData: ZoneRating[] = [];
  public routes = routes;
  pageSize = 10;
  totalData = 0;
  selectedZoneRating: any = null;
  serialNumberArray: number[] = [];
  paginationSkip = 0;
  searchDataValue = new FormControl("");
  prefixModeFilter: "ALL" | "MANUAL" | "DROPDOWN" = "ALL";
  isCollapsed = false;
  private destroy$ = new Subject<void>();
  private lastPayload: string = "";
  permission: IPermission;

  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private zoneManageService: ZoneManageService,
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
        const currentPayload = JSON.stringify({
          skip: res.skip,
          limit: res.pageSize,
          search: searchValue,
          prefixInputMode: this.prefixModeFilter,
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
    };

    const criteria: any = {};
    if (searchValue) {
      criteria.searchTerm = searchValue;
    }
    if (this.prefixModeFilter !== "ALL") {
      criteria.prefixInputMode = this.prefixModeFilter;
      payload.prefixInputMode = this.prefixModeFilter;
    }
    if (Object.keys(criteria).length) {
      payload.searchCriteria = criteria;
    }

    this.commonService.spinnerShow();
    this.zoneManageService
      .postPaginated(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.commonService.spinnerHide();
          const responseList: ZoneRating[] = res.content || [];
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
    this.prefixModeFilter = "ALL";
    this.lastPayload = "";
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  onPrefixModeFilterChange(mode: string): void {
    this.prefixModeFilter = mode as "ALL" | "MANUAL" | "DROPDOWN";
    this.searchData();
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  public confirmDelete(id: number | undefined): void {
    if (!id) return;
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: "420px",
      disableClose: true,
      data: {
        title: "Delete Zone",
        message: "Are you sure you want to delete this zone?",
        confirmButtonText: "Yes Delete",
        cancelButtonText: "Cancel",
        iconClass: "ti ti-trash fs-24 text-danger",
      },
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.deleteZone(id);
      }
    });
  }

  public deleteZone(id: number | undefined): void {
    if (!id) return;

    this.zoneManageService
      .deleteMethod(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.lastPayload = "";
          this.pagination.tablePageSize.next({
            skip: 0,
            limit: this.pageSize,
            pageSize: this.pageSize,
          });
          this.commonService.toastSuccess(response.statusMsg || "Zone deleted successfully");
        },
        error: (error: any) => {
          this.commonService.toastError(error?.error?.msg || "Failed to delete zone");
        },
      });
  }

  onCloseAddEdit(isReload: boolean): void {
    this.selectedZoneRating = null;
    isReload ? this.searchData() : null;
  }

  openZoneRatingAddEdit(item: any = null): void {
    const state: any = { isAddEdit: true };
    if (item?.zoneId) {
      state.id = item.zoneId;
    }
    this.router.navigate([routes.ratingzone + "/add-edit"], {
      state,
    });
  }
}
