import { Component, OnDestroy, OnInit } from "@angular/core";
import { IOrganization } from "./organization-manage.interface";
import { MatDialog } from "@angular/material/dialog";
import {
  PaginationService,
  SidebarService,
  CommonService,
  tablePageSize,
  pageSelection,
} from "src/app/core.index";
import { OrganizationManageService } from "./organization-manage.service";
import { FormControl } from "@angular/forms";
import { debounceTime, Subject, takeUntil } from "rxjs";
import { ConfirmDialogComponent } from "src/app/core/shared/confirm-dialog/confirm-dialog.component";
declare var bootstrap: any;

@Component({
  selector: "app-organization-manage",
  templateUrl: "./organization-manage.component.html",
  styleUrl: "./organization-manage.component.scss",
  standalone: false,
})
export class OrganizationManageComponent implements OnInit, OnDestroy {
  tableData: IOrganization[] = [];
  pageSize = 10;
  totalData = 0;
  selectedOrganization: any = null;
  serialNumberArray: number[] = [];
  paginationSkip = 0;
  searchDataValue = new FormControl("");
  isCollapsed = false;
  private destroy$ = new Subject<void>();
  private lastPayload: string = "";

  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private organizationManageService: OrganizationManageService,
    private commonService: CommonService,
    private dialog: MatDialog
  ) {}

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
    this.organizationManageService
      .postPaginated(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.commonService.spinnerHide();
          this.totalData = res.pageDetails?.totalRecords || 0;
          this.tableData = res.content || [];
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

  openAddEdit(item: any = null): void {
    this.selectedOrganization = item ?? {};

    const modalElement = document.getElementById("add-organization");
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.show();
    }
  }

  onCloseAddEdit(isReload: boolean): void {
    this.selectedOrganization = null;
    isReload ? this.searchData() : null;
  }

  confirmDelete(id: number): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: "420px",
      disableClose: true,
      data: {
        title: "Delete Organization",
        message: "Are you sure you want to delete this organization?",
        confirmButtonText: "Yes Delete",
        cancelButtonText: "Cancel",
        iconClass: "ti ti-trash fs-24 text-danger",
      },
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.deleteOrganization(id);
      }
    });
  }

  deleteOrganization(id: number): void {
    if (!id) return;

    this.commonService.spinnerShow();
    this.organizationManageService
      .deleteMethod(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.commonService.spinnerHide();
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
          this.commonService.spinnerHide();
          this.commonService.toastError(
            error?.error?.ERROR || error?.error?.msg || "Failed to delete organization"
          );
        },
      });
  }
}
