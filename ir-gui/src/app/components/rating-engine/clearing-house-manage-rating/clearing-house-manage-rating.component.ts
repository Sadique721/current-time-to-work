import { Component, OnDestroy, OnInit } from "@angular/core";
import { FormControl } from "@angular/forms";
import { MatDialog } from "@angular/material/dialog";
import { debounceTime, Subject, takeUntil, finalize } from "rxjs";
import { UntypedFormControl, UntypedFormGroup, Validators } from "@angular/forms";
import {
  PaginationService,
  SidebarService,
  CommonService,
  pageSelection,
  tablePageSize,
  routes,
} from "src/app/core.index";
import { ClearingHouseManageService } from "./clearing-house-manage-rating.service";
import { ConfirmDialogComponent } from "src/app/core/shared/confirm-dialog/confirm-dialog.component";
declare var bootstrap: any;

@Component({
  selector: "app-clearing-house-manage-rating",
  templateUrl: "./clearing-house-manage-rating.component.html",
  standalone: false,
})
export class ClearingHouseManageRatingComponent implements OnInit, OnDestroy {
  tableData: any[] = [];
  pageSize = 10;
  totalData = 0;
  selectedItem: any = undefined;
  paginationSkip = 0;
  searchControl = new FormControl("");
  public routes = routes;
  private destroy$ = new Subject<void>();
  private lastPayload = "";

  showPassword = false;
  sftpForm: UntypedFormGroup;

  // API uses 1-based page numbers
  private currentApiPage = 1;

  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private clearingHouseService: ClearingHouseManageService,
    private commonService: CommonService,
    private dialog: MatDialog
  ) {
    this.sftpForm = new UntypedFormGroup({
      id: new UntypedFormControl(null),
      sftpHost: new UntypedFormControl("", [Validators.required]),
      sftpPort: new UntypedFormControl(22, [Validators.required]),
      sftpUsername: new UntypedFormControl("", [Validators.required]),
      sftpPassword: new UntypedFormControl("", [Validators.required]),
      sftpRemotePath: new UntypedFormControl("", [Validators.required]),
      sftpInboxPath: new UntypedFormControl("", [Validators.required]),
    });
  }

  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        this.pageSize = res.pageSize;

        // Calculate 1-based page number from skip/limit
        this.currentApiPage = Math.floor(res.skip / res.pageSize) + 1;

        const currentPayload = JSON.stringify({
          page: this.currentApiPage,
          pageSize: res.pageSize,
          search: this.searchControl.value?.trim(),
        });

        if (this.lastPayload !== currentPayload) {
          this.lastPayload = currentPayload;
          this.getTableData();
        }
      });
  }

  private getTableData(): void {
    const searchValue = this.searchControl.value?.trim() || "";
    this.commonService.spinnerShow();

    // Use /search when filter is active, /paginated for plain list
    const request$ = searchValue
      ? this.clearingHouseService.search({
          name: searchValue,
          page: this.currentApiPage,
          pageSize: this.pageSize,
        })
      : this.clearingHouseService.getPaginated(
          this.currentApiPage,
          this.pageSize
        );

    request$.pipe(takeUntil(this.destroy$)).subscribe({
      next: (res: any) => {
        this.commonService.spinnerHide();
        this.tableData = res.content || [];
        const pageDetails = res.pageDetails || {};
        this.totalData = pageDetails.totalRecords || 0;

        this.pagination.calculatePageSize.next({
          totalData: this.totalData,
          pageSize: this.pageSize,
          tableData: this.tableData,
          serialNumberArray: this.tableData.map((_, i) => i + 1),
        });
      },
      error: (err: any) => {
        this.commonService.spinnerHide();
        this.commonService.toastError(
          err?.error?.errorMessage || "Failed to fetch clearing houses"
        );
      },
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  fetchData(): void {
    this.lastPayload = "";
    this.currentApiPage = 1;
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  clearSearch(): void {
    this.searchControl.setValue("");
    this.fetchData();
  }

  openAddEdit(item: any = null): void {
    if (item?.id) {
      this.commonService.spinnerShow();
      this.clearingHouseService
        .getById(item.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (res: any) => {
            this.commonService.spinnerHide();
            this.selectedItem = res;
            this.showModal();
          },
          error: (err: any) => {
            this.commonService.spinnerHide();
            this.commonService.toastError(
              err?.error?.errorMessage || "Failed to load clearing house"
            );
          },
        });
    } else {
      this.selectedItem = {};
      this.showModal();
    }
  }

  showModal(): void {
    const modalElement = document.getElementById("add-clearing-house");
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.show();
    }
  }

  onCloseAddEdit(isReload: boolean): void {
    this.selectedItem = undefined;
    if (isReload) {
      this.lastPayload = "";
      this.pagination.tablePageSize.next({
        skip: this.paginationSkip,
        limit: this.pageSize,
        pageSize: this.pageSize,
      });
    }
  }

  confirmDelete(id: any): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: "420px",
      disableClose: true,
      data: {
        title: "Delete Clearing House",
        message: "Are you sure you want to delete this clearing house?",
        confirmButtonText: "Yes, Delete",
        cancelButtonText: "Cancel",
        iconClass: "ti ti-trash fs-24 text-danger",
      },
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.commonService.spinnerShow();
        this.clearingHouseService
          .deleteMethod(id)
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: () => {
              this.commonService.spinnerHide();
              this.commonService.toastSuccess(
                "Clearing house deleted successfully"
              );
              this.lastPayload = "";
              this.pagination.tablePageSize.next({
                skip: this.paginationSkip,
                limit: this.pageSize,
                pageSize: this.pageSize,
              });
            },
            error: (err: any) => {
              this.commonService.spinnerHide();
              this.commonService.toastError(
                err?.error?.errorMessage || "Failed to delete clearing house"
              );
            },
          });
      }
    });
  }

  openSftpSettings(item: any): void {
    this.showPassword = false;
    this.sftpForm.reset({ id: item.id });
    
    this.commonService.spinnerShow();
    this.clearingHouseService.getSftpConfig(item.id)
      .pipe(takeUntil(this.destroy$), finalize(() => this.commonService.spinnerHide()))
      .subscribe({
        next: (res: any) => {
          const config = res?.content || res;
          this.sftpForm.patchValue({
            id: item.id,
            sftpHost: config.sftpHost || "",
            sftpPort: config.sftpPort || 22,
            sftpUsername: config.sftpUsername || "",
            sftpPassword: config.sftpPassword || "",
            sftpRemotePath: config.sftpRemotePath || "",
            sftpInboxPath: config.sftpInboxPath || "",
          });

          const modalElement = document.getElementById("clearing-house-sftp-settings");
          if (modalElement) {
            const modalInstance = bootstrap.Modal.getInstance(modalElement) || new bootstrap.Modal(modalElement);
            modalInstance.show();
          }
        },
        error: (err) => {
          this.commonService.toastError("Failed to fetch TAP Configuration");
        }
      });
  }

  saveSftpSettings(): void {
    if (this.sftpForm.invalid) {
      this.sftpForm.markAllAsTouched();
      return;
    }

    const { id, ...payload } = this.sftpForm.getRawValue();
    this.commonService.spinnerShow();
    this.clearingHouseService.patchSftpConfig(id, payload)
      .pipe(takeUntil(this.destroy$), finalize(() => this.commonService.spinnerHide()))
      .subscribe({
        next: () => {
          this.commonService.toastSuccess("TAP Configuration updated successfully");
          const modalEl = document.getElementById("clearing-house-sftp-settings");
          if (modalEl) {
            const inst = bootstrap.Modal.getInstance(modalEl);
            inst?.hide();
          }
        },
        error: (err) => {
          this.commonService.toastError(err?.error?.msg || "Failed to update TAP Configuration");
        }
      });
  }
}
