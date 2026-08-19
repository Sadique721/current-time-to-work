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
import { CommonService, status } from "src/app/core.index";
import { WhiteeSpaceValidator } from "src/app/core/shared/custom-validations/white-space.validator";
import { SourceConfigurationManageService } from "../source-configuration-manage-rating.service";
import { ISourceConfigManage } from "../source-configuration-manage-rating.interface";
declare var bootstrap: any;
@Component({
  selector: "app-source-configuration-add-edit",
  templateUrl: "./source-configuration-add-edit.component.html",
  styleUrl: "./source-configuration-add-edit.component.scss",
  standalone: false,
})
export class SourceConfigurationAddEditComponent implements OnInit, OnDestroy {
  statusOptions = [
    { label: "enabled", value: "enabled" },
    { label: "disabled", value: "disabled" },
  ];
  serviceTypeOptions = [
    { label: "VOICE", value: "VOICE" },
    { label: "SMS", value: "SMS" },
    { label: "USAGE", value: "USAGE" },
  ];
  lineOfBusinessOptions = [
    { label: "Interconnect", value: "interconnect" },
    { label: "Roaming", value: "roaming" },
  ];
  souceConfigurationRatingForm: UntypedFormGroup;
  private destroy$ = new Subject<void>();
  @Output() close = new EventEmitter<boolean>();
  @Input() selectedSourceConfiguration: any = null;

  constructor(
    private common: CommonService,
    private sourceConfigurationManageService: SourceConfigurationManageService
  ) {
    this.souceConfigurationRatingForm = new UntypedFormGroup({
      sourceName: new UntypedFormControl("", [Validators.required]),
      topicName: new UntypedFormControl("", [Validators.required]),
      status: new UntypedFormControl("", [Validators.required]),
      serviceType: new UntypedFormControl("", [Validators.required]),
      lineOfBusiness: new UntypedFormControl("interconnect", [Validators.required]),
    });
  }
  ngOnInit(): void {
    if (this.selectedSourceConfiguration?.sourceId) {
      const { status, sourceName, topicName, serviceType, ...rest } =
        this.selectedSourceConfiguration;
      
      let baseTopicName = topicName;
      let lob = "interconnect";

      if (topicName && topicName.includes("-")) {
        const parts = topicName.split("-");
        lob = parts[0];
        baseTopicName = parts.slice(1).join("-");
      }

      this.souceConfigurationRatingForm.patchValue({
        status,
        sourceName,
        topicName: baseTopicName,
        serviceType,
        lineOfBusiness: lob,
      });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.souceConfigurationRatingForm.valid) {
      if (this.selectedSourceConfiguration.sourceId) {
        const countryId = this.selectedSourceConfiguration.sourceId;

        const fv = this.souceConfigurationRatingForm.value;
        const sourceData: any = {
          sourceName: fv.sourceName,
          topicName: `${fv.lineOfBusiness}-${fv.topicName}`,
          status: fv.status,
          serviceType: fv.serviceType,
        };
        this.common.spinnerShow();
        this.sourceConfigurationManageService
          .updateMethod(countryId, sourceData)
          .pipe(
            takeUntil(this.destroy$),
            finalize(() => {
              this.common.spinnerHide();
            }),
            catchError((error) => {
              const message =
                error?.error?.errorMessage ||
                error?.message ||
                "Something went wrong";
              this.common.toastError(message);
              return throwError(() => error);
            })
          )
          .subscribe({
            next: (response: any) => {
              this.souceConfigurationRatingForm.reset();
              this.onClose(true);
              this.common.toastSuccess(
                "Source Configuration rating updated successfully"
              );
            },
            error: () => {},
          });
      } else {
        const fv = this.souceConfigurationRatingForm.value;

        const sourceData: any = {
          sourceName: fv.sourceName,
          topicName: `${fv.lineOfBusiness}-${fv.topicName}`,
          status: fv.status,
          serviceType: fv.serviceType,
        };

        this.common.spinnerShow();
        this.sourceConfigurationManageService
          .postMethod(sourceData)
          .pipe(
            takeUntil(this.destroy$),
            finalize(() => {
              this.common.spinnerHide();
            }),
            catchError((error) => {
              const message =
                error?.error?.errorMessage ||
                error?.message ||
                "Something went wrong";
              this.common.toastError(message);
              return throwError(() => error);
            })
          )
          .subscribe({
            next: (response: any) => {
              this.souceConfigurationRatingForm.reset();
              this.onClose(true);
              this.common.toastSuccess(
                "Source Configuration created successfully"
              );
            },
            error: () => {},
          });
      }
    } else {
      this.souceConfigurationRatingForm.markAllAsTouched();
    }
  }

  onClose(isReload: boolean = false) {
    this.souceConfigurationRatingForm.reset();
    this.selectedSourceConfiguration = false;
    this.close.emit(isReload);

    const modalElement = document.getElementById(
      "add-source-configuration-rating"
    );
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.hide();
    }
  }
}
