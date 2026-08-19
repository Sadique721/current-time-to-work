import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import {
  FormsModule,
  ReactiveFormsModule,
  UntypedFormControl,
  UntypedFormGroup,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import {
  catchError,
  EMPTY,
  filter,
  finalize,
  iif,
  of,
  Subject,
  takeUntil,
} from 'rxjs';
import {
  CommonService,
  routes,
  sharedModule,
  status,
} from 'src/app/core.index';
import { PaymentGatewayService } from '../payment-gateway.service';
import {
  IpaymentConfigMapping,
  IPaymentGateway,
  IPaymentGatewayType,
} from '../payment-gateway.interface';
import { CustomElementModule } from 'src/app/core/shared/custom-elements/custom-elemets.module';
import { CommonModule } from '@angular/common';
import { AppRoutingModule } from 'src/app/app-routing.module';
import { WhiteeSpaceValidator } from 'src/app/core/shared/custom-validations/white-space.validator';

@Component({
  selector: 'app-payment-gateway-add-edit',
  imports: [
    sharedModule,
    CustomElementModule,
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
  ],
  templateUrl: './payment-gateway-add-edit.component.html',
  styleUrl: './payment-gateway-add-edit.component.scss',
})
export class PaymentGatewayAddEditComponent implements OnInit, OnDestroy {
  commonFG: UntypedFormGroup;
  private destroy$ = new Subject<void>();
  statusOptions = status;
  data: IPaymentGateway | any = {};
  gatewayConfiFG: UntypedFormGroup;
  systemConfiFG: UntypedFormGroup;
  listOfPaymentGateway: IPaymentGatewayType[] = [];
  paymentConfigMappingList: IpaymentConfigMapping[] = [];
  isLoading: boolean = true;

  constructor(
    private common: CommonService,
    private payGateService: PaymentGatewayService,
    private router: Router,
  ) {
    const nav = this.router.getCurrentNavigation();
    this.data = nav ? nav?.extras?.state : null;

    this.commonFG = new UntypedFormGroup({
      paymentConfigName: new UntypedFormControl(null),
      paymentGatewayInfo: new UntypedFormControl(null),
    });

    this.gatewayConfiFG = new UntypedFormGroup({});
    this.systemConfiFG = new UntypedFormGroup({});
  }

  ngOnInit(): void {
    if (this.data == null || !this.data?.isAddEdit) {
      this.router.navigateByUrl(routes.paymentGateway, {
        replaceUrl: true,
      });
    }

    this.getListOfCommonPaymentGatewaty();

    if (this.data && this.data?.paymentConfigId) {
      const { paymentGatewayInfo, paymentConfigName, ...rest } = this.data;
      this.commonFG.patchValue({
        paymentConfigName,
        paymentGatewayInfo,
      });

      this.commonFG.get('paymentConfigName')?.disable();

      this.getPaymentGatewayParameterById(
        this.commonFG.get('paymentConfigName')?.value,
      );
    } else {
      this.commonFG
        .get('paymentConfigName')
        ?.valueChanges.pipe(
          takeUntil(this.destroy$),
          filter((res) => !!res.trim()),
        )
        .subscribe((res) => {
          this.getPaymentGatewayParameterById(res);
        });
    }
  }

  private getPaymentGatewayParameterById(name: string): void {
    this.common.spinnerShow();
    this.payGateService
      .getPaymentGatewayParameterById(name)
      .pipe(
        finalize(() => {
          this.common.spinnerHide();
        }),
        catchError((error) => {
          this.common.toastError(
            error?.error?.msg ||
              error?.error?.ERROR ||
              'Something went wrong while fetching data',
          );

          return of({ paymentConfig: { paymentConfigMappingList: [] } });
        }),
        takeUntil(this.destroy$),
      )
      .subscribe((data) => {
        this.createForm(data?.paymentConfig?.paymentConfigMappingList || []);

        if (data?.status != 200) {
          this.common.toastError(data?.responseMessage || data?.message);
        }
      });
  }

  private getListOfCommonPaymentGatewaty(): void {
    this.common.spinnerShow();
    this.payGateService
      .getListOfCommonPaymentGatewaty()
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.common.toastError(
            error?.error?.msg ||
              error?.error?.ERROR ||
              'Something went wrong while fetching data',
          );

          return of({ dataList: [] });
        }),
        finalize(() => {
          this.common.spinnerHide();
        }),
      )
      .subscribe((res) => {
        this.listOfPaymentGateway = res?.dataList || [];

        if (res.responseCode != 200) {
          this.common.toastError(res?.responseMessage || res?.message);
        }
      });
  }

  private createForm(data: IpaymentConfigMapping[]): void {
    this.paymentConfigMappingList = data;

    this.paymentConfigMappingList.forEach((i) => {
      const control = new UntypedFormControl(
        this.findValue(i?.paymentParameterName),
        [Validators.required, WhiteeSpaceValidator.cannotContainSpace],
      );

      if (i.paymentParameterFor == 'SYSTEM') {
        this.systemConfiFG.addControl(i.paymentParameterName, control);
      } else {
        this.gatewayConfiFG.addControl(i.paymentParameterName, control);
      }
    });

    this.isLoading = false;
  }

  private findValue(name: string): string | null {
    return (
      this.data?.paymentConfigMappingList?.find(
        (f: IpaymentConfigMapping) => f.paymentParameterName == name,
      )?.paymentParameterValue ?? ''
    );
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  @HostListener('window:beforeunload', ['$event'])
  handleBeforeUnload(event: BeforeUnloadEvent) {
    if (this.data && this.data?.isAddEdit) {
      event.preventDefault();
    } else {
      this.onClose();
    }
  }

  canDeactivate(): boolean {
    if (this.data == null || !this.data?.isAddEdit) return true;

    const confirmLeave = window.confirm(
      'Changes will be lost. Do you want to go back?',
    );

    if (!confirmLeave) {
      this.common.spinnerHide();
    }

    return confirmLeave;
  }

  onClose() {
    this.data?.isAddEdit ? (this.data.isAddEdit = false) : null;
    this.router.navigateByUrl(routes.paymentGateway, {
      replaceUrl: true,
    });
  }

  submit(): void {
    if (
      !(
        this.commonFG.valid &&
        this.gatewayConfiFG.valid &&
        this.systemConfiFG.valid
      )
    ) {
      this.commonFG.markAllAsTouched();
      this.gatewayConfiFG.markAllAsTouched();
      this.systemConfiFG.markAllAsTouched();
      return;
    }
    const commonFGValue = this.commonFG.getRawValue();
    const gatewayConfiFGValue = this.gatewayConfiFG.value;
    const systemConfiFGValue = this.systemConfiFG.value;
    const payload = { ...commonFGValue, paymentConfigMappingList: [] };

    if (this.data?.paymentConfigId) {
      payload.paymentConfigId = this.data.paymentConfigId;
    }

    this.paymentConfigMappingList.forEach((i) => {
      let paymentParameterValue;
      if (i.paymentParameterFor == 'SYSTEM') {
        paymentParameterValue = systemConfiFGValue[i.paymentParameterName];
      } else {
        paymentParameterValue = gatewayConfiFGValue[i.paymentParameterName];
      }

      payload.paymentConfigMappingList.push({
        paymentParameterName: i.paymentParameterName,
        paymentParameterValue,
      });
    });

    this.common.spinnerShow();
    iif(
      () => this.data.paymentConfigId,
      this.payGateService.updatePaymentGateway(payload),
      this.payGateService.createPaymentGateway(payload),
    )
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.common.spinnerHide();
        }),
        catchError((error) => {
          this.common.toastError(
            error?.error?.ERROR ||
              error?.error?.msg ||
              error?.error?.error ||
              'Something went wrong!',
          );
          return EMPTY;
        }),
      )
      .subscribe((res) => {
        if (res.status == 200) {
          this.common.toastSuccess(res?.message);
          this.onClose();
        } else {
          this.common.toastError(res?.message);
        }
      });
  }
}
