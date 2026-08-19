import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, takeUntil } from 'rxjs/operators';
import { 
  PaginationService, 
  SidebarService, 
  CommonService, 
  IPermission, 
  tablePageSize, 
  pageSelection, 
  routes 
} from 'src/app/core.index';
import { TapConfigurationService } from './tap-configuration.service';
import { ConfirmDialogComponent } from 'src/app/core/shared/confirm-dialog/confirm-dialog.component';

declare var bootstrap: any;

@Component({
  selector: 'app-tap-configuration',
  templateUrl: './tap-configuration.component.html',
  styleUrls: ['./tap-configuration.component.scss'],
  standalone: false
})
export class TapConfigurationComponent implements OnInit, OnDestroy {
  public routes = routes;
  activeTab: 'fields' | 'profiles' | 'profile-groups' = 'fields';

  // Table & Pagination Data
  tableDataFields: any[] = [];
  tableDataProfiles: any[] = [];
  tableDataProfileGroups: any[] = [];
  allProfileGroups: any[] = [];
  pageSize = 10;
  totalData = 0;
  serialNumberArray: number[] = [];
  paginationSkip = 0;

  // Search
  searchDataValue = new FormControl('');

  // Fields Filter Form Controls
  filterCallType = new FormControl('');
  filterDataType = new FormControl('');
  filterMandatory = new FormControl('');
  filterFieldName = new FormControl('');

  // Profiles Filter Form Controls
  filterProfileName = new FormControl('');
  filterProfileDescription = new FormControl('');
  filterProfileActive = new FormControl('');

  // Applied Search and Filter Criteria snapshots
  appliedSearchValue = '';
  appliedFilterCallType = '';
  appliedFilterDataType = '';
  appliedFilterMandatory = '';
  appliedFilterFieldName = '';

  appliedFilterProfileName = '';
  appliedFilterProfileDescription = '';
  appliedFilterProfileActive = '';
  
  // Selection
  selectedField: any = null;
  selectedProfile: any = null;
  selectedGroup: any = null;
  detailField: any = null;
  detailProfile: any = null;
  detailGroup: any = null;

  isCollapsed = false;
  private destroy$ = new Subject<void>();
  private lastPayload: string = '';

  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private tapService: TapConfigurationService,
    private commonService: CommonService,
    private dialog: MatDialog,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const qTab = params['tab'];
      if (qTab === 'profile-groups' || qTab === 'profiles' || qTab === 'fields') {
        this.activeTab = qTab;
      }
    });

    // Watch for pagination page size / skip adjustments
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        const currentPayload = JSON.stringify({
          activeTab: this.activeTab,
          skip: res.skip,
          limit: res.pageSize,
          search: this.appliedSearchValue,
          callType: this.appliedFilterCallType,
          dataType: this.appliedFilterDataType,
          mandatory: this.appliedFilterMandatory,
          fieldName: this.appliedFilterFieldName,
          profileName: this.appliedFilterProfileName,
          profileDescription: this.appliedFilterProfileDescription,
          profileActive: this.appliedFilterProfileActive,
        });

        if (this.lastPayload !== currentPayload) {
          this.lastPayload = currentPayload;
          this.getTableData({ skip: res.skip, limit: res.pageSize });
          this.pageSize = res.pageSize;
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  switchTab(tab: 'fields' | 'profiles' | 'profile-groups'): void {
    this.activeTab = tab;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab: tab },
      queryParamsHandling: 'merge'
    });
    this.searchDataValue.setValue('', { emitEvent: false });
    this.filterCallType.setValue('', { emitEvent: false });
    this.filterDataType.setValue('', { emitEvent: false });
    this.filterMandatory.setValue('', { emitEvent: false });
    this.filterFieldName.setValue('', { emitEvent: false });

    this.filterProfileName.setValue('', { emitEvent: false });
    this.filterProfileDescription.setValue('', { emitEvent: false });
    this.filterProfileActive.setValue('', { emitEvent: false });

    this.searchData();
  }

  private getTableData(pageOption: pageSelection): void {
    const page = pageOption.skip / pageOption.limit + 1;

    if (this.activeTab === 'profile-groups') {
      this.commonService.spinnerShow();
      this.tapService.getTapProfileGroups()
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (res: any[]) => {
            this.commonService.spinnerHide();
            this.allProfileGroups = res || [];

            // Client-side filtering
            let filtered = [...this.allProfileGroups];
            if (this.appliedFilterProfileName) {
              filtered = filtered.filter(g => g.name?.toLowerCase().includes(this.appliedFilterProfileName.toLowerCase()));
            }
            if (this.appliedFilterProfileDescription) {
              filtered = filtered.filter(g => g.description?.toLowerCase().includes(this.appliedFilterProfileDescription.toLowerCase()));
            }
            if (this.appliedFilterProfileActive) {
              const isActiveBool = this.appliedFilterProfileActive === 'true';
              filtered = filtered.filter(g => (g.active ?? g.isActive) === isActiveBool);
            }

            this.totalData = filtered.length;

            // Slice for pagination
            const skip = pageOption.skip;
            const limit = pageOption.limit;
            this.tableDataProfileGroups = filtered.slice(skip, skip + limit);

            this.serialNumberArray = this.tableDataProfileGroups.map((_, i) => i + 1);

            this.pagination.calculatePageSize.next({
              totalData: this.totalData,
              pageSize: this.pageSize,
              tableData: this.tableDataProfileGroups,
              serialNumberArray: this.serialNumberArray,
            });
          },
          error: (error: any) => {
            this.commonService.spinnerHide();
            this.commonService.toastError(
              error?.error?.errorMessage || 'Failed to load profile groups'
            );
          }
        });
      return;
    }

    const payload: any = {
      page,
      pageSize: pageOption.limit,
      searchCriteria: {},
    };

    if (this.activeTab === 'fields') {
      const criteria: any = {};

      if (this.appliedFilterCallType) criteria.callType = this.appliedFilterCallType;
      if (this.appliedFilterDataType) criteria.dataType = this.appliedFilterDataType;
      if (this.appliedFilterMandatory) criteria.isMandatory = this.appliedFilterMandatory === 'true';

      if (this.appliedFilterFieldName) {
        criteria.fieldName = this.appliedFilterFieldName;
      } else if (this.appliedSearchValue) {
        criteria.fieldName = this.appliedSearchValue;
      }

      payload.searchCriteria = criteria;
    } else {
      const criteria: any = {};

      if (this.appliedFilterProfileName) criteria.profileName = this.appliedFilterProfileName;
      if (this.appliedFilterProfileDescription) criteria.description = this.appliedFilterProfileDescription;
      if (this.appliedFilterProfileActive) criteria.isActive = this.appliedFilterProfileActive === 'true';

      payload.searchCriteria = criteria;
    }

    this.commonService.spinnerShow();

    const request$ = this.activeTab === 'fields'
      ? this.tapService.postPaginatedFields(payload)
      : this.tapService.postPaginatedProfiles(payload);

    request$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.commonService.spinnerHide();
          if (this.activeTab === 'fields') {
            this.tableDataFields = res.content || [];
          } else {
            this.tableDataProfiles = res.content || [];
          }

          this.totalData = res.pageDetails?.totalRecords || 0;
          this.serialNumberArray = (this.activeTab === 'fields' ? this.tableDataFields : this.tableDataProfiles)
            .map((_, i) => i + 1);

          this.pagination.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: this.pageSize,
            tableData: this.activeTab === 'fields' ? this.tableDataFields : this.tableDataProfiles,
            serialNumberArray: this.serialNumberArray,
          });
        },
        error: (error: any) => {
          this.commonService.spinnerHide();
          this.commonService.toastError(
            error?.error?.errorMessage || `Failed to load ${this.activeTab}`
          );
        }
      });
  }

  public searchData(): void {
    this.appliedSearchValue = this.searchDataValue.value?.trim() || '';
    this.appliedFilterCallType = this.filterCallType.value || '';
    this.appliedFilterDataType = this.filterDataType.value || '';
    this.appliedFilterMandatory = this.filterMandatory.value || '';
    this.appliedFilterFieldName = this.filterFieldName.value?.trim() || '';

    this.appliedFilterProfileName = this.filterProfileName.value?.trim() || '';
    this.appliedFilterProfileDescription = this.filterProfileDescription.value?.trim() || '';
    this.appliedFilterProfileActive = this.filterProfileActive.value || '';

    this.lastPayload = '';
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  public refreshData(): void {
    this.lastPayload = '';
    this.getTableData({ skip: this.paginationSkip, limit: this.pageSize });
  }

  public clearSearch(): void {
    this.searchDataValue.setValue('', { emitEvent: false });
    this.filterCallType.setValue('', { emitEvent: false });
    this.filterDataType.setValue('', { emitEvent: false });
    this.filterMandatory.setValue('', { emitEvent: false });
    this.filterFieldName.setValue('', { emitEvent: false });

    this.filterProfileName.setValue('', { emitEvent: false });
    this.filterProfileDescription.setValue('', { emitEvent: false });
    this.filterProfileActive.setValue('', { emitEvent: false });

    this.searchData();
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  // Fields Add/Edit modal trigger
  openFieldAddEdit(item: any = null): void {
    this.selectedField = item ?? {};
    const modalElement = document.getElementById('add-tap-field');
    if (modalElement) {
      const modalInstance = bootstrap.Modal.getInstance(modalElement) || new bootstrap.Modal(modalElement);
      modalInstance.show();
    }
  }

  onFieldClose(isReload: boolean): void {
    this.selectedField = null;
    if (isReload) this.refreshData();
  }

  // Profiles Add/Edit modal trigger
  openProfileAddEdit(item: any = null): void {
    if (item && item.id) {
      this.commonService.spinnerShow();
      this.tapService.getTapProfileById(item.id)
        .subscribe({
          next: (res: any) => {
            this.commonService.spinnerHide();
            this.selectedProfile = res ?? {};
            this.showProfileModal();
          },
          error: (err: any) => {
            this.commonService.spinnerHide();
            this.commonService.toastError(err?.error?.errorMessage || 'Failed to fetch profile details');
          }
        });
    } else {
      this.selectedProfile = {};
      this.showProfileModal();
    }
  }

  private showProfileModal(): void {
    const modalElement = document.getElementById('add-tap-profile');
    if (modalElement) {
      const modalInstance = bootstrap.Modal.getInstance(modalElement) || new bootstrap.Modal(modalElement);
      modalInstance.show();
    }
  }

  onProfileClose(isReload: boolean): void {
    this.selectedProfile = null;
    if (isReload) this.refreshData();
  }

  confirmDeleteField(id: number | undefined): void {
    if (id === undefined) return;
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      data: {
        title: 'Delete TAP Field Mapping',
        message: 'Are you sure you want to delete this field mapping entry from the master dictionary?',
        confirmButtonText: 'Yes Delete',
        cancelButtonText: 'Cancel',
        iconClass: 'ti ti-trash fs-24 text-danger'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.tapService.deleteTapField(id).subscribe({
          next: () => {
            this.commonService.toastSuccess('TAP Field Mapping deleted successfully');
            this.refreshData();
          },
          error: (err: any) => {
            this.commonService.toastError(err?.error?.errorMessage || 'Delete failed');
          }
        });
      }
    });
  }

  confirmDeleteProfile(id: number | undefined): void {
    if (id === undefined) return;
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      data: {
        title: 'Delete TAP Profile',
        message: 'Are you sure you want to delete this TAP Profile config?',
        confirmButtonText: 'Yes Delete',
        cancelButtonText: 'Cancel',
        iconClass: 'ti ti-trash fs-24 text-danger'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.tapService.deleteTapProfile(id).subscribe({
          next: () => {
            this.commonService.toastSuccess('TAP Profile deleted successfully');
            this.refreshData();
          },
          error: (err: any) => {
            this.commonService.toastError(err?.error?.errorMessage || 'Delete failed');
          }
        });
      }
    });
  }

  openFieldDetail(id: number): void {
    this.commonService.spinnerShow();
    this.tapService.getTapFieldById(id)
      .subscribe({
        next: (res: any) => {
          this.commonService.spinnerHide();
          this.detailField = res ?? {};
          const modalElement = document.getElementById('view-tap-field-detail');
          if (modalElement) {
            const modalInstance = bootstrap.Modal.getInstance(modalElement) || new bootstrap.Modal(modalElement);
            modalInstance.show();
          }
        },
        error: (err: any) => {
          this.commonService.spinnerHide();
          this.commonService.toastError(err?.error?.errorMessage || 'Failed to fetch field details');
        }
      });
  }

  openProfileDetail(id: number): void {
    this.commonService.spinnerShow();
    this.tapService.getTapProfileById(id)
      .subscribe({
        next: (res: any) => {
          this.commonService.spinnerHide();
          this.detailProfile = res ?? {};
          const modalElement = document.getElementById('view-tap-profile-detail');
          if (modalElement) {
            const modalInstance = bootstrap.Modal.getInstance(modalElement) || new bootstrap.Modal(modalElement);
            modalInstance.show();
          }
        },
        error: (err: any) => {
          this.commonService.spinnerHide();
          this.commonService.toastError(err?.error?.errorMessage || 'Failed to fetch profile details');
        }
      });
  }

  openProfileGroupDetail(id: number): void {
    // Re-use or show info if needed, or inline
  }

  openProfileGroupAddEdit(item: any = null): void {
    if (item && item.id) {
      this.router.navigate([this.routes.ratingtapconfiguration, 'profile-group', 'add-edit', item.id]);
    } else {
      this.router.navigate([this.routes.ratingtapconfiguration, 'profile-group', 'add-edit']);
    }
  }

  onProfileGroupClose(isReload: boolean): void {
    this.selectedGroup = null;
    if (isReload) this.refreshData();
  }

  confirmDeleteProfileGroup(id: number | undefined): void {
    if (id === undefined) return;
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      data: {
        title: 'Delete TAP Profile Group',
        message: 'Are you sure you want to delete this TAP Profile Group?',
        confirmButtonText: 'Yes Delete',
        cancelButtonText: 'Cancel',
        iconClass: 'ti ti-trash fs-24 text-danger'
      }
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.tapService.deleteTapProfileGroup(id).subscribe({
          next: () => {
            this.commonService.toastSuccess('TAP Profile Group deleted successfully');
            this.refreshData();
          },
          error: (err: any) => {
            this.commonService.toastError(err?.error?.errorMessage || 'Delete failed');
          }
        });
      }
    });
  }
}
