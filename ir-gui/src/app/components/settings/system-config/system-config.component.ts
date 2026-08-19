import { Component } from '@angular/core';
import { PaginationService } from 'src/app/core/service/pagination.service';
import { SidebarService } from 'src/app/core/service/sidebar.service';
import { SystemConfigService } from './system-config.service';
import { CommonService } from 'src/app/core/service/common.service';
import {
  IPermission,
  pageSelection,
  tablePageSize,
} from 'src/app/core/models/models';
import { ChildMenuEnum, MenuEnum } from 'src/app/core/enums/sidebar-menu.enum';
import { FormControl } from '@angular/forms';
import { catchError, debounceTime, finalize, Subject, takeUntil } from 'rxjs';
import { sharedModule } from 'src/app/core/shared/shared.module';
import { CustomElementModule } from 'src/app/core/shared/custom-elements/custom-elemets.module';
import { CommonModule } from '@angular/common';
import { SystemConfigAddEditComponent } from './system-config-add-edit/system-config-add-edit.component';

declare var bootstrap: any;

@Component({
  selector: 'app-system-config',
  imports: [
    sharedModule,
    CustomElementModule,
    CommonModule,
    SystemConfigAddEditComponent,
  ],
  templateUrl: './system-config.component.html',
  styleUrl: './system-config.component.scss',
})
export class SystemConfigComponent {
  tableData: any[] = [];
  pageSize = 10;
  totalData = 0;
  selectedSystemConfig: any = null;
  serialNumberArray: number[] = [];
  paginationSkip = 0;
  searchDataValue = new FormControl('');
  isCollapsed = false;
  private destroy$ = new Subject<void>();
  private lastPayload: string = '';
  permission: IPermission;

  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private systemConfigService: SystemConfigService,
    private commonService: CommonService,
  ) {
    this.permission = this.commonService.hasPermission([
      MenuEnum.SETTING,
      ChildMenuEnum.SYSTEM_CONFIGURATION,
    ]);
  }

  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        const searchValue = this.searchDataValue.value?.trim() || '';
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
    const searchValue = this.searchDataValue.value?.trim() || '';
    const url = searchValue
      ? `/system/configuration/searchConfigurationByName?name=${searchValue}`
      : '/system/configuration/';

    this.commonService.spinnerShow();
    this.systemConfigService
      .getMethod(url)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.commonService.spinnerHide();
        }),
        catchError((error) => {
          this.commonService.toastError(error?.error?.ERROR);
          return error;
        }),
      )
      .subscribe((res: any) => {
        if (!res?.clientlist?.length) {
          this.tableData = [];
          this.totalData = 0;

          this.pagination.calculatePageSize.next({
            totalData: 0,
            pageSize: this.pageSize,
            tableData: [],
            serialNumberArray: [],
          });
          this.commonService.toastInfo('No Records Found !');
        } else {
          this.commonService.spinnerHide();
          const responseList: any[] = res.clientlist || [];
          this.totalData = res.clientlist?.length || 0;
          this.tableData = responseList;
          this.serialNumberArray = this.tableData.map((_, i) => i + 1);
          this.pagination.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: this.pageSize,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });
        }
      });
  }

  public searchData(): void {
    
    this.lastPayload = '';
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  public clearSearch(): void {
    this.searchDataValue.setValue('');
    this.lastPayload = '';
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

  openSystemConfigAddEdit(item: any = null): void {
    this.selectedSystemConfig = item ?? {};

    const modalElement = document.getElementById('add-system-config');
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.show();
    }
  }

  onCloseAddEdit(isReload: boolean): void {
    this.selectedSystemConfig = null;
    isReload ? this.searchData() : null;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
