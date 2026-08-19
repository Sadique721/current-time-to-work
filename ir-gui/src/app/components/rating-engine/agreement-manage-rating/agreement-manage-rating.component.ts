import { Component, OnDestroy, OnInit } from "@angular/core";
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
import { Router } from "@angular/router";
import { MenuEnum, ChildMenuEnum } from "src/app/core/enums/sidebar-menu.enum";
import { AgreementManageService } from "./agreement-manage-rating.service";
import { ConfirmDialogComponent } from "src/app/core/shared/confirm-dialog/confirm-dialog.component";
import { IAgreement } from "./agreement-manage-rating.interface";

declare var bootstrap: any;

@Component({
  selector: "app-agreement-manage-rating",
  templateUrl: "./agreement-manage-rating.component.html",
  styleUrl: "./agreement-manage-rating.component.scss",
  standalone: false,
})
export class AgreementManageRatingComponent implements OnInit, OnDestroy {
  tableData: IAgreement[] = [];
  pageSize = 10;
  totalData = 0;
  selectedAgreement: any = null;
  serialNumberArray: number[] = [];
  paginationSkip = 0;
  searchDataValue = new FormControl("");
  isCollapsed = false;
  public routes = routes;
  private destroy$ = new Subject<void>();
  private lastPayload: string = "";
  permission: IPermission;

  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private agreementService: AgreementManageService,
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
        const currentPayload = JSON.stringify({ skip: res.skip, limit: res.pageSize });
        if (this.lastPayload !== currentPayload) {
          this.lastPayload = currentPayload;
          this.getTableData({ skip: res.skip, limit: res.pageSize });
          this.pageSize = res.pageSize;
        }
      });
  }

  private getTableData(pageOption: pageSelection): void {
    const searchValue = this.searchDataValue.value?.trim() || "";

    this.commonService.spinnerShow();
    this.agreementService
      .getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.commonService.spinnerHide();
          let responseList: IAgreement[] = Array.isArray(res) ? res : (res.content || []);
          if (searchValue) {
            responseList = responseList.filter((item) =>
              item.agreementCode?.toLowerCase().includes(searchValue.toLowerCase())
            );
          }
          this.totalData = responseList.length;
          const start = pageOption.skip;
          const end = start + pageOption.limit;
          this.tableData = responseList.slice(start, end);
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
            error?.error?.msg || "Something went wrong while fetching data"
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
        title: "Delete Agreement",
        message: "Are you sure you want to delete this agreement?",
        confirmButtonText: "Yes Delete",
        cancelButtonText: "Cancel",
        iconClass: "ti ti-trash fs-24 text-danger",
      },
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.deleteAgreement(id);
      }
    });
  }

  public deleteAgreement(id: number): void {
    if (!id) return;

    this.agreementService
      .deleteMethod(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.lastPayload = "";
          this.pagination.tablePageSize.next({
            skip: this.paginationSkip,
            limit: this.pageSize,
            pageSize: this.pageSize,
          });
          this.commonService.toastSuccess("Agreement deleted successfully");
        },
        error: () => {
          this.commonService.toastError("Error deleting agreement");
        },
      });
  }

  onCloseAddEdit(isReload: boolean): void {
    this.selectedAgreement = null;
    if (isReload) {
      this.lastPayload = "";
      this.pagination.tablePageSize.next({
        skip: this.paginationSkip,
        limit: this.pageSize,
        pageSize: this.pageSize,
      });
    }
  }

  openAgreementAddEdit(item: any = null): void {
    if (item && item.agreementId) {
      this.router.navigate([this.routes.ratingagreement, 'add-edit'], {
        state: { id: item.agreementId }
      });
    } else {
      this.router.navigate([this.routes.ratingagreement, 'add-edit']);
    }
  }

  showModal(): void {
    // No longer needed
  }
}
