import { Component, OnDestroy, OnInit } from "@angular/core";
import { Icountry } from "./country-manage.interface";
import { MatDialog } from "@angular/material/dialog";
import {
  PaginationService,
  SidebarService,
  CommonService,
  IPermission,
  tablePageSize,
  pageSelection,
} from "src/app/core.index";
import { CountryManageService } from "./country-manage.service";
import { FormControl } from "@angular/forms";
import { debounceTime, Subject, takeUntil } from "rxjs";
import { ChildMenuEnum, MenuEnum } from "src/app/core/enums/sidebar-menu.enum";
import { ConfirmDialogComponent } from "src/app/core/shared/confirm-dialog/confirm-dialog.component";
declare var bootstrap: any;

@Component({
  selector: "app-country-manage",
  templateUrl: "./country-manage.component.html",
  styleUrl: "./country-manage.component.scss",
  standalone: false,
})
export class CountryManageComponent implements OnInit, OnDestroy {
  tableData: Icountry[] = [];
  pageSize = 10;
  totalData = 0;
  selectedCountryRating: any = null;
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
    private countryManageService: CountryManageService,
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

    const payload: any = searchValue
      ? {
          page,
          pageSize: pageOption.limit,
          searchCriteria: { searchTerm: searchValue },
        }
      : {
          page,
          pageSize: pageOption.limit,
        };

    this.commonService.spinnerShow();
    this.countryManageService
      .postPaginated(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.commonService.spinnerHide();
          const responseList: Icountry[] = res.content || [];
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
        title: "Delete Country Rating",
        message: "Are you sure you want to delete this country rating?",
        confirmButtonText: "Yes Delete",
        cancelButtonText: "Cancel",
        iconClass: "ti ti-trash fs-24 text-danger",
      },
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.deleteCountry(id);
      }
    });
  }

  public deleteCountry(id: number): void {
    if (!id) return;

    this.countryManageService
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
    this.selectedCountryRating = null;
    isReload ? this.searchData() : null;
  }

  openCountryRatingAddEdit(item: any = null): void {
    this.selectedCountryRating = item ?? {};

    const modalElement = document.getElementById("add-country-rating");
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.show();
    }
  }
}
