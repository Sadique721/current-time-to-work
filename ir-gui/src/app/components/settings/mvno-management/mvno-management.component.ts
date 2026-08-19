import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, UntypedFormControl } from '@angular/forms';
import {
  CommonService,
  IPermission,
  pageSelection,
  PaginationService,
  routes,
  sharedModule,
  SidebarService,
  tablePageSize,
} from 'src/app/core.index';
import { CustomElementModule } from 'src/app/core/shared/custom-elements/custom-elemets.module';
import { ChildMenuEnum, MenuEnum } from 'src/app/core/enums/sidebar-menu.enum';
import {
  catchError,
  debounceTime,
  EMPTY,
  finalize,
  of,
  Subject,
  takeUntil,
} from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmDialogComponent } from 'src/app/core/shared/confirm-dialog/confirm-dialog.component';
import { Router, RouterModule } from '@angular/router';
import { IMvnoManagement } from './mvno-management.interface';
import { MvnoManagementService } from './mvno-management.service';
import { InputNumberModule } from 'primeng/inputnumber';
import customDatetime from 'src/app/core/shared/custom-elements/custom-datetime.pipe';
import dayjs from 'src/app/core/helpers/dayjs.config';

@Component({
  selector: 'app-mvno-management',
  imports: [
    sharedModule,
    CustomElementModule,
    CommonModule,
    InputNumberModule,
    RouterModule,
  ],
  templateUrl: './mvno-management.component.html',
  styleUrl: './mvno-management.component.scss',
})
export class MvnoManagementComponent implements OnInit, OnDestroy {
  routes = routes;
  tableData: Array<IMvnoManagement> = [];
  pageSize = 10;
  totalData = 0;
  paginationSkip = 0;
  serialNumberArray: number[] = [];
  isCollapsed = false;
  private destroy$ = new Subject<void>();
  private lastPayload: string = '';
  searchFC = new FormControl('');
  invoiceDateFC: UntypedFormControl = new UntypedFormControl();
  migrationDialog = false;
  mvnoOptions: any[] = [];
  oldMvno: any = null;
  oldMvnoName: string = '';
  mvnoMasterOptions: any[] = [];
  mvnoTitle = 'MVNO';
  mvnoNameList: any[] = [];
  workFlowData: any[] = [];
  newMvnoId = new UntypedFormControl(null);
  oldMvnoNameFC = new UntypedFormControl('');

  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private mvnoService: MvnoManagementService,
    private commonService: CommonService,
    private dialog: MatDialog,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.getMvnoNames();
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        const searchValue = this.searchFC.value?.trim() || '';
        const currentPayload = JSON.stringify({
          skip: res.skip,
          limit: res.pageSize,
        });

        
        if (this.lastPayload !== currentPayload) {
          this.lastPayload = currentPayload;
          this.getTableData({ skip: res.skip, limit: res.pageSize });
          this.pageSize = res.pageSize;
        }
      });
  }

  generateInvoice(): void {
    const payload = {
      startDate: customDatetime(this.invoiceDateFC.value.toISOString(), {
        format: 'YYYY-MM-DD',
      }),
    };

    this.mvnoService
      .genarateIspInvoice(payload)
      .pipe(
        takeUntil(this.destroy$),

        finalize(() => this.commonService.spinnerHide()),
        catchError((error) => {
          this.commonService.toastError(
            error?.error?.msg ||
              error?.error?.error ||
              error?.error?.ERROR ||
              'Something went wrong while fetching data',
          );

          return EMPTY;
        }),
      )
      .subscribe();
  }

  private getTableData(pageOption: pageSelection): void {
    const page = pageOption.skip / pageOption.limit + 1;

    let params = new HttpParams()
      .set('page', page)
      .set('pageSize', pageOption.limit)
      .set('sortOrder', 0)
      .set('sortBy', 'id');

    const filterValue = this.searchFC.value?.trim() || '';

    const filterPayload = {
      filter: [
        {
          filterDataType: '',
          filterValue,
          filterColumn: 'any',
          filterOperator: 'equalto',
          filterCondition: 'and',
        },
      ],
    };
    this.commonService.spinnerShow();
    this.mvnoService
      .getAllMVNO(filterPayload, params)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
        catchError((error) => {
          this.commonService.toastError(
            error?.error?.msg ||
              error?.error?.error ||
              error?.error?.ERROR ||
              'Something went wrong while fetching data',
          );

          return of({ dataList: [] });
        }),
      )
      .subscribe((res: any) => {
        const responseList: IMvnoManagement[] = res.dataList || [];
        this.tableData = responseList;

        this.totalData = res?.totalRecords || 0;
        this.serialNumberArray = this.tableData.map((_, i) => i + 1);
        this.pagination.calculatePageSize.next({
          totalData: this.totalData,
          pageSize: this.pageSize,
          tableData: this.tableData,
          serialNumberArray: this.serialNumberArray,
        });
      });
  }

  searchData(): void {
    
    this.lastPayload = '';
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  clearSearch(): void {
    this.searchFC.setValue('');
    this.lastPayload = '';
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  openAddEdit(item: IMvnoManagement | any = {}): void {
    this.router.navigate([routes.mvnoManagement + '/add-edit'], {
      state: {
        ...(item ? item : {}),
        isAddEdit: true,
      },
    });
  }

  openDocumentList(item: IMvnoManagement | any = {}): void {
    this.router.navigate([routes.mvnoManagement + '/document', item.id]);
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  sendMvnoId(mvno: any): void {
    this.mvnoOptions = this.mvnoMasterOptions.filter(
      (mvNo) => mvNo.id != mvno.id,
    );
    this.migrationDialog = true;
    this.oldMvno = mvno;
    this.oldMvnoNameFC.setValue(mvno.name);
    this.oldMvnoNameFC.disable();

    this.newMvnoId.setValue(null);
  }

  closeDialog(): void {
    this.migrationDialog = false;
  }

  transferISP(): void {
    const newMvnoId = this.newMvnoId.value;
    this.workFlowData = [];
    this.commonService.spinnerShow();
    this.mvnoService
      .PostMvnoId(this.oldMvno.id, newMvnoId)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.commonService.spinnerHide();
        }),
      )
      .subscribe({
        next: (res: any) => {
          if (res.dataList) {
            this.workFlowData = res.dataList;
          } else {
            this.migrationDialog = false;
            this.commonService.toastSuccess(res.responseMessage);
          }
        },
        error: (error: any) => {
          this.commonService.toastError(error?.error?.responseMessage);
        },
      });
  }

  getMvnoNames(): void {
    const url = '/mvno/getMvnoNameAndIds';
    this.commonService.spinnerShow();
    this.mvnoService
      .getMethod(url)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.commonService.spinnerHide();
        }),
      )
      .subscribe({
        next: (res: any) => {
          this.mvnoNameList = res.dataList;
          this.mvnoOptions = this.mvnoNameList.map((mvno) => ({
            name: mvno.name,
            id: mvno.id,
          }));
          this.mvnoMasterOptions = [
            ...this.mvnoNameList.map((mvno) => ({
              name: mvno.name,
              id: mvno.id,
            })),
          ];
        },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.responseMessage ||
              'Something went wrong while fetching data',
          );
        },
      });
  }
}
