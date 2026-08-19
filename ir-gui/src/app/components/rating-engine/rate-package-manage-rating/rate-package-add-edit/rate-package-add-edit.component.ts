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
import { CommonService, routes, SidebarService } from "src/app/core.index";
import { RatePackageManageService } from "../rate-package-manage-rating.service";
import { Router } from "@angular/router";

@Component({
  selector: "app-rate-package-add-edit",
  templateUrl: "./rate-package-add-edit.component.html",
  styleUrl: "./rate-package-add-edit.component.scss",
  standalone: false,
})
export class RatePackageAddEditComponent implements OnInit, OnDestroy {
  public routes = routes;
  private destroy$ = new Subject<void>();

  @Output() close = new EventEmitter<boolean>();
  @Input() selectedRatePackage: any = null;

  // ─── Step 1 – Package Info ──────────────────────────────────────────────────
  packageForm: UntypedFormGroup;
  submitted = false;
  isCollapsed: boolean = false;

  serviceTypeOptions = [
    { label: "VOICE", value: "VOICE" },
    { label: "SMS", value: "SMS" },
    { label: "USAGE", value: "USAGE" },
  ];

  typeOptions = [
    { label: "BUYING", value: "BUYING" },
    { label: "SELLING", value: "SELLING" },
  ];

  ratePackageTypeOptions = [
    { label: "DESTINATION_BASED", value: "DESTINATION_BASED" },
    { label: "SOURCE_DESTINATION_BASED", value: "SOURCE_DESTINATION_BASED" },
    { label: "ZONE_DESTINATION_BASED", value: "ZONE_DESTINATION_BASED" },
  ];

  roundingOptions = [
    { label: "UPPER", value: "UPPER" },
    { label: "LOWER", value: "LOWER" },
    { label: "DEFAULT", value: "DEFAULT" },
  ];

  priceroundingOptions = [
    { label: "UPPER", value: "UPPER" },
    { label: "LOWER", value: "LOWER" },
    { label: "DEFAULT", value: "DEFAULT" },
  ];



  pulseListFiltered: any[] = [];
  zoneList: any[] = [];
  currencyList: any[] = [];

  // ─── Step 2 – Rate Details ──────────────────────────────────────────────────
  /** The flat JS array that drives the inline-editable table */
  rateDetailRows: {
    destinationPrefix: string;
    destinationPrefixName: string;
    sourcePrefix: string;
    sourcePrefixName: string;
    zoneName: string;
    rate: string;
    startTime: Date | string | null;
    endTime: Date | string | null;
  }[] = [];

  isSourceBased = false;
  isZoneBased = false;
  isPreviewLoading = false;

  constructor(
    private commonservice: CommonService,
    private ratePackageManageService: RatePackageManageService,
    private sidebar: SidebarService,
    private router: Router
  ) {
    this.packageForm = new UntypedFormGroup({
      packageName: new UntypedFormControl(null, [Validators.required]),
      packageDesc: new UntypedFormControl(null, [
        Validators.required,
        Validators.pattern("^.{0,150}$"),
      ]),
      serviceType: new UntypedFormControl("", [Validators.required]),
      type: new UntypedFormControl("", [Validators.required]),
      ratePackageType: new UntypedFormControl("DESTINATION_BASED", []),
      rounding: new UntypedFormControl("", [Validators.required]),
      priceRounding: new UntypedFormControl("", [Validators.required]),
      pulseId: new UntypedFormControl("", [Validators.required]),
      currency: new UntypedFormControl("", [Validators.required]),
      startTime: new UntypedFormControl(null),
      endTime: new UntypedFormControl(null),
      rate: new UntypedFormControl(null),
    });
  }

  ngOnInit(): void {
    this.loadZoneList();
    this.fetchCurrencyCodes();

    // Subscribe to ratePackageType changes to update isSourceBased flag
    this.packageForm
      .get("ratePackageType")
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((value) => {
        this.isSourceBased = value === "SOURCE_DESTINATION_BASED";
        this.isZoneBased = value === "ZONE_DESTINATION_BASED";
      });

    // Subscribe to serviceType changes to fetch pulses
    this.packageForm
      .get("serviceType")
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((value) => {
        this.handleServiceTypeChange(value);
      });

    // Handle navigation-state based hydration (edit via router state)
    const navState: any =
      this.router.getCurrentNavigation()?.extras?.state || window.history.state;

    if (navState?.id && !this.selectedRatePackage?.ratePackageId) {
      this.selectedRatePackage = {
        ratePackageId: navState.id,
        ...navState,
      };
    }

    if (this.selectedRatePackage?.ratePackageId) {
      this.fetchRatePackageDetails(this.selectedRatePackage.ratePackageId);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get isEditMode(): boolean {
    return !!(
      this.selectedRatePackage && this.selectedRatePackage.ratePackageId
    );
  }



  loadZoneList(): void {
    this.ratePackageManageService
      .getZoneNames()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data: any) => {
          this.zoneList = Array.isArray(data) ? data : [];
        },
        error: () => {},
      });
  }

  isZoneInvalid(zoneName: string): boolean {
    if (!zoneName) return false;
    return !this.zoneList.some((z) => z.zoneName === zoneName);
  }

  private fetchCurrencyCodes(): void {
    this.ratePackageManageService.getCurrencyCodes().subscribe({
      next: (res: any) => {
        this.currencyList = Array.isArray(res) ? res : [];
      },
      error: (err: any) => console.error("Failed to load currency codes", err),
    });
  }

  handleServiceTypeChange(value: string): void {
    const ratePackageTypeCtrl = this.packageForm.get('ratePackageType');
    const startTimeCtrl = this.packageForm.get('startTime');
    const endTimeCtrl = this.packageForm.get('endTime');
    const rateCtrl = this.packageForm.get('rate');

    if (value === 'USAGE') {
      ratePackageTypeCtrl?.clearValidators();
      startTimeCtrl?.setValidators([Validators.required]);
      endTimeCtrl?.setValidators([Validators.required]);
      rateCtrl?.setValidators([Validators.required, Validators.min(0)]);
    } else {
      ratePackageTypeCtrl?.setValidators([Validators.required]);
      startTimeCtrl?.clearValidators();
      endTimeCtrl?.clearValidators();
      rateCtrl?.clearValidators();
    }
    ratePackageTypeCtrl?.updateValueAndValidity();
    startTimeCtrl?.updateValueAndValidity();
    endTimeCtrl?.updateValueAndValidity();
    rateCtrl?.updateValueAndValidity();

    this.packageForm.controls["pulseId"].setValue("");
    this.pulseListFiltered = [];
    if (value) {
      this.ratePackageManageService
        .getPulseByServiceType(value.toLowerCase())
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (res: any) => {
            const data = Array.isArray(res) ? res : [];
            this.pulseListFiltered = data.map((item: any) => ({
              ...item,
              pulseId: item.pulseId ?? item.id,
            }));
          },
          error: (err: any) => {
            console.error("Failed to fetch pulses by service type", err);
            this.pulseListFiltered = [];
          },
        });
    }
  }

  // ─── Fetch for edit mode ─────────────────────────────────────────────────────
  fetchRatePackageDetails(ratePackageId: number): void {
    this.commonservice.spinnerShow();
    this.ratePackageManageService
      .getById(ratePackageId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data: any) => {
          this.commonservice.spinnerHide();
          this.selectedRatePackage = data;
          this.populateFormForEdit();
        },
        error: (error) => {
          this.commonservice.spinnerHide();
          this.commonservice.toastError(
            error?.error?.msg || "Failed to load rate package details"
          );
        },
      });
  }

  populateFormForEdit(): void {
    if (!this.selectedRatePackage) return;

    this.packageForm.patchValue({
      packageName: this.selectedRatePackage.packageName,
      packageDesc: this.selectedRatePackage.packageDesc,
      type: this.selectedRatePackage.type,
      serviceType: this.selectedRatePackage.serviceType,
      ratePackageType: this.selectedRatePackage.ratePackageType,
      pulseId: this.selectedRatePackage.pulseId,
      rounding: this.selectedRatePackage.rounding,
      priceRounding: this.selectedRatePackage.priceRounding,
      currency: this.selectedRatePackage.currency || "",
      startTime: this.formatForInput(this.selectedRatePackage.startTime),
      endTime: this.formatForInput(this.selectedRatePackage.endTime),
      rate: this.selectedRatePackage.rate,
    });

    // Fetch pulse list based on pre-filled serviceType
    if (this.selectedRatePackage.serviceType) {
      this.ratePackageManageService
        .getPulseByServiceType(this.selectedRatePackage.serviceType.toLowerCase())
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (res: any) => {
            const data = Array.isArray(res) ? res : [];
            this.pulseListFiltered = data.map((item: any) => ({
              ...item,
              pulseId: item.pulseId ?? item.id,
            }));
          },
          error: (err: any) => console.error("Failed to fetch pulses for edit mode", err),
        });
    }

    this.isSourceBased =
      this.selectedRatePackage.ratePackageType === "SOURCE_DESTINATION_BASED";

    this.isZoneBased =
      this.selectedRatePackage.ratePackageType === "ZONE_DESTINATION_BASED";

    // Hydrate rate detail rows from existing data
    if (
      this.selectedRatePackage.rate_details &&
      Array.isArray(this.selectedRatePackage.rate_details)
    ) {
      this.rateDetailRows = this.selectedRatePackage.rate_details.map(
        (d: any) => ({
          destinationPrefix: d.destinationPrefix ?? "",
          destinationPrefixName: d.destinationPrefixName ?? "",
          sourcePrefix: d.sourcePrefix ?? "",
          sourcePrefixName: d.sourcePrefixName ?? "",
          zoneName: d.zoneName ?? "",
          rate: d.rate ?? "",
          startTime: d.startTime ? new Date(d.startTime.replace(" ", "T")) : "",
          endTime: d.endTime ? new Date(d.endTime.replace(" ", "T")) : "",
        })
      );
    }
  }

  // ─── File Upload & Preview ─────────────────────────────────────────────────────────
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    const formData = new FormData();
    formData.append("file", file);

    const ratePackageType = this.packageForm.get("ratePackageType")?.value;
    if (ratePackageType) {
      formData.append("ratePackageType", ratePackageType);
    }

    this.isPreviewLoading = true;
    this.ratePackageManageService
      .previewRateDetailsFile(formData)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.isPreviewLoading = false))
      )
      .subscribe({
        next: (res: any) => {
          const parsed: any[] = Array.isArray(res) ? res : [];
          this.rateDetailRows = parsed.map((d: any) => ({
            destinationPrefix: d.destinationPrefix ?? "",
            destinationPrefixName: d.destinationPrefixName ?? "",
            sourcePrefix: d.sourcePrefix ?? "",
            sourcePrefixName: d.sourcePrefixName ?? "",
            zoneName: d.zoneName ?? "",
            rate: d.rate ?? "",
            startTime: d.startTime ? new Date(d.startTime.replace(" ", "T")) : "",
            endTime: d.endTime ? new Date(d.endTime.replace(" ", "T")) : "",
          }));
          input.value = "";
          this.commonservice.toastSuccess(
            `${parsed.length} rate row(s) loaded from file.`
          );
        },
        error: (err) => {
          this.commonservice.toastError(
            err?.error?.errorMessage ?? "Failed to parse file. Please check the format."
          );
          input.value = "";
        },
      });
  }

  // ─── Step 2 – Manual Row Management ─────────────────────────────────────────
  addEmptyRow(): void {
    this.rateDetailRows.push({
      destinationPrefix: "",
      destinationPrefixName: "",
      sourcePrefix: "",
      sourcePrefixName: "",
      zoneName: "",
      rate: "",
      startTime: "",
      endTime: "",
    });
  }

  removeRow(index: number): void {
    this.rateDetailRows.splice(index, 1);
  }

  // ─── Download sample template ────────────────────────────────────────────────
  /**
   * Downloads the rate-detail template in the requested format.
   * API shape: GET /api/rate-details/template/{format}/{ratePackageType}
   * @param format  'excel' | 'csv'
   */
  downloadSampleFile(format: 'excel' | 'csv'): void {
    const selectedType = this.packageForm.get("ratePackageType")?.value;
    if (!selectedType) {
      this.commonservice.toastError(
        "Rate Package Type is not set. Please go back and select one."
      );
      return;
    }

    const defaultExt = format === 'excel' ? 'xlsx' : 'csv';
    const url = `/rate-details/template/${format}/${selectedType}`;
    this.ratePackageManageService
      .downloadSampleRateDetailsFile(url)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          const contentDisposition = res.headers.get("content-disposition");
          const fileNameMatch =
            contentDisposition?.match(/filename="?([^"]+)"?/);
          const fileName = fileNameMatch
            ? fileNameMatch[1]
            : `sample_${selectedType}.${defaultExt}`;

          const blob = new Blob([res.body], {
            type:
              res.headers.get("content-type") ||
              (format === 'excel'
                ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
                : 'text/csv'),
          });
          const downloadUrl = window.URL.createObjectURL(blob);
          const link = document.createElement("a");
          link.href = downloadUrl;
          link.download = fileName;
          link.click();
          window.URL.revokeObjectURL(downloadUrl);
        },
        error: () => {
          this.commonservice.toastError(`Failed to download ${format.toUpperCase()} template.`);
        },
      });
  }

  // ─── Final Submit ────────────────────────────────────────────────────────────
  submitPackage(): void {
    if (this.packageForm.invalid) {
      this.packageForm.markAllAsTouched();
      return;
    }
    const step1Data = this.packageForm.getRawValue();

    const formatDateTime = (dt: any): string | null => {
      if (!dt) return null;
      if (dt instanceof Date) {
        const yyyy = dt.getFullYear();
        const MM = this.padZero(dt.getMonth() + 1);
        const dd = this.padZero(dt.getDate());
        const hh = this.padZero(dt.getHours());
        const mm = this.padZero(dt.getMinutes());
        const ss = this.padZero(dt.getSeconds());
        return `${yyyy}-${MM}-${dd} ${hh}:${mm}:${ss}`;
      }
      if (typeof dt === 'string') {
        let formatted = dt.replace("T", " ");
        if (formatted.split(":").length === 2) {
          formatted += ":00";
        }
        return formatted;
      }
      return null;
    };

    const rateDetails = this.rateDetailRows.map((row) => {
      const detail: any = {
        rate: row.rate,
        startTime: formatDateTime(row.startTime),
        endTime: formatDateTime(row.endTime),
      };

      if (step1Data.ratePackageType === "ZONE_DESTINATION_BASED") {
        detail.zoneName = row.zoneName || null;
      } else {
        // Both DESTINATION_BASED and SOURCE_DESTINATION_BASED need destination
        detail.destinationPrefix = row.destinationPrefix;
        detail.destinationPrefixName = row.destinationPrefixName;

        // Only SOURCE_DESTINATION_BASED needs source
        if (step1Data.ratePackageType === "SOURCE_DESTINATION_BASED") {
          detail.sourcePrefix = row.sourcePrefix || null;
          detail.sourcePrefixName = row.sourcePrefixName || null;
        }
      }

      return detail;
    });

    const payload = {
      packageName: step1Data.packageName,
      packageDesc: step1Data.packageDesc,
      type: step1Data.type,
      serviceType: step1Data.serviceType,
      ratePackageType: step1Data.serviceType === 'USAGE' ? null : step1Data.ratePackageType,
      pulseId: Number(step1Data.pulseId),
      rounding: step1Data.rounding,
      priceRounding: step1Data.priceRounding,
      currency: step1Data.currency,
      startTime: step1Data.serviceType === 'USAGE' ? formatDateTime(step1Data.startTime) : null,
      endTime: step1Data.serviceType === 'USAGE' ? formatDateTime(step1Data.endTime) : null,
      rate: step1Data.serviceType === 'USAGE' ? Number(step1Data.rate) : null,
      rate_details: step1Data.serviceType === 'USAGE' ? [] : rateDetails,
    };

    if (this.isEditMode) {
      this.updateRatePackage(payload);
    } else {
      this.createRatePackage(payload);
    }
  }

  private createRatePackage(payload: any): void {
    this.commonservice.spinnerShow();
    this.ratePackageManageService
      .addRatePackages("/rate-packages", payload)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonservice.spinnerHide()),
        catchError((error) => throwError(() => error))
      )
      .subscribe({
        next: () => {
          this.commonservice.toastSuccess("Rate Package created successfully.");
          this.onClose(true);
        },
        error: (err) => {
          this.commonservice.toastError(
            err?.error?.errorMessage ?? "Failed to create rate package."
          );
        },
      });
  }

  private updateRatePackage(payload: any): void {
    this.commonservice.spinnerShow();
    this.ratePackageManageService
      .updateRatePackages(
        `/rate-packages/${this.selectedRatePackage.ratePackageId}`,
        payload
      )
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonservice.spinnerHide()),
        catchError((error) => throwError(() => error))
      )
      .subscribe({
        next: () => {
          this.commonservice.toastSuccess("Rate Package updated successfully.");
          this.onClose(true);
        },
        error: (err) => {
          this.commonservice.toastError(
            err?.error?.errorMessage ?? "Failed to update rate package."
          );
        },
      });
  }

  // ─── Miscellaneous ────────────────────────────────────────────────────────────
  onClose(success?: boolean): void {
    this.router.navigateByUrl(routes.ratingratepackage, { replaceUrl: true });
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  trackByIndex(index: number): number {
    return index;
  }

  private padZero(n: number): string {
    return n < 10 ? "0" + n : n.toString();
  }

  private formatForInput(date: any): Date | null {
    if (!date) return null;
    // Replace space with 'T' to ensure consistent parsing across browsers
    const dateString = typeof date === 'string' ? date.replace(' ', 'T') : date;
    const d = new Date(dateString);
    if (isNaN(d.getTime())) return null;
    return d;
  }
}
