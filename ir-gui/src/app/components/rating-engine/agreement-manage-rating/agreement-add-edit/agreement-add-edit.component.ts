import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from "@angular/core";
import {
  UntypedFormControl,
  UntypedFormGroup,
  Validators,
} from "@angular/forms";
import { catchError, finalize, Subject, takeUntil } from "rxjs";
import dayjs from 'dayjs';
import { CommonService, routes, SidebarService } from "src/app/core.index";
import { Router } from "@angular/router";
import { AgreementManageService } from "../agreement-manage-rating.service";

declare var bootstrap: any;

@Component({
  selector: "app-agreement-add-edit",
  templateUrl: "./agreement-add-edit.component.html",
  styleUrl: "./agreement-add-edit.component.scss",
  standalone: false,
})
export class AgreementAddEditComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  public routes = routes;
  selectedAgreement: any = null;

  previewMessage: string = '';
  disabledWeeklyDays: number[] = [];

  // ─── Wizard state ───────────────────────────────────────────────────────────
  currentStep = 1;
  isCollapsed = false;

  get shouldHideAccountSelection(): boolean {
    return this.isEditMode || this.basicForm?.get('lineOfBusiness')?.value === 'ROAMING';
  }

  get visibleSteps(): { label: string; stepNumber: number; isActive: boolean; isCompleted: boolean }[] {
    const lob = this.basicForm?.get('lineOfBusiness')?.value;
    const steps: { label: string; stepNumber: number; isActive: boolean; isCompleted: boolean }[] = [];
    
    // Step 1: Basic Info is always there
    steps.push({
      label: 'Basic Info',
      stepNumber: 1,
      isActive: this.currentStep === 1,
      isCompleted: this.currentStep > 1
    });

    if (lob !== 'ROAMING') {
      if (!this.isEditMode) {
        // Step 2: Select Accounts
        steps.push({
          label: 'Select Accounts',
          stepNumber: 2,
          isActive: this.currentStep === 2,
          isCompleted: this.currentStep > 2
        });
      }
      
      // Step 3: Configure
      steps.push({
        label: 'Configure',
        stepNumber: this.isEditMode ? 2 : 3,
        isActive: this.currentStep === 3,
        isCompleted: this.currentStep > 3
      });
      // Step 4: Settlement
      let settlementStepNum = this.isEditMode ? 3 : 4;
      
      steps.push({
        label: 'Settlement',
        stepNumber: settlementStepNum,
        isActive: this.currentStep === 4,
        isCompleted: false
      });
    }

    return steps;
  }

  // ─── Step 1 – Basic Info ─────────────────────────────────────────────────────
  basicForm: UntypedFormGroup;

  partnerList: { partnerName: string; partnerId: number }[] = [];

  // ─── Step 2 – Account Selection ──────────────────────────────────────────────
  availableAccounts: { accountCode: string; accountType: string; accountId: number }[] = [];
  selectedAccounts: Set<number> = new Set();
  isLoadingAccounts = false;
  accountSearchQuery = '';

  get filteredAccounts(): { accountCode: string; accountType: string; accountId: number }[] {
    const q = this.accountSearchQuery.toLowerCase().trim();
    if (!q) return this.availableAccounts;
    return this.availableAccounts.filter(
      (a) =>
        a.accountCode.toLowerCase().includes(q) ||
        a.accountType.toLowerCase().includes(q)
    );
  }

  // ─── Step 3 – Configure & Review ─────────────────────────────────────────────
  accountRows: {
    accountId: number | null;
    accountCode: string;
    accountType: string;
    invoiceFormat: string;
  }[] = [];

  invoiceFormatOptions = [
    { label: "XML", value: "XML" },
    { label: "PDF", value: "PDF" },
    { label: "CSV", value: "CSV" },
    { label: "JSON", value: "JSON" },
  ];

  accountTypeOptions = [
    { label: "CUSTOMER", value: "CUSTOMER" },
    { label: "VENDOR", value: "VENDOR" },
  ];

  lobOptions = [
    { label: "Interconnect", value: "INTERCONNECT" },
    { label: "Roaming", value: "ROAMING" },
  ];

  tapDirectionOptions = [
    { label: "TAP IN", value: "TAP_IN" },
    { label: "TAP OUT", value: "TAP_OUT" },
  ];

  billingTypeOptions = [
    { label: "DAYS", value: "DAYS" },
    { label: "WEEKLY", value: "WEEKLY" },
    { label: "FORTNIGHTLY", value: "FORTNIGHTLY" },
    { label: "MONTHLY", value: "MONTHLY" },
  ];

  weeklyDayOptions = [
    { label: "Sunday", value: "SUN" },
    { label: "Monday", value: "MON" },
    { label: "Tuesday", value: "TUE" },
    { label: "Wednesday", value: "WED" },
    { label: "Thursday", value: "THU" },
    { label: "Friday", value: "FRI" },
    { label: "Saturday", value: "SAT" },
  ];

  taxConfigList: any[] = [];            // legacy (kept for compatibility)
  isLoadingTaxConfigs: boolean = false;

  // ─── Tax Configuration Table (new spec) ──────────────────────────────────────
  availableTaxConfigs: any[] = [];       // fetched by customerCountryCode
  isLoadingAvailableTaxConfigs = false;

  /** Each row in the tax table */
  taxRows: {
    taxConfigControl: UntypedFormControl;  // drives custom-select
    applyOrder: number;                    // auto-calculated
    accumulateChecks: boolean[];           // one per previous row
  }[] = [];

  countryList: any[] = [];
  customerStateList: any[] = [];
  supplierStateList: any[] = [];

  readonly placeOfSupplyRuleOptions = [
    { label: "Billing Address", value: "BILLING_ADDRESS" },
    { label: "Recipient Location", value: "RECIPIENT_LOCATION" },
    { label: "Service Address", value: "SERVICE_ADDRESS" },
    { label: "Primary Use", value: "PRIMARY_USE" },
    { label: "Customer Location", value: "CUSTOMER_LOCATION" },
  ];

  // Step 4 Properties
  settlementForm!: UntypedFormGroup;
  invoiceTemplates: any[] = [];
  isLoadingTemplates: boolean = false;

  hasCustomer: boolean = false;
  hasVendor: boolean = false;
  hasBoth: boolean = false;

  constructor(
    private commonService: CommonService,
    private agreementService: AgreementManageService,
    private router: Router,
    private sidebar: SidebarService
  ) {
    this.basicForm = new UntypedFormGroup({
      agreementCode: new UntypedFormControl("", [Validators.required]),
      billingCycleStartDate: new UntypedFormControl(null, [Validators.required]),
      billingType: new UntypedFormControl("DAYS", [Validators.required]),
      billingCyclePeriod: new UntypedFormControl(null, [
        Validators.required,
        Validators.min(1),
      ]),
      weeklyDay: new UntypedFormControl(null),
      partnerId: new UntypedFormControl(null, [Validators.required]),
      lineOfBusiness: new UntypedFormControl("INTERCONNECT", [Validators.required]),
      homePlmn: new UntypedFormControl(""),
      visitorPlmn: new UntypedFormControl(""),
      tapDirection: new UntypedFormControl(null),
      isTaxExempt: new UntypedFormControl(false),
      roamingSettlementTemplateId: new UntypedFormControl(null),
    });

    this.settlementForm = new UntypedFormGroup({
      isIncomingSettlement: new UntypedFormControl(false),
      isOutgoingSettlement: new UntypedFormControl(false),
      isNetSettlement: new UntypedFormControl(false),
      incomingSettlementTemplateId: new UntypedFormControl(0),
      outgoingSettlementTemplateId: new UntypedFormControl(0),
      netSettlementTemplateId: new UntypedFormControl(0),
    });

    // Detection flags
    this.hasCustomer = false;
    this.hasVendor = false;
    this.hasBoth = false;

    this.invoiceTemplates = [];
    this.isLoadingTemplates = false;

    // Dynamic validation for templates
    ['isIncomingSettlement', 'isOutgoingSettlement', 'isNetSettlement'].forEach(key => {
      const templateKey = key.replace('is', '').charAt(0).toLowerCase() + key.replace('is', '').slice(1) + 'TemplateId';
      this.settlementForm.get(key)?.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(enabled => {
        const ctrl = this.settlementForm.get(templateKey);
        if (enabled) {
          ctrl?.setValidators([Validators.required, Validators.min(1)]);
        } else {
          ctrl?.clearValidators();
        }
        ctrl?.updateValueAndValidity();
      });
    });

    // Dynamic validation for Tax fields based on isTaxExempt
    this.basicForm.get('isTaxExempt')?.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(isExempt => {
      if (isExempt) {
        this.taxRows = [];
      }
    });

    // Dynamic validation for Billing Type
    this.basicForm.get('billingType')?.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(billingType => {
      this.updateBillingTypeValidation(billingType);
    });

    // Dynamic handling of Weekly Day changes to disable days in the calendar
    this.basicForm.get('weeklyDay')?.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(day => {
      const billingType = this.basicForm.get('billingType')?.value;
      if (billingType === 'WEEKLY') {
         this.updateDisabledDays(day);
      }
    });

    this.basicForm.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.generatePreviewMessage();
    });
  }

  ngOnInit(): void {
    const navState: any = this.router.getCurrentNavigation()?.extras?.state || window.history.state;
    if (navState?.id) {
      this.fetchAgreementDetails(navState.id);
    } else {
      this.fetchInvoiceTemplates();
    }

    // Fetch initial data
    this.fetchPartners();
    this.fetchTaxConfigs();

    this.basicForm.get('lineOfBusiness')?.valueChanges.pipe(takeUntil(this.destroy$)).subscribe((lob) => {
      this.updateLOBValidation(lob);
      this.fetchInvoiceTemplates();
    });

    if (this.isEditMode) {
      this.patchForm();
    } else {
      this.basicForm.get('partnerId')?.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
        this.selectedAccounts = new Set();
        this.accountRows = [];
      });
    }

    // Trigger initial validation check
    const currentLob = this.basicForm.get('lineOfBusiness')?.value;
    if (currentLob) {
      this.updateLOBValidation(currentLob);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get isEditMode(): boolean {
    return !!(this.selectedAgreement && this.selectedAgreement.agreementId);
  }

  private fetchPartners(): void {
    this.agreementService
      .getPartners()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.partnerList = Array.isArray(res) ? res : (res.content || []);
        },
        error: () => this.commonService.toastError("Failed to load partner list"),
      });
  }

  goToStep2(): void {
    if (this.basicForm.invalid) {
      console.log('Form Errors:', this.getFormValidationErrors());
      this.basicForm.markAllAsTouched();
      return;
    }

    if (!this.isTaxTableValid) {
      if (this.taxRows.length === 0) {
        this.commonService.toastError("At least one Applied Tax is required.");
      } else {
        this.commonService.toastError("Please complete all Applied Tax rows.");
      }
      return;
    }

    const lob = this.basicForm.get("lineOfBusiness")?.value;

    if (this.isEditMode) {
      if (lob === 'ROAMING') {
        this.detectSettlementRequirements();
        this.fetchInvoiceTemplates();
        this.currentStep = 4;
      } else {
        this.currentStep = 3;
      }
    } else {
      if (!this.basicForm.get("partnerId")?.value) {
        this.basicForm.get("partnerId")?.markAsTouched();
        this.commonService.toastError("Please select a partner.");
        return;
      }
      const partnerId = this.basicForm.get("partnerId")?.value;
      if (lob === 'ROAMING') {
        this.fetchAccountsForRoaming(partnerId);
      } else {
        this.currentStep = 2;
        this.fetchAccountsByPartner(partnerId);
      }
    }
  }

  goToStep3(): void {
    if (this.selectedAccounts.size === 0) {
      this.commonService.toastError("Please select at least one account.");
      return;
    }
    this.accountRows = Array.from(this.selectedAccounts).map((id) => {
      const existing = this.accountRows.find((r) => r.accountId === id);
      const available = this.availableAccounts.find((a) => a.accountId === id);
      return {
        accountId: id,
        accountCode: available?.accountCode ?? existing?.accountCode ?? "",
        accountType: available?.accountType ?? existing?.accountType ?? "CUSTOMER",
        invoiceFormat: existing?.invoiceFormat ?? "PDF",
      };
    });
    this.currentStep = 3;
  }

  goToStep4(): void {
    if (this.accountRows.length === 0) {
      this.commonService.toastError("No accounts configured.");
      return;
    }

    this.detectSettlementRequirements();
    this.currentStep = 4;
    this.fetchInvoiceTemplates();
  }

  private detectSettlementRequirements(skipPatching = false): void {
    this.hasCustomer = this.accountRows.some(r => r.accountType === 'CUSTOMER');
    this.hasVendor = this.accountRows.some(r => r.accountType === 'VENDOR');
    this.hasBoth = this.hasCustomer && this.hasVendor;

    const settlementForm = this.settlementForm;

    // Reset all to disabled initially
    settlementForm.get('isIncomingSettlement')?.disable();
    settlementForm.get('isOutgoingSettlement')?.disable();
    settlementForm.get('isNetSettlement')?.disable();

    if (this.hasBoth) {
      if (!skipPatching) settlementForm.patchValue({ isIncomingSettlement: true, isOutgoingSettlement: true });
      settlementForm.get('isIncomingSettlement')?.disable(); // Mandatory
      settlementForm.get('isOutgoingSettlement')?.disable(); // Mandatory
      settlementForm.get('isNetSettlement')?.enable();       // Optional Choice
    } else if (this.hasCustomer) {
      if (!skipPatching) settlementForm.patchValue({ isIncomingSettlement: true, isOutgoingSettlement: false, isNetSettlement: false });
      settlementForm.get('isIncomingSettlement')?.disable();
      settlementForm.get('isOutgoingSettlement')?.disable();
      settlementForm.get('isNetSettlement')?.disable();
    } else if (this.hasVendor) {
      if (!skipPatching) settlementForm.patchValue({ isIncomingSettlement: false, isOutgoingSettlement: true, isNetSettlement: false });
      settlementForm.get('isIncomingSettlement')?.disable();
      settlementForm.get('isOutgoingSettlement')?.disable();
      settlementForm.get('isNetSettlement')?.disable();
    }

    // Force validation update
    ['incomingSettlementTemplateId', 'outgoingSettlementTemplateId', 'netSettlementTemplateId'].forEach(key => {
      this.settlementForm.get(key)?.updateValueAndValidity();
    });
  }

  goBack(): void {
    if (this.currentStep > 1) {
      const lob = this.basicForm.get('lineOfBusiness')?.value;
      if (lob === 'ROAMING') {
        this.currentStep = 1;
      } else if (this.isEditMode && this.currentStep === 3) {
        this.currentStep = 1;
      } else {
        this.currentStep--;
      }
    }
  }

  private fetchAccountsForRoaming(partnerId: number): void {
    this.commonService.spinnerShow();
    this.agreementService.getAccountsByPartnerId(partnerId)
      .pipe(takeUntil(this.destroy$), finalize(() => this.commonService.spinnerHide()))
      .subscribe({
        next: (res: any) => {
          const accounts = Array.isArray(res) ? res : [];
          if (accounts.length === 0) {
            this.commonService.toastError("No accounts found for the selected partner.");
            return;
          }
          this.availableAccounts = accounts;
          this.selectedAccounts = new Set(accounts.map((a: any) => a.accountId));
          this.accountRows = accounts.map((a: any) => ({
            accountId: a.accountId,
            accountCode: a.accountCode,
            accountType: a.accountType || "CUSTOMER",
            invoiceFormat: "PDF"
          }));
          this.detectSettlementRequirements();
          this.fetchInvoiceTemplates();
          this.currentStep = 4;
        },
        error: () => this.commonService.toastError("Failed to load accounts"),
      });
  }

  private fetchAccountsByPartner(partnerId: number): void {
    this.isLoadingAccounts = true;
    this.availableAccounts = [];
    this.accountSearchQuery = '';
    this.agreementService.getAccountsByPartnerId(partnerId)
      .pipe(takeUntil(this.destroy$), finalize(() => (this.isLoadingAccounts = false)))
      .subscribe({
        next: (res: any) => this.availableAccounts = Array.isArray(res) ? res : [],
        error: () => this.commonService.toastError("Failed to load accounts"),
      });
  }

  private fetchInvoiceTemplates(): void {
    this.isLoadingTemplates = true;
    const lob = this.basicForm.get('lineOfBusiness')?.value || null;
    this.agreementService.getTemplateIds(lob)
      .pipe(takeUntil(this.destroy$), finalize(() => (this.isLoadingTemplates = false)))
      .subscribe({
        next: (res: any) => this.invoiceTemplates = Array.isArray(res) ? res : [],
        error: () => this.commonService.toastError("Failed to load invoice templates"),
      });
  }

  private fetchTaxConfigs(): void {
    this.isLoadingTaxConfigs = true;
    this.isLoadingAvailableTaxConfigs = true;
    this.agreementService.getTaxConfigs()
      .pipe(takeUntil(this.destroy$), finalize(() => {
        this.isLoadingTaxConfigs = false;
        this.isLoadingAvailableTaxConfigs = false;
      }))
      .subscribe({
        next: (res: any) => {
          this.taxConfigList = Array.isArray(res) ? res : (res.content || []);
          this.availableTaxConfigs = this.taxConfigList;
        },
        error: () => this.commonService.toastError("Failed to load tax configurations"),
      });
  }

  private fetchAgreementDetails(id: number): void {
    this.commonService.spinnerShow();
    this.agreementService.getById(id)
      .pipe(takeUntil(this.destroy$), finalize(() => this.commonService.spinnerHide()))
      .subscribe({
        next: (res: any) => {
          this.selectedAgreement = res.content || res;
          this.patchForm();
        },
        error: () => this.commonService.toastError("Failed to load agreement details"),
      });
  }

  private patchForm(): void {
    if (!this.selectedAgreement) return;

    this.basicForm.patchValue({
      agreementCode: this.selectedAgreement.agreementCode,
      billingCycleStartDate: this.selectedAgreement.billingCycleStartDate
        ? new Date(this.selectedAgreement.billingCycleStartDate)
        : null,
      billingType: this.selectedAgreement.billingType || 'DAYS',
      billingCyclePeriod: this.selectedAgreement.billingCyclePeriod,
      weeklyDay: this.selectedAgreement.weeklyDay || null,
      lineOfBusiness: this.selectedAgreement.lineOfBusiness || "INTERCONNECT",
      homePlmn: this.selectedAgreement.homePlmn || "",
      visitorPlmn: this.selectedAgreement.visitorPlmn || "",
      tapDirection: this.selectedAgreement.tapDirection || null,
      partnerId: this.selectedAgreement.partnerId || null,
      isTaxExempt: this.selectedAgreement.isTaxExempt || false,
      roamingSettlementTemplateId: this.selectedAgreement.lineOfBusiness === 'ROAMING'
        ? this.selectedAgreement.outgoingSettlementTemplateId
        : null,
    }, { emitEvent: false }); // Disable events to prevent clearing taxRows via subscriptions

    // Sync billing validation manually because emitEvent is false
    this.updateBillingTypeValidation(this.basicForm.get('billingType')?.value);

    this.basicForm.get('agreementCode')?.disable();
    this.basicForm.get('lineOfBusiness')?.disable();

    if (!this.selectedAgreement.partnerId) {
      this.basicForm.get('partnerId')?.clearValidators();
      this.basicForm.get('partnerId')?.updateValueAndValidity();
    }

    // Populate Account Rows
    const existingAccounts: any[] = this.selectedAgreement.accountAgreements || [];
    this.selectedAccounts = new Set(existingAccounts.map((a: any) => a.accountId).filter(Boolean));
    this.accountRows = existingAccounts.map((a: any) => ({
      accountId: a.accountId ?? null,
      accountCode: a.accountCode,
      accountType: a.accountType,
      invoiceFormat: a.invoiceFormat,
    }));

    // Populate Tax Rows from taxConfigs
    if (!this.selectedAgreement.isTaxExempt && this.selectedAgreement.taxConfigs) {
      this.taxRows = this.selectedAgreement.taxConfigs.map((tc: any, index: number) => {
        const control = new UntypedFormControl(tc.taxConfigId, Validators.required);
        
        // Map accumulateFromOrders (e.g. "1,2") to boolean array
        const accumulateChecks = Array(index).fill(false);
        if (tc.accumulateFromOrders && index > 0) {
          const orders = String(tc.accumulateFromOrders).split(',').map(s => parseInt(s.trim()));
          orders.forEach(order => {
            if (order > 0 && order <= index) {
              accumulateChecks[order - 1] = true;
            }
          });
        }

        return {
          taxConfigControl: control,
          applyOrder: tc.applyOrder || (index + 1),
          accumulateChecks: accumulateChecks
        };
      });
    }



    this.settlementForm.patchValue({
      isIncomingSettlement: this.selectedAgreement.isIncomingSettlement ?? false,
      isOutgoingSettlement: this.selectedAgreement.isOutgoingSettlement ?? false,
      isNetSettlement: this.selectedAgreement.isNetSettlement ?? false,
      incomingSettlementTemplateId: this.selectedAgreement.incomingSettlementTemplateId ?? null,
      outgoingSettlementTemplateId: this.selectedAgreement.outgoingSettlementTemplateId ?? null,
      netSettlementTemplateId: this.selectedAgreement.netSettlementTemplateId ?? null,
    });

    this.detectSettlementRequirements(true);
    this.updateLOBValidation(this.selectedAgreement.lineOfBusiness || "INTERCONNECT");
    this.fetchInvoiceTemplates();
  }





  /**
   * Returns filtered tax configs for a given row index:
   *  - Row 0: only applyOn === 'BASE'
   *  - Rows 1+: only applyOn === 'CUMULATIVE'
   *  - Already-selected configs in other rows are excluded
   */
  getFilteredTaxConfigs(rowIndex: number): any[] {
    const selectedIds = this.taxRows
      .filter((_, i) => i !== rowIndex)
      .map(r => r.taxConfigControl.value)
      .filter(v => v !== null);

    return this.availableTaxConfigs.filter(tc => {
      if (selectedIds.includes(tc.taxConfigId)) return false;
      if (rowIndex === 0) return tc.applyOn === 'BASE';
      return tc.applyOn === 'CUMULATIVE';
    });
  }

  /** Returns the full tax config object for a row (for displaying rate/applyOn) */
  getSelectedTaxConfig(row: any): any | null {
    const id = row.taxConfigControl.value;
    if (id === null) return null;
    return this.availableTaxConfigs.find(tc => tc.taxConfigId === id) || null;
  }

  addTaxRow(): void {
    const newOrder = this.taxRows.length + 1;
    this.taxRows.push({
      taxConfigControl: new UntypedFormControl(null),
      applyOrder: newOrder,
      accumulateChecks: new Array(newOrder - 1).fill(false),
    });
  }

  removeTaxRow(index: number): void {
    const wasFirstRow = index === 0;
    this.taxRows.splice(index, 1);
    // Re-calculate applyOrder and reset accumulateChecks
    this.taxRows.forEach((row, i) => {
      row.applyOrder = i + 1;
      row.accumulateChecks = new Array(i).fill(false);
    });
    // If row 1 was removed, the new row 1 might now be CUMULATIVE but should be BASE
    // → clear its selection since the filtered options will change
    if (wasFirstRow && this.taxRows.length > 0) {
      this.taxRows[0].taxConfigControl.setValue(null);
    }
  }

  /** Convert taxRows to the API payload shape */
  buildTaxConfigPayload(): { taxConfigId: number; applyOrder: number; accumulateFromOrders: string | null }[] {
    return this.taxRows.map((row, i) => {
      const accumulateFromOrders = i === 0
        ? null
        : row.accumulateChecks
            .map((checked, idx) => checked ? String(idx + 1) : null)
            .filter(Boolean)
            .join(',') || null;
      return {
        taxConfigId: row.taxConfigControl.value,
        applyOrder: row.applyOrder,
        accumulateFromOrders,
      };
    });
  }

  /** Per-row accumulate validation error (only after a tax config is selected) */
  getAccumulateError(rowIndex: number): string | null {
    if (rowIndex === 0) return null;
    const row = this.taxRows[rowIndex];
    if (!row || row.taxConfigControl.value === null) return null;
    if (row.accumulateChecks.some(c => c)) return null;
    return `Row ${rowIndex + 1}: At least one prior order must be selected`;
  }

  get isTaxTableValid(): boolean {
    if (this.basicForm.get('isTaxExempt')?.value) return true;
    if (this.taxRows.length === 0) return false;
    // Every row must have a selected config
    if (!this.taxRows.every(r => r.taxConfigControl.value !== null)) return false;
    // Rows 2+ must have at least one accumulate checkbox checked (only when config is selected)
    for (let i = 1; i < this.taxRows.length; i++) {
      if (this.taxRows[i].taxConfigControl.value !== null && !this.taxRows[i].accumulateChecks.some(c => c)) return false;
    }
    return true;
  }

  toggleAccount(id: number): void {
    if (this.selectedAccounts.has(id)) {
      this.selectedAccounts.delete(id);
    } else {
      this.selectedAccounts.add(id);
    }
  }

  isAccountSelected(id: number): boolean {
    return this.selectedAccounts.has(id);
  }

  removeAccountRow(index: number): void {
    const removed = this.accountRows.splice(index, 1)[0];
    if (!this.isEditMode && removed.accountId) {
      this.selectedAccounts.delete(removed.accountId);
    }
    if (this.accountRows.length === 0 && !this.isEditMode) this.currentStep = 2;
  }

  updateLOBValidation(lob: string): void {
    const tapDirectionCtrl = this.basicForm.get('tapDirection');
    const roamingSettlementCtrl = this.basicForm.get('roamingSettlementTemplateId');

    if (lob === 'ROAMING') {
      tapDirectionCtrl?.setValidators([Validators.required]);
      roamingSettlementCtrl?.setValidators([Validators.required]);
    } else {
      tapDirectionCtrl?.clearValidators();
      roamingSettlementCtrl?.clearValidators();
      if (tapDirectionCtrl?.value) {
        this.basicForm.patchValue({ tapDirection: null }, { emitEvent: false });
      }
      if (roamingSettlementCtrl?.value) {
        this.basicForm.patchValue({ roamingSettlementTemplateId: null }, { emitEvent: false });
      }
    }

    tapDirectionCtrl?.updateValueAndValidity();
    roamingSettlementCtrl?.updateValueAndValidity();
  }

  private formatDate(value: Date | string | null): string {
    if (!value) return "";
    const d = value instanceof Date ? value : new Date(value);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }

  private getPayload(): any {
    const basic = this.basicForm.getRawValue();
    const settlement = this.settlementForm.getRawValue();
    const isRoaming = basic.lineOfBusiness === 'ROAMING';

    return {
      agreementCode: basic.agreementCode,
      billingCycleStartDate: this.formatDate(basic.billingCycleStartDate),
      billingType: basic.billingType,
      billingCyclePeriod: basic.billingType === 'DAYS' ? Number(basic.billingCyclePeriod) : null,
      weeklyDay: basic.billingType === 'WEEKLY' ? basic.weeklyDay : null,
      lineOfBusiness: basic.lineOfBusiness,
      tapDirection: isRoaming ? basic.tapDirection : null,
      isTaxExempt: basic.isTaxExempt,
      isIncomingSettlement: isRoaming ? false : settlement.isIncomingSettlement,
      isOutgoingSettlement: isRoaming ? true : settlement.isOutgoingSettlement,
      isNetSettlement: isRoaming ? false : settlement.isNetSettlement,
      incomingSettlementTemplateId: isRoaming ? null : (settlement.isIncomingSettlement ? settlement.incomingSettlementTemplateId : null),
      outgoingSettlementTemplateId: isRoaming ? basic.roamingSettlementTemplateId : (settlement.isOutgoingSettlement ? settlement.outgoingSettlementTemplateId : null),
      netSettlementTemplateId: isRoaming ? null : (settlement.isNetSettlement ? settlement.netSettlementTemplateId : null),
      taxConfigs: basic.isTaxExempt ? [] : this.buildTaxConfigPayload(),
      accountAgreements: this.accountRows.map((row) => ({
        accountId: row.accountId,
        accountCode: row.accountCode,
        accountType: row.accountType,
        invoiceFormat: isRoaming ? 'PDF' : row.invoiceFormat,
      })),
    };
  }

  createAgreement(): void {
    if (this.basicForm.invalid) {
      this.basicForm.markAllAsTouched();
      return;
    }
    
    if (!this.validateWeeklyStartDate()) {
      return;
    }

    const lob = this.basicForm.get('lineOfBusiness')?.value;
    if (lob === 'ROAMING') {
      const partnerId = this.basicForm.get("partnerId")?.value;
      this.commonService.spinnerShow();
      this.agreementService.getAccountsByPartnerId(partnerId)
        .pipe(takeUntil(this.destroy$), finalize(() => this.commonService.spinnerHide()))
        .subscribe({
          next: (res: any) => {
            const accounts = Array.isArray(res) ? res : [];
            if (accounts.length === 0) {
              this.commonService.toastError("No accounts found for the selected partner.");
              return;
            }
            this.availableAccounts = accounts;
            this.selectedAccounts = new Set(accounts.map((a: any) => a.accountId));
            this.accountRows = accounts.map((a: any) => ({
              accountId: a.accountId,
              accountCode: a.accountCode,
              accountType: a.accountType || "CUSTOMER",
              invoiceFormat: "PDF"
            }));
            
            this.submitCreateAgreement();
          },
          error: () => this.commonService.toastError("Failed to load accounts"),
        });
    } else {
      if (this.settlementForm.invalid) {
        this.settlementForm.markAllAsTouched();
        return;
      }
      this.submitCreateAgreement();
    }
  }

  private submitCreateAgreement(): void {
    const payload = this.getPayload();
    this.commonService.spinnerShow();
    this.agreementService.postMethod(payload)
      .pipe(takeUntil(this.destroy$), finalize(() => this.commonService.spinnerHide()), catchError((err) => {
        this.commonService.toastError(err?.error?.errorMessage || err?.error?.msg || "Error creating agreement");
        throw err;
      }))
      .subscribe(() => {
        this.commonService.toastSuccess("Agreement created successfully");
        this.onClose(true);
      });
  }

  updateAgreement(): void {
    if (this.basicForm.invalid) {
      this.basicForm.markAllAsTouched();
      return;
    }
    if (!this.validateWeeklyStartDate()) {
      return;
    }
    const lob = this.basicForm.get('lineOfBusiness')?.value;
    if (lob !== 'ROAMING' && this.settlementForm.invalid) {
      this.settlementForm.markAllAsTouched();
      return;
    }

    const payload = this.getPayload();
    this.commonService.spinnerShow();
    this.agreementService.putMethod(this.selectedAgreement.agreementId, payload)
      .pipe(takeUntil(this.destroy$), finalize(() => this.commonService.spinnerHide()), catchError((err) => {
        this.commonService.toastError(err?.error?.errorMessage || err?.error?.msg || "Error updating agreement");
        throw err;
      }))
      .subscribe(() => {
        this.commonService.toastSuccess("Agreement updated successfully");
        this.onClose(true);
      });
  }

  onClose(isReload: boolean = false): void {
    this.router.navigate([this.routes.ratingagreement], {
      replaceUrl: true
    });
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  private getFormValidationErrors(): any {
    const errors: any = {};
    Object.keys(this.basicForm.controls).forEach(key => {
      const controlErrors = this.basicForm.get(key)?.errors;
      if (controlErrors != null) {
        errors[key] = controlErrors;
      }
    });
    return errors;
  }

  private validateWeeklyStartDate(): boolean {
    const billingType = this.basicForm.get('billingType')?.value;
    if (billingType === 'WEEKLY') {
      const selectedDay = this.basicForm.get('weeklyDay')?.value;
      const startDateValue = this.basicForm.get('billingCycleStartDate')?.value;
      if (selectedDay && startDateValue) {
        const d = new Date(startDateValue);
        const daysMap: { [key: string]: number } = {
          'SUN': 0, 'MON': 1, 'TUE': 2, 'WED': 3, 'THU': 4, 'FRI': 5, 'SAT': 6
        };
        if (d.getDay() !== daysMap[selectedDay]) {
          this.commonService.toastError(`Billing Cycle Start Date must be a ${selectedDay} for WEEKLY billing.`);
          return false;
        }
      }
    }
    return true;
  }

  private updateBillingTypeValidation(billingType: string): void {
    const periodCtrl = this.basicForm.get('billingCyclePeriod');
    const dayCtrl = this.basicForm.get('weeklyDay');

    if (billingType === 'DAYS') {
      periodCtrl?.setValidators([Validators.required, Validators.min(1)]);
      dayCtrl?.clearValidators();
      this.updateDisabledDays(null); // Reset disabled days
    } else if (billingType === 'WEEKLY') {
      dayCtrl?.setValidators([Validators.required]);
      periodCtrl?.clearValidators();
      this.updateDisabledDays(dayCtrl?.value);
    } else {
      // FORTNIGHTLY, MONTHLY
      periodCtrl?.clearValidators();
      dayCtrl?.clearValidators();
      this.updateDisabledDays(null); // Reset disabled days
    }
    
    periodCtrl?.updateValueAndValidity();
    dayCtrl?.updateValueAndValidity();
  }

  private updateDisabledDays(day: string | null | undefined): void {
    const allDays = [0, 1, 2, 3, 4, 5, 6];
    const daysMap: { [key: string]: number } = {
      'SUN': 0, 'MON': 1, 'TUE': 2, 'WED': 3, 'THU': 4, 'FRI': 5, 'SAT': 6
    };
    
    if (day && daysMap[day] !== undefined) {
      const allowedDay = daysMap[day];
      this.disabledWeeklyDays = [...allDays.filter(d => d !== allowedDay)];
      
      // Clear the current start date if it doesn't match the new weekly day
      const currentDate = this.basicForm.get('billingCycleStartDate')?.value;
      if (currentDate) {
        const d = new Date(currentDate);
        if (d.getDay() !== allowedDay) {
          this.basicForm.get('billingCycleStartDate')?.setValue(null);
          this.commonService.toastError(`Start date cleared because it must be a ${day}`);
        }
      }
    } else {
      this.disabledWeeklyDays = []; // reset disabled days if no day is selected or if we switch back to DAYS/MONTHLY
    }
  }

  private generatePreviewMessage(): void {
    const type = this.basicForm.get('billingType')?.value;
    const startDateVal = this.basicForm.get('billingCycleStartDate')?.value;
    
    if (!type || !startDateVal) {
      this.previewMessage = '';
      return;
    }

    const start = dayjs(startDateVal);
    let end: dayjs.Dayjs;
    let nextInvoice: dayjs.Dayjs;

    if (type === 'DAYS') {
      const period = Number(this.basicForm.get('billingCyclePeriod')?.value);
      if (!period || period <= 0) {
        this.previewMessage = '';
        return;
      }
      end = start.add(period - 1, 'day');
      nextInvoice = end.add(1, 'day');
    } else if (type === 'WEEKLY') {
      end = start.add(6, 'day');
      nextInvoice = end.add(1, 'day');
    } else if (type === 'FORTNIGHTLY') {
      end = start.add(14, 'day'); // Fortnight is 14 days. e.g. 15th to 29th is 15 days? Wait. 15th to 28th is 14 days inclusive. 
      // User example: 2026-08-15 to 2026-08-29. 29 - 15 = 14 days difference (15 days inclusive). So start.add(14, 'day')
      end = start.add(14, 'day');
      nextInvoice = end.add(1, 'day');
    } else if (type === 'MONTHLY') {
      // User example: 2026-08-15 to 2026-09-14.
      end = start.add(1, 'month').subtract(1, 'day');
      nextInvoice = end.add(1, 'day');
    } else {
      this.previewMessage = '';
      return;
    }

    this.previewMessage = `First Cycle: ${start.format('YYYY-MM-DD')} to ${end.format('YYYY-MM-DD')}. Next invoice will generate on ${nextInvoice.format('YYYY-MM-DD')}.`;
  }
}
