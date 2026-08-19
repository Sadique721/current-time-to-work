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
import { catchError, finalize, Subject, takeUntil, throwError } from "rxjs";
import { CommonService } from "src/app/core.index";
import { WhiteeSpaceValidator } from "src/app/core/shared/custom-validations/white-space.validator";
import { PartnerManageService } from "../partner-manage-rating.service";
declare var bootstrap: any;

@Component({
  selector: "app-partner-rating-add-edit",
  templateUrl: "./partner-rating-add-edit.component.html",
  styleUrl: "./partner-rating-add-edit.component.scss",
  standalone: false,
})
export class PartnerRatingAddEditComponent implements OnInit, OnDestroy {
  partnerRatingForm: UntypedFormGroup;
  private destroy$ = new Subject<void>();
  @Output() close = new EventEmitter<boolean>();
  @Input() selectedPartnerRating: any = null;

  // ─── Dropdown options ────────────────────────────────────────────────────────
  partnerTypeOptions = [
    { label: "CUSTOMER", value: "CUSTOMER" },
    { label: "VENDOR", value: "VENDOR" },
    { label: "BOTH", value: "BOTH" },
  ];

  statusOptions = [
    { label: "ACTIVE", value: "ACTIVE" },
    { label: "INACTIVE", value: "INACTIVE" },
  ];

  lineOfBusinessOptions = [
    { label: "INTERCONNECT", value: "INTERCONNECT" },
    { label: "ROAMING", value: "ROAMING" },
  ];

  interconnectTypeOptions = [
    { label: "IP", value: "IP" },
    { label: "SS7", value: "SS7" },
    { label: "SIP", value: "SIP" },
  ];

  billingCycleOptions = [
    { label: "MONTHLY", value: "MONTHLY" },
    { label: "WEEKLY", value: "WEEKLY" },
  ];
  
  organizationList: any[] = [];
  clearingHouseList: any[] = [];
  countryList: any[] = [];
  currencyList: any[] = [];

  // ─── Conditional visibility ──────────────────────────────────────────────────
  get isInterconnectTypeIP(): boolean {
    return this.partnerRatingForm.get("interconnectType")?.value === "IP";
  }

  get isInterconnectTypeSS7(): boolean {
    return this.partnerRatingForm.get("interconnectType")?.value === "SS7";
  }

  get isRoaming(): boolean {
    return this.partnerRatingForm.get("lineOfBusiness")?.value === "ROAMING";
  }


  constructor(
    private commonservice: CommonService,
    private partnerManageService: PartnerManageService
  ) {
    this.partnerRatingForm = new UntypedFormGroup({
      // Basic Info
      partnerName: new UntypedFormControl("", [
        Validators.required,
        WhiteeSpaceValidator.cannotContainSpace,
      ]),
      partnerCode: new UntypedFormControl("", [
        Validators.required,
        WhiteeSpaceValidator.cannotContainSpace,
      ]),
      partnerType: new UntypedFormControl("", [Validators.required]),
      status: new UntypedFormControl("ACTIVE", [Validators.required]),
      organizationId: new UntypedFormControl(null, [Validators.required]),
      lineOfBusiness: new UntypedFormControl("", [Validators.required]),
      tadigCode: new UntypedFormControl(""), // Moved to Basic Info

      // Contact & Address
      contactPersonName: new UntypedFormControl("", [Validators.required]),
      email: new UntypedFormControl("", [Validators.required, Validators.email]),
      phoneNumber: new UntypedFormControl("", [Validators.required]),
      addressLine1: new UntypedFormControl("", [Validators.required]),
      city: new UntypedFormControl("", [Validators.required]),
      postalCode: new UntypedFormControl("", [Validators.required]),
      country: new UntypedFormControl("", [Validators.required]),

      // Technical
      interconnectType: new UntypedFormControl("", [Validators.required]),
      ipAddress: new UntypedFormControl(""),    // shown only when IP
      pointCode: new UntypedFormControl(""),    // Section: Interconnect Settings, Optional
      routingPrefix: new UntypedFormControl(""),
      hplmn: new UntypedFormControl(""), // Rename homePlmn to hplmn

      // Billing & Financial
      billingCurrency: new UntypedFormControl("", [Validators.required]),
      billingCycle: new UntypedFormControl("", [Validators.required]),
      paymentTerms: new UntypedFormControl("", [Validators.required]),
      taxNumber: new UntypedFormControl("", [Validators.required]),
      bankAccountNumber: new UntypedFormControl("", [Validators.required]),
      swiftCode: new UntypedFormControl(""),
    });
  }

  ngOnInit(): void {
    this.fetchOrganizations();
    this.fetchClearingHouses();
    this.fetchCountries();
    this.fetchCurrencyCodes();
    if (this.selectedPartnerRating?.partnerId) {
      this.loadPartnerData(this.selectedPartnerRating.partnerId);
    }

    // Dynamic validator for Home PLMN and TADIG Code based on LOB
    this.partnerRatingForm.get("lineOfBusiness")?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((lob) => {
        const hplmnControl = this.partnerRatingForm.get("hplmn");
        const tadigControl = this.partnerRatingForm.get("tadigCode");
        const interconnectControl = this.partnerRatingForm.get("interconnectType");
        if (lob === "ROAMING") {
          hplmnControl?.setValidators([Validators.required]);
          tadigControl?.setValidators([Validators.required]);
          
          interconnectControl?.clearValidators();
          interconnectControl?.setValue("");
        } else {
          hplmnControl?.clearValidators();
          hplmnControl?.setValue("");
          tadigControl?.clearValidators();
          tadigControl?.setValue("");
          
          interconnectControl?.setValidators([Validators.required]);
        }

        hplmnControl?.updateValueAndValidity();
        tadigControl?.updateValueAndValidity();
        interconnectControl?.updateValueAndValidity();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private fetchOrganizations(): void {
    this.partnerManageService.getOrganizations().subscribe({
      next: (res: any) => {
        this.organizationList = Array.isArray(res) ? res : [];
      },
      error: (err: any) => console.error("Failed to load organizations", err),
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

  private fetchCountries(): void {
    this.partnerManageService.getContries().subscribe({
      next: (res: any) => {
        this.countryList = Array.isArray(res) ? res : [];
      },
      error: (err: any) => console.error("Failed to load countries", err),
    });
  }

  private fetchCurrencyCodes(): void {
    this.partnerManageService.getCurrencyCodes().subscribe({
      next: (res: any) => {
        this.currencyList = Array.isArray(res) ? res : [];
      },
      error: (err: any) => console.error("Failed to load currency codes", err),
    });
  }


  private loadPartnerData(partnerId: number): void {
    this.commonservice.spinnerShow();
    this.partnerManageService
      .getById(partnerId)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonservice.spinnerHide())
      )
      .subscribe({
        next: (res: any) => this.patchForm(res),
        error: (error: any) => {
          this.commonservice.toastError(
            error?.error?.errorMessage || error?.message || "Failed to load partner data"
          );
          this.patchForm(this.selectedPartnerRating);
        },
      });
  }

  private patchForm(src: any): void {
    this.partnerRatingForm.patchValue({
      partnerName: src.partnerName || "",
      partnerCode: src.partnerCode || "",
      partnerType: src.partnerType || "",
      status: src.status || "ACTIVE",
      organizationId: src.organizationId || null,
      lineOfBusiness: src.lineOfBusiness || "",
      contactPersonName: src.contactPersonName || "",
      email: src.email || "",
      phoneNumber: src.phoneNumber || "",
      addressLine1: src.addressLine1 || "",
      city: src.city || "",
      postalCode: src.postalCode || "",
      country: src.country || "",
      interconnectType: src.interconnectType || "",
      ipAddress: src.ipAddress || "",
      pointCode: src.pointCode || "",
      routingPrefix: src.routingPrefix || "",
      tadigCode: src.tadigCode || "",
      hplmn: src.hplmn || "",
      billingCurrency: src.billingCurrency || "",
      billingCycle: src.billingCycle || "",
      paymentTerms: src.paymentTerms || "",
      taxNumber: src.taxNumber || "",
      bankAccountNumber: src.bankAccountNumber || "",
      swiftCode: src.swiftCode || "",
    });
  }

  submit(): void {
    if (this.partnerRatingForm.invalid) {
      this.partnerRatingForm.markAllAsTouched();
      return;
    }

    const fv = this.partnerRatingForm.getRawValue();
    const payload: any = {
      partnerName: fv.partnerName,
      partnerCode: fv.partnerCode,
      partnerType: fv.partnerType,
      status: fv.status,
      organizationId: fv.organizationId,
      lineOfBusiness: fv.lineOfBusiness,
      contactPersonName: fv.contactPersonName,
      email: fv.email,
      phoneNumber: fv.phoneNumber,
      addressLine1: fv.addressLine1,
      city: fv.city,
      postalCode: fv.postalCode,
      country: fv.country,
      interconnectType: fv.lineOfBusiness === 'INTERCONNECT' ? fv.interconnectType : null,
      routingPrefix: fv.lineOfBusiness === 'INTERCONNECT' ? fv.routingPrefix : null,
      billingCurrency: fv.billingCurrency,
      billingCycle: fv.billingCycle,
      paymentTerms: fv.paymentTerms,
      taxNumber: fv.taxNumber,
      bankAccountNumber: fv.bankAccountNumber,
      swiftCode: fv.swiftCode,
      pointCode: fv.lineOfBusiness === 'INTERCONNECT' ? fv.pointCode || null : null,
      tadigCode: fv.tadigCode || null,
      clearingHouseId: null,
      hplmn: fv.hplmn || null,
      // SFTP fields are managed separately; send null to avoid overwriting from main form
      tapSftpRouteType: null,
      sftpHost: null,
      sftpPort: null,
      sftpUsername: null,
      sftpPassword: null,
      sftpRemotePath: null,
      sftpInboxPath: null,
      ipAddress: null,
    };

    // Conditional fields — only included when relevant
    if (fv.lineOfBusiness === 'INTERCONNECT' && this.isInterconnectTypeIP) {
      payload.ipAddress = fv.ipAddress;
    }



    const isEdit = !!this.selectedPartnerRating?.partnerId;
    this.commonservice.spinnerShow();

    const request$ = isEdit
      ? this.partnerManageService.putMethod(this.selectedPartnerRating.partnerId, payload)
      : this.partnerManageService.postMethod(payload);

    request$
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonservice.spinnerHide()),
        catchError((error) => {
          this.commonservice.toastError(
            error?.error?.errorMessage || error?.message || "Something went wrong"
          );
          return throwError(() => error);
        })
      )
      .subscribe({
        next: () => {
          this.commonservice.toastSuccess(
            `Partner ${isEdit ? "updated" : "created"} successfully`
          );
          this.onClose(true);
        },
        error: () => {},
      });
  }

  onClose(isReload: boolean = false): void {
    this.partnerRatingForm.reset({ status: "ACTIVE" });
    this.selectedPartnerRating = null;
    this.close.emit(isReload);

    const modalEl = document.getElementById("add-partner-rating");
    if (modalEl) {
      const inst = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
      inst.hide();
      document.querySelectorAll(".modal-backdrop").forEach(el => el.remove());
      document.body.classList.remove("modal-open");
      document.body.style.overflow = "";
    }
  }
}
