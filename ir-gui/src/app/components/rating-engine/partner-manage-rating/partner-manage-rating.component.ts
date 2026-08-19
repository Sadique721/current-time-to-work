import { Component, OnDestroy, OnInit } from "@angular/core";
import { Ipartner } from "./partner-manage-rating.interface";
import { FormControl, UntypedFormControl, UntypedFormGroup, Validators } from "@angular/forms";
import { MatDialog } from "@angular/material/dialog";
import { debounceTime, Subject, takeUntil, finalize } from "rxjs";
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
import { PartnerManageService } from "./partner-manage-rating.service";
import { ConfirmDialogComponent } from "src/app/core/shared/confirm-dialog/confirm-dialog.component";
import { Code } from "angular-feather/icons";

declare var bootstrap: any;

@Component({
  selector: "app-partner-manage-rating",
  templateUrl: "./partner-manage-rating.component.html",
  styleUrl: "./partner-manage-rating.component.scss",
  standalone: false,
})
export class PartnerManageRatingComponent implements OnInit, OnDestroy {
  tableData: Ipartner[] = [];
  pageSize = 10;
  totalData = 0;
  selectedPartnerRating: any = null;
  serialNumberArray: number[] = [];
  paginationSkip = 0;
  searchDataValue = new FormControl("");
  searchPartnerType = new FormControl("");
  isCollapsed = false;
  showPassword = false;
  public routes = routes;
  private destroy$ = new Subject<void>();
  private lastPayload: string = "";
  permission: IPermission;

  partnerTypeOptions = [
    { label: "CUSTOMER", value: "CUSTOMER" },
    { label: "VENDOR", value: "VENDOR" },
    { label: "BOTH", value: "BOTH" },
  ];
  
  tapSftpRouteTypeOptions = [
    { label: "DIRECT", value: "DIRECT" },
    { label: "VIA_CLEARING_HOUSE", value: "VIA_CLEARING_HOUSE" },
  ];

  tapVersionOptions = [
    { label: "TAP3.12", value: "TAP3.12" },
  ];

  sftpForm: UntypedFormGroup;
  clearingHouseList: any[] = [];
  tapProfileGroupList: any[] = [];
  isPartnerRoaming = false;

  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private partnerManageService: PartnerManageService,
    private commonService: CommonService,
    private dialog: MatDialog
  ) {
    this.sftpForm = new UntypedFormGroup({
      partnerId: new UntypedFormControl(null),
      tapSftpRouteType: new UntypedFormControl(null, [Validators.required]),
      clearingHouseId: new UntypedFormControl(null),
      sftpHost: new UntypedFormControl(""),
      sftpPort: new UntypedFormControl(22),
      sftpUsername: new UntypedFormControl(""),
      sftpPassword: new UntypedFormControl(""),
      sftpRemotePath: new UntypedFormControl(""),
      sftpInboxPath: new UntypedFormControl(""),
      tapVersion: new UntypedFormControl(""),
      tapProfileGroupId: new UntypedFormControl(null),
    });

    this.sftpForm.get("tapSftpRouteType")?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((val) => {
        const chCtrl = this.sftpForm.get("clearingHouseId");
        const sftpFields = [
          "sftpHost",
          "sftpPort",
          "sftpUsername",
          "sftpPassword",
          "sftpRemotePath",
          "sftpInboxPath",
        ];
        
        if (val === "DIRECT") {
          sftpFields.forEach(f => this.sftpForm.get(f)?.setValidators([Validators.required]));
          chCtrl?.clearValidators();
          chCtrl?.setValue(null);
        } else if (val === "VIA_CLEARING_HOUSE") {
          chCtrl?.setValidators([Validators.required]);
          sftpFields.forEach(f => {
            const ctrl = this.sftpForm.get(f);
            ctrl?.clearValidators();
            ctrl?.setValue(f === "sftpPort" ? 22 : "");
          });
        } else {
          chCtrl?.clearValidators();
          sftpFields.forEach(f => this.sftpForm.get(f)?.clearValidators());
        }
        
        chCtrl?.updateValueAndValidity();
        sftpFields.forEach(f => this.sftpForm.get(f)?.updateValueAndValidity());
      });

    this.permission = this.commonService.hasPermission([
      MenuEnum.MASTER,
      ChildMenuEnum.COUNTRY,
    ]);
  }

  ngOnInit(): void {
    this.fetchClearingHouses();
    this.fetchTapProfileGroups();
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        const searchValue = this.searchDataValue.value?.trim() || "";
        const searchpartnertype = this.searchPartnerType.value?.trim() || "";
        const currentPayload = JSON.stringify({
          skip: res.skip,
          limit: res.pageSize,
          search: searchValue,
          partnerType: searchpartnertype || null,
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
    const partnerType = this.searchPartnerType.value?.trim() || null;

    const hasCriteria = !!searchValue || !!partnerType;

    const payload: any = hasCriteria
      ? {
          page,
          pageSize: pageOption.limit,
          searchCriteria: {
            partnerName: searchValue || null,
            partnerType: partnerType,
          },
        }
      : {
          page,
          pageSize: pageOption.limit,
        };

    this.commonService.spinnerShow();
    this.partnerManageService
      .postPaginated(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.commonService.spinnerHide();
          const responseList: Ipartner[] = res.content || [];
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
    this.searchPartnerType.setValue("");
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
        title: "Delete Partner Rating",
        message: "Are you sure you want to delete this partner rating?",
        confirmButtonText: "Yes Delete",
        cancelButtonText: "Cancel",
        iconClass: "ti ti-trash fs-24 text-danger",
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.deletePartner(id);
      }
    });
  }

  public deletePartner(id: number): void {
    if (!id) return;

    this.partnerManageService
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

          this.commonService.toastSuccess(
            "Partner rating deleted successfully"
          );
        },
        error: (error: any) => {
          this.commonService.toastError(
            "There is error deleting Partner rating"
          );
        },
      });
  }

  private fetchClearingHouses(): void {
    this.partnerManageService.getClearingHouses().subscribe({
      next: (res: any) => {
        this.clearingHouseList = Array.isArray(res) ? res : [];
      },
      error: (err: any) => console.error("Failed to load clearing houses", err),
    });
  }


  private fetchTapProfileGroups(): void {
    this.partnerManageService.getTapProfileGroupsDropdown().subscribe({
      next: (res: any) => {
        this.tapProfileGroupList = Array.isArray(res) ? res : [];
      },
      error: (err: any) => console.error("Failed to load TAP profile groups dropdown", err),
    });
  }

  onCloseAddEdit(isReload: boolean): void {
    this.selectedPartnerRating = null;
    isReload ? this.searchData() : null;
  }

  openPartnerRatingAddEdit(item: any = null): void {
    this.selectedPartnerRating = item ?? {};

    const modalElement = document.getElementById("add-partner-rating");
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.show();
    }
  }

  openSftpSettings(item: any): void {
    this.isPartnerRoaming = item.lineOfBusiness === "ROAMING";
    this.showPassword = false;
    
    const versionCtrl = this.sftpForm.get("tapVersion");
    const groupCtrl = this.sftpForm.get("tapProfileGroupId");
    
    if (this.isPartnerRoaming) {
      versionCtrl?.setValidators([Validators.required]);
      groupCtrl?.setValidators([Validators.required]);
    } else {
      versionCtrl?.clearValidators();
      groupCtrl?.clearValidators();
    }
    versionCtrl?.updateValueAndValidity();
    groupCtrl?.updateValueAndValidity();

    this.sftpForm.reset({
      partnerId: item.partnerId,
      tapSftpRouteType: null,
      clearingHouseId: null,
      sftpHost: "",
      sftpPort: 22,
      sftpUsername: "",
      sftpPassword: "",
      sftpRemotePath: "",
      sftpInboxPath: "",
      tapVersion: "",
      tapProfileGroupId: null,
    });

    this.commonService.spinnerShow();
    this.partnerManageService.getSftpConfig(item.partnerId)
      .pipe(takeUntil(this.destroy$), finalize(() => this.commonService.spinnerHide()))
      .subscribe({
        next: (res: any) => {
          const config = res?.content || res;
          
          if (config) {
            this.sftpForm.patchValue({
              tapSftpRouteType: config.tapSftpRouteType || (config.clearingHouseId ? "VIA_CLEARING_HOUSE" : null),
              clearingHouseId: config.clearingHouseId || null,
              sftpHost: config.sftpHost || "",
              sftpPort: config.sftpPort || 22,
              sftpUsername: config.sftpUsername || "",
              sftpPassword: config.sftpPassword || "",
              sftpRemotePath: config.sftpRemotePath || "",
              sftpInboxPath: config.sftpInboxPath || "",
              tapVersion: config.tapVersion || "",
              tapProfileGroupId: config.tapProfileGroupId || null,
            });
          }

          const modalElement = document.getElementById("partner-sftp-settings");
          if (modalElement) {
            const modalInstance = bootstrap.Modal.getInstance(modalElement) || new bootstrap.Modal(modalElement);
            modalInstance.show();
          }
        },
        error: (err) => {
          this.commonService.toastError("Failed to fetch SFTP settings");
        }
      });
  }

  saveSftpSettings(): void {
    if (this.sftpForm.invalid) {
      this.sftpForm.markAllAsTouched();
      return;
    }

    const { partnerId, tapSftpRouteType, clearingHouseId, tapVersion, tapProfileGroupId, ...sftpData } = this.sftpForm.value;
    const isDirect = tapSftpRouteType === "DIRECT";
    const payload: any = {
      tapSftpRouteType,
      clearingHouseId: tapSftpRouteType === "VIA_CLEARING_HOUSE" ? clearingHouseId : null,
      sftpHost: isDirect ? sftpData.sftpHost : null,
      sftpPort: isDirect ? sftpData.sftpPort : null,
      sftpUsername: isDirect ? sftpData.sftpUsername : null,
      sftpPassword: isDirect ? sftpData.sftpPassword : null,
      sftpRemotePath: isDirect ? sftpData.sftpRemotePath : null,
      sftpInboxPath: isDirect ? sftpData.sftpInboxPath : null,
    };

    if (this.isPartnerRoaming) {
      payload.tapVersion = tapVersion || null;
      payload.tapProfileGroupId = tapProfileGroupId || null;
    } else {
      payload.tapVersion = null;
      payload.tapProfileGroupId = null;
    }

    this.commonService.spinnerShow();
    this.partnerManageService.patchSftpConfig(partnerId, payload)
      .pipe(takeUntil(this.destroy$), finalize(() => this.commonService.spinnerHide()))
      .subscribe({
        next: () => {
          this.commonService.toastSuccess("TAP Configuration updated successfully");
          const modalEl = document.getElementById("partner-sftp-settings");
          if (modalEl) {
            const inst = bootstrap.Modal.getInstance(modalEl);
            inst?.hide();
          }
          this.searchData();
        },
        error: (err) => {
          this.commonService.toastError(err?.error?.msg || "Failed to update TAP Configuration");
        }
      });
  }
}
