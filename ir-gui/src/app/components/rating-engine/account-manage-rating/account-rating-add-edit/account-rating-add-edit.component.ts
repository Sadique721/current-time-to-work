import { Component, OnDestroy, OnInit } from "@angular/core";
import { UntypedFormControl, UntypedFormGroup, Validators } from "@angular/forms";
import { Router } from "@angular/router";
import { Subject, takeUntil, throwError } from "rxjs";
import { catchError, finalize } from "rxjs/operators";
import { routes, CommonService, SidebarService } from "src/app/core.index";
import { AccountManageService } from "../account-manage-rating.service";

@Component({
  selector: "app-account-rating-add-edit",
  templateUrl: "./account-rating-add-edit.component.html",
  styleUrl: "./account-rating-add-edit.component.scss",
  standalone: false,
})
export class AccountRatingAddEditComponent implements OnInit, OnDestroy {
  routes = routes;
  destroy$ = new Subject<void>();
  isCollapsed = false;
  selectedAccountRating: any = null;

  accountForm = new UntypedFormGroup({
    accountCode: new UntypedFormControl("", [Validators.required]),
    partnerId: new UntypedFormControl(null, [Validators.required]),
    partnerType: new UntypedFormControl("", [Validators.required]),
    productPlanId: new UntypedFormControl(null, [Validators.required]),
  });

  partnerListData: any[] = [];
  productPlansListData: any[] = [];
  isLoadingPartners = false;
  isLoadingPlans = false;
  isBothPartnerType = false;
  accountCodeLabel = "Account Code";
  private plansLoadedOnce = false;

  partnerTypeOptions = [
    { label: "CUSTOMER", value: "CUSTOMER" },
    { label: "VENDOR", value: "VENDOR" },
  ];

  constructor(
    private router: Router,
    private sidebar: SidebarService,
    private commonService: CommonService,
    private accountManageService: AccountManageService
  ) {}

  ngOnInit(): void {
    const navState: any =
      this.router.getCurrentNavigation()?.extras?.state || window.history.state;

    this.fetchAllPartners();

    if (navState?.id) {
      this.fetchAccountDetails(navState.id);
    }

    // React to partner selection
    this.accountForm.get("partnerId")?.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((id) => {
      if (id) {
        this.onPartnerChange(id);
      }
    });

    // React to manual partnerType selection (for BOTH case)
    this.accountForm.get("partnerType")?.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((type) => {
      if (type) {
        // Clear product plan selection whenever service type changes
        // But only if we've already done the initial load for edit mode
        if (this.plansLoadedOnce) {
          this.accountForm.patchValue({ productPlanId: null }, { emitEvent: false });
        }
        this.loadProductPlansByType(type);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get isEditMode(): boolean {
    return !!(this.selectedAccountRating && this.selectedAccountRating.accountId);
  }

  fetchAllPartners(): void {
    this.isLoadingPartners = true;
    this.accountManageService.getAllPartners()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.isLoadingPartners = false)
      )
      .subscribe({
        next: (data: any) => {
          this.partnerListData = Array.isArray(data) ? data : [];
          if (this.isEditMode && this.selectedAccountRating) {
            this.handlePartnerSelection(this.selectedAccountRating.partnerId, true);
          }
        },
        error: () => this.commonService.toastError("Failed to load partners.")
      });
  }

  onPartnerChange(partnerId: number): void {
    this.handlePartnerSelection(partnerId, false);
  }

  handlePartnerSelection(partnerId: number, isInitialLoad: boolean): void {
    const selectedPartner = this.partnerListData.find(p => p.partnerId === partnerId);
    if (!selectedPartner) {
      this.isBothPartnerType = false;
      if (!isInitialLoad) {
        this.accountForm.patchValue({ partnerType: '', productPlanId: null });
        this.productPlansListData = [];
      }
      return;
    }

    const type = selectedPartner.partnerType;
    this.plansLoadedOnce = false; // Reset whenever partner changes
    if (type === 'BOTH') {
      this.isBothPartnerType = true;
      this.accountForm.get('partnerType')?.enable();
      if (!isInitialLoad) {
        this.accountForm.patchValue({ partnerType: '', productPlanId: null });
      }
    } else {
      this.isBothPartnerType = false;
      this.accountForm.patchValue({ partnerType: type });
      this.accountForm.get('partnerType')?.disable();
      if (!isInitialLoad) {
        this.accountForm.patchValue({ productPlanId: null });
      }
    }

    // Dynamic Label & Read-only logic for Account Code / Home PLMN
    const lob = selectedPartner.lineOfBusiness;
    const accountCodeControl = this.accountForm.get('accountCode');

    if (lob === 'ROAMING') {
      this.accountCodeLabel = 'Home PLMN';
      accountCodeControl?.setValue(selectedPartner.hplmn || '');
      accountCodeControl?.disable();
    } else {
      this.accountCodeLabel = 'Account Code';
      if (!isInitialLoad) {
        accountCodeControl?.setValue('');
      }
      accountCodeControl?.enable();
    }
  }

  loadProductPlansByType(partnerType: string): void {
    this.isLoadingPlans = true;
    this.accountManageService.getProductPlansByType(partnerType)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.isLoadingPlans = false)
      )
      .subscribe({
        next: (data: any) => {
          this.productPlansListData = Array.isArray(data) ? data : [];
          // If editing, the valueChanges subscription or manual patch will pick up productPlanId
          if (this.isEditMode && this.selectedAccountRating?.productPlanId && !this.plansLoadedOnce) {
            this.accountForm.patchValue({ productPlanId: this.selectedAccountRating.productPlanId }, { emitEvent: false });
          }
          this.plansLoadedOnce = true;
        },
        error: () => this.commonService.toastError("Failed to load product plans.")
      });
  }

  fetchAccountDetails(id: number): void {
    this.commonService.spinnerShow();
    this.accountManageService.getById(id)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide())
      )
      .subscribe({
        next: (data: any) => {
          this.selectedAccountRating = data;
          
          const pType = data.accountType || data.partnerType;
          
          this.accountForm.patchValue({
             accountCode: data.accountCode,
             partnerId: data.partnerId,
             partnerType: pType,
             productPlanId: data.productPlanId
          }, { emitEvent: false });

          if (pType) {
            this.loadProductPlansByType(pType);
          }

          // After patching, if partnerList is already loaded, we handle selection logic
          if (this.partnerListData.length > 0) {
            this.handlePartnerSelection(data.partnerId, true);
          }
        },
        error: (err: any) => {
          this.commonService.toastError(err?.error?.msg || "Failed to load account details.");
        }
      });
  }

  submit(): void {
    if (this.accountForm.invalid) {
      this.accountForm.markAllAsTouched();
      return;
    }

    const raw = this.accountForm.getRawValue();
    const payload = {
      accountCode: raw.accountCode,
      partnerId: raw.partnerId,
      accountType: raw.partnerType,   // API expects accountType
      productPlanId: raw.productPlanId,
    };

    this.commonService.spinnerShow();
    const request = this.isEditMode
      ? this.accountManageService.putMethod(this.selectedAccountRating.accountId, payload)
      : this.accountManageService.postMethod(payload);

    request.pipe(
      takeUntil(this.destroy$),
      finalize(() => this.commonService.spinnerHide()),
      catchError((error) => throwError(() => error))
    ).subscribe({
      next: () => {
        this.commonService.toastSuccess(`Account rating ${this.isEditMode ? 'updated' : 'created'} successfully`);
        this.onClose();
      },
      error: (err: any) => {
        this.commonService.toastError(err?.error?.errorMessage ?? `Failed to ${this.isEditMode ? 'update' : 'create'} account rating.`);
      }
    });
  }

  onClose(): void {
    this.router.navigateByUrl(this.routes.ratingaccount, { replaceUrl: true });
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }
}
