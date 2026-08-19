import { Component, OnDestroy, OnInit } from "@angular/core";
import { UntypedFormControl, UntypedFormGroup, Validators } from "@angular/forms";
import { Subject, forkJoin } from "rxjs";
import { finalize, takeUntil } from "rxjs/operators";
import { CommonService } from "src/app/core.index";
import { DownloadCdrsService, CdrFilterRequest } from "./download-cdrs.service";

@Component({
  selector: "app-download-cdrs",
  templateUrl: "./download-cdrs.component.html",
  styleUrls: ["./download-cdrs.component.scss"],
  standalone: false,
})
export class DownloadCdrsComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  isLoading = false;

  // Dropdown options loaded dynamically from APIs
  zoneOptions: { label: string; value: string }[] = [];
  homePlmnOptions: { label: string; value: string }[] = [];
  visitedPlmnOptions: { label: string; value: string }[] = [];
  incomingAccountOptions: { label: string; value: string }[] = [];
  outgoingAccountOptions: { label: string; value: string }[] = [];
  statusOptions: { label: string; value: string }[] = [];
  serviceTypeOptions: { label: string; value: string }[] = [];
  callTypeOptions: { label: string; value: string }[] = [];
  lobOptions: { label: string; value: string }[] = [];

  // Labels that change dynamically based on serviceType
  callingLabel = "Calling Number";
  calledLabel = "Called Number";
  dateRangeLabel = "Start Time Range";

  // Form definition
  cdrForm = new UntypedFormGroup({
    serviceType: new UntypedFormControl("VOICE", [Validators.required]),
    callingOrSubscriber: new UntypedFormControl(null),
    calledOrApn: new UntypedFormControl(null),
    incomingAccountId: new UntypedFormControl(null),
    outgoingAccountId: new UntypedFormControl(null),
    incomingRatingStatus: new UntypedFormControl(null),
    outgoingRatingStatus: new UntypedFormControl(null),
    homePlmn: new UntypedFormControl(null),
    visitedPlmn: new UntypedFormControl(null),
    zoneName: new UntypedFormControl(null),
    lineOfBusiness: new UntypedFormControl(null, [Validators.required]),
    callType: new UntypedFormControl(null),
    fromTime: new UntypedFormControl((() => {
      const d = new Date();
      d.setHours(0, 0, 0, 0);
      return d;
    })(), [Validators.required]),
    toTime: new UntypedFormControl((() => {
      const d = new Date();
      d.setHours(0, 0, 0, 0);
      return d;
    })(), [Validators.required]),
  });

  constructor(
    private cdrService: DownloadCdrsService,
    private commonService: CommonService
  ) {}

  ngOnInit(): void {
    this.loadDropdowns();
    this.setupServiceTypeChangeListener();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** Load all options from APIs */
  private loadDropdowns(): void {
    this.isLoading = true;
    forkJoin({
      zones: this.cdrService.getZoneNames(),
      homePlmns: this.cdrService.getHomePlmn(),
      visitedPlmns: this.cdrService.getVisitedPlmn(),
      incomingAccs: this.cdrService.getIncomingAccountIds(),
      outgoingAccs: this.cdrService.getOutgoingAccountIds(),
      statuses: this.cdrService.getRatingStatuses(),
      serviceTypes: this.cdrService.getServiceTypes(),
      callTypes: this.cdrService.getCallTypes(),
      lobs: this.cdrService.getLineOfBusiness(),
    })
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.isLoading = false))
      )
      .subscribe({
        next: (data) => {
          this.zoneOptions = data.zones.map((x) => ({ label: x, value: x }));
          this.homePlmnOptions = data.homePlmns.map((x) => ({ label: x, value: x }));
          this.visitedPlmnOptions = data.visitedPlmns.map((x) => ({ label: x, value: x }));
          this.incomingAccountOptions = data.incomingAccs.map((x) => ({ label: x, value: x }));
          this.outgoingAccountOptions = data.outgoingAccs.map((x) => ({ label: x, value: x }));
          this.statusOptions = data.statuses.map((x) => ({ label: x, value: x }));
          this.serviceTypeOptions = data.serviceTypes.map((x) => ({ label: x, value: x }));
          this.callTypeOptions = data.callTypes.map((x) => ({ label: x, value: x }));
          this.lobOptions = data.lobs.map((x) => ({ label: x, value: x }));
        },
        error: (err) => {
          this.commonService.toastError(
            err?.error?.msg || "Failed to load dropdown options"
          );
        },
      });
  }

  /** Dynamic UI behavior listener based on Service Type */
  private setupServiceTypeChangeListener(): void {
    this.cdrForm
      .get("serviceType")
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((type: string) => {
        // Reset dynamic field values to null first to avoid sending cross-type filters
        this.cdrForm.patchValue({
          callingOrSubscriber: null,
          calledOrApn: null,
          callType: null,
        });

        if (type === "USAGE") {
          this.callingLabel = "Subscriber Identity";
          this.calledLabel = "Access Point Name";
          this.dateRangeLabel = "Start Time Range";
        } else if (type === "SMS") {
          this.callingLabel = "Calling Number";
          this.calledLabel = "Called Number";
          this.dateRangeLabel = "Created Date Range";
        } else {
          // VOICE
          this.callingLabel = "Calling Number";
          this.calledLabel = "Called Number";
          this.dateRangeLabel = "Start Time Range";
        }
      });
  }

  /** Trigger download */
  exportExcel(): void {
    if (this.cdrForm.invalid) {
      this.cdrForm.markAllAsTouched();
      return;
    }

    const formValue = this.cdrForm.getRawValue();

    // Prepare payload
    const filter: CdrFilterRequest = {
      serviceType: formValue.serviceType,
      callingOrSubscriber: this.getNullOrValue(formValue.callingOrSubscriber),
      calledOrApn: this.getNullOrValue(formValue.calledOrApn),
      incomingAccountId: this.getNullOrValue(formValue.incomingAccountId),
      outgoingAccountId: this.getNullOrValue(formValue.outgoingAccountId),
      incomingRatingStatus: this.getNullOrValue(formValue.incomingRatingStatus),
      outgoingRatingStatus: this.getNullOrValue(formValue.outgoingRatingStatus),
      homePlmn: this.getNullOrValue(formValue.homePlmn),
      visitedPlmn: this.getNullOrValue(formValue.visitedPlmn),
      zoneName: this.getNullOrValue(formValue.zoneName),
      lineOfBusiness: this.getNullOrValue(formValue.lineOfBusiness),
      callType: formValue.serviceType !== "USAGE" ? this.getNullOrValue(formValue.callType) : null,
      fromTime: this.formatToIso(formValue.fromTime),
      toTime: this.formatToIso(formValue.toTime),
    };

    this.commonService.spinnerShow();
    this.cdrService
      .exportCdr(filter)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide())
      )
      .subscribe({
        next: (blob: Blob) => {
          if (!blob || blob.size === 0) {
            this.commonService.toastError("No CDR data found matching the filters.");
            return;
          }
          const url = URL.createObjectURL(blob);
          const a = document.createElement("a");
          a.href = url;
          const timestamp = this.getTimestampString();
          a.download = `${filter.serviceType.toLowerCase()}_cdr_export_${timestamp}.xlsx`;
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          URL.revokeObjectURL(url);
          this.commonService.toastSuccess("CDR Excel file exported successfully");
        },
        error: (err) => {
          this.commonService.toastError(
            err?.error?.msg || "Failed to download CDR file"
          );
        },
      });
  }

  /** Reset form */
  resetFilters(): void {
    this.cdrForm.reset({
      serviceType: "VOICE",
      callingOrSubscriber: null,
      calledOrApn: null,
      incomingAccountId: null,
      outgoingAccountId: null,
      incomingRatingStatus: null,
      outgoingRatingStatus: null,
      homePlmn: null,
      visitedPlmn: null,
      zoneName: null,
      lineOfBusiness: null,
      callType: null,
      fromTime: (() => {
        const d = new Date();
        d.setHours(0, 0, 0, 0);
        return d;
      })(),
      toTime: (() => {
        const d = new Date();
        d.setHours(0, 0, 0, 0);
        return d;
      })(),
    });
    this.commonService.toastSuccess("Filters reset to default");
  }

  /** Helper to map empty string/undefined to null */
  private getNullOrValue(val: any): any {
    if (val === undefined || val === "" || val === null) {
      return null;
    }
    return val;
  }

  /** Helper to format Date into local ISO string (yyyy-MM-ddTHH:mm:ss) */
  private formatToIso(dateVal: any): string | null {
    if (!dateVal) return null;
    const date = new Date(dateVal);
    if (isNaN(date.getTime())) return null;

    const pad = (num: number) => String(num).padStart(2, "0");
    const yyyy = date.getFullYear();
    const MM = pad(date.getMonth() + 1);
    const dd = pad(date.getDate());
    const hh = pad(date.getHours());
    const mm = pad(date.getMinutes());
    const ss = pad(date.getSeconds());

    return `${yyyy}-${MM}-${dd}T${hh}:${mm}:${ss}`;
  }

  /** Get a timestamp string for filename (YYYYMMDD_HHMMSS) */
  private getTimestampString(): string {
    const date = new Date();
    const pad = (num: number) => String(num).padStart(2, "0");
    const yyyy = date.getFullYear();
    const MM = pad(date.getMonth() + 1);
    const dd = pad(date.getDate());
    const hh = pad(date.getHours());
    const mm = pad(date.getMinutes());
    const ss = pad(date.getSeconds());
    return `${yyyy}${MM}${dd}_${hh}${mm}${ss}`;
  }
}
