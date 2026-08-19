import { Component, OnDestroy, OnInit } from "@angular/core";
import { Iprefix } from "./prefix-manage-rating.interface";
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
} from "src/app/core.index";
import { MenuEnum, ChildMenuEnum } from "src/app/core/enums/sidebar-menu.enum";
import { PrefixManageService } from "./prefix-manage-rating.service";
import { ConfirmDialogComponent } from "src/app/core/shared/confirm-dialog/confirm-dialog.component";
declare var bootstrap: any;

@Component({
  selector: "app-prefix-manage-rating",
  templateUrl: "./prefix-manage-rating.component.html",
  styleUrl: "./prefix-manage-rating.component.scss",
  standalone: false,
})
export class PrefixManageRatingComponent implements OnInit, OnDestroy {
  tableData: Iprefix[] = [];
  pageSize = 10;
  totalData = 0;
  selectedPrefixRating: any = null;
  serialNumberArray: number[] = [];
  paginationSkip = 0;
  searchDataValue = new FormControl("");
  prefixTypeFilter: string = "ALL";
  isCollapsed = false;
  private destroy$ = new Subject<void>();
  private lastPayload: string = "";
  permission: IPermission;

  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private prefixManageService: PrefixManageService,
    private commonService: CommonService,
    private dialog: MatDialog
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
    if (this.prefixTypeFilter !== "ALL") {
      criteria.prefixType = this.prefixTypeFilter;
      payload.prefixType = this.prefixTypeFilter;
    }
    if (Object.keys(criteria).length) {
      payload.searchCriteria = criteria;
    }

    this.commonService.spinnerShow();
    this.prefixManageService
      .postPaginated(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.commonService.spinnerHide();
          const responseList: Iprefix[] = res.content || [];
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
    this.prefixTypeFilter = "ALL";
    this.lastPayload = "";
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  onPrefixTypeFilterChange(type: string): void {
    this.prefixTypeFilter = type;
    this.searchData();
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
        title: "Delete Prefix Rating",
        message: "Are you sure you want to delete this prefix rating?",
        confirmButtonText: "Yes Delete",
        cancelButtonText: "Cancel",
        iconClass: "ti ti-trash fs-24 text-danger",
      },
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.deletePrefix(id);
      }
    });
  }

  public deletePrefix(id: number): void {
    if (!id) return;

    this.prefixManageService
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

          this.commonService.toastSuccess(response.statusMsg);
        },
        error: (error: any) => {
          this.commonService.toastError(error?.error?.ERROR);
        },
      });
  }

  onCloseAddEdit(isReload: boolean): void {
    this.selectedPrefixRating = null;
    isReload ? this.searchData() : null;
  }

  openPrefixRatingAddEdit(item: any = null): void {
    this.selectedPrefixRating = item ?? {};

    const modalElement = document.getElementById("add-prefix-rating");
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.show();
    }
  }
}
