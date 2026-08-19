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
import { IPaymentGateway } from './payment-gateway.interface';
import { PaymentGatewayService } from './payment-gateway.service';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmDialogComponent } from 'src/app/core/shared/confirm-dialog/confirm-dialog.component';
import { Router } from '@angular/router';

type IPaymentGatewayExtend = IPaymentGateway & {
  isActiveControl: UntypedFormControl;
};

@Component({
  selector: 'app-payment-gateway',
  imports: [sharedModule, CustomElementModule, CommonModule],
  templateUrl: './payment-gateway.component.html',
  styleUrl: './payment-gateway.component.scss',
})
export class PaymentGatewayComponent implements OnInit, OnDestroy {
  tableData: Array<IPaymentGatewayExtend> = [];
  pageSize = 10;
  totalData = 0;
  serialNumberArray: number[] = [];
  paginationSkip = 0;
  isCollapsed = false;
  private destroy$ = new Subject<void>();
  private destroySub$;
  private lastPayload: string = '';
  permission: IPermission;

  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private payGateService: PaymentGatewayService,
    private commonService: CommonService,
    private dialog: MatDialog,
    private router: Router,
  ) {
    this.destroySub$ = new Subject<void>();
    this.permission = this.commonService.hasPermission([
      MenuEnum.SETTING,
      ChildMenuEnum.PAYMENT_GATEWAY_CONFIGURATION,
    ]);
  }

  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        const currentPayload = JSON.stringify({
          skip: res.skip,
          limit: res.pageSize,
        });

        
        if (this.lastPayload !== currentPayload) {
          this.lastPayload = currentPayload;
          this.destroySub$.next();
          this.getTableData({ skip: res.skip, limit: res.pageSize });
          this.pageSize = res.pageSize;
        }
      });
  }

  private getTableData(pageOption: pageSelection): void {
    const page = pageOption.skip / pageOption.limit + 1;

    const pageBody = {
      page,
      pageSize: pageOption.limit,
    };
    this.commonService.spinnerShow();

    this.payGateService
      .getAlllPaymentConfig(pageBody)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
        catchError((error) => {
          if (error.status === 404) {
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
              error?.error?.error || 'Something went wrong while fetching data',
            );
          }
          return of({ templateList: [] });
        }),
      )
      .subscribe((res) => {
        const responseList: IPaymentGateway[] = res.dataList || [];
        this.tableData = responseList.map(this.addControls.bind(this));

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

  private addControls(data: IPaymentGateway): IPaymentGatewayExtend {
    const item = {
      ...data,
      isActiveControl: new UntypedFormControl(data.isActive || false),
    };

    item.isActiveControl.valueChanges
      .pipe(takeUntil(this.destroySub$))
      .subscribe(() => {
        this.updateStauts(item);
      });

    return item;
  }

  updateStauts(data: IPaymentGatewayExtend): void {
    const payload = {
      paymentConfigId: data.paymentConfigId,
      isActive: data.isActiveControl.value,
    };

    this.commonService.spinnerShow();
    this.payGateService
      .updatePaymentGatewayStatus(payload)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
        catchError((error) => {
          this.commonService.toastError(
            (error?.error?.error || error?.error?.ERROR) ??
              'Something went wrong while update data',
          );
          return of({});
        }),
      )
      .subscribe((res) => {
        if (
          this.totalData % this.pageSize === 1 &&
          this.totalData > this.pageSize
        ) {
          const previousPageSkip =
            (Math.ceil(this.totalData / this.pageSize) - 2) * this.pageSize;
          this.lastPayload = ''; 
          this.pagination.tablePageSize.next({
            skip: previousPageSkip,
            limit: this.pageSize,
            pageSize: this.pageSize,
          });
        } else {
          this.lastPayload = ''; 
          this.pagination.tablePageSize.next({
            skip: 0,
            limit: this.pageSize,
            pageSize: this.pageSize,
          });
        }

        if (res.status == 200) {
          this.commonService.toastSuccess(res?.message);
        } else {
          res?.message ? this.commonService.toastError(res?.message) : null;
        }
      });
  }

  confirmDelete(paymentConfigId: number): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      disableClose: true,
      data: {
        title: 'Delete Payment Gateway',
        message: 'Do you want to delete this payment gateway configuration ?',
        confirmButtonText: 'Yes Delete',
        cancelButtonText: 'Cancel',
        iconClass: 'ti ti-trash fs-24 text-danger',
      },
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.deleteGateWay(paymentConfigId);
      }
    });
  }

  deleteGateWay(paymentConfigId: number): void {
    this.commonService.spinnerShow();
    this.payGateService
      .deletePaymentGateway(paymentConfigId)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
        catchError((error) => {
          this.commonService.toastError(
            (error?.error?.error || error?.error?.ERROR) ??
              'Something went wrong while delete',
          );
          return EMPTY;
        }),
      )
      .subscribe((res) => {
        if (res.status == 200) {
          this.commonService.toastSuccess(res?.message);

          if (
            this.totalData % this.pageSize === 1 &&
            this.totalData > this.pageSize
          ) {
            const previousPageSkip =
              (Math.ceil(this.totalData / this.pageSize) - 2) * this.pageSize;
            this.lastPayload = ''; 
            this.pagination.tablePageSize.next({
              skip: previousPageSkip,
              limit: this.pageSize,
              pageSize: this.pageSize,
            });
          } else {
            this.lastPayload = ''; 
            this.pagination.tablePageSize.next({
              skip: 0,
              limit: this.pageSize,
              pageSize: this.pageSize,
            });
          }
        } else {
          this.commonService.toastError(res?.message);
        }
      });
  }

  openAddEdit(item: IPaymentGatewayExtend | any = {}): void {
    const { isActiveControl, ...data } = item as IPaymentGatewayExtend;
    this.router.navigate([routes.paymentGateway + '/add-edit'], {
      state: {
        ...(data ? data : {}),
        isAddEdit: true,
      },
    });
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.destroySub$.next();
    this.destroySub$.complete();
  }
}
