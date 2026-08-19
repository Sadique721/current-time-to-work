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
import { OrganizationManageService } from "../organization-manage.service";
import { IOrganizationManage } from "../organization-manage.interface";
declare var bootstrap: any;

@Component({
  selector: "app-organization-add-edit",
  templateUrl: "./organization-add-edit.component.html",
  styleUrl: "./organization-add-edit.component.scss",
  standalone: false,
})
export class OrganizationAddEditComponent implements OnInit, OnDestroy {
  organizationForm: UntypedFormGroup;
  private destroy$ = new Subject<void>();
  @Output() close = new EventEmitter<boolean>();
  @Input() selectedOrganization: any = null;

  constructor(
    private commonservice: CommonService,
    private organizationManageService: OrganizationManageService
  ) {
    this.organizationForm = new UntypedFormGroup({
      name: new UntypedFormControl("", [Validators.required]),
      suffixName: new UntypedFormControl("", [Validators.required]),
      legalName: new UntypedFormControl("", [Validators.required]),
      address: new UntypedFormControl("", [Validators.required]),
    });
  }

  ngOnInit(): void {
    if (this.selectedOrganization?.organizationId) {
      const { name, suffixName, legalName, address } = this.selectedOrganization;
      this.organizationForm.patchValue({ name, suffixName, legalName, address });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  submit(): void {
    if (this.organizationForm.valid) {
      const { name, suffixName, legalName, address } = this.organizationForm.value;
      const payload: IOrganizationManage = { name, suffixName, legalName, address };

      if (this.selectedOrganization?.organizationId) {
        const id = this.selectedOrganization.organizationId;
        this.commonservice.spinnerShow();
        this.organizationManageService
          .putMethod(id, payload)
          .pipe(
            takeUntil(this.destroy$),
            finalize(() => this.commonservice.spinnerHide()),
            catchError((error) => {
              const message =
                error?.error?.errorMessage ||
                error?.message ||
                "Something went wrong";
              this.commonservice.toastError(message);
              return throwError(() => error);
            })
          )
          .subscribe({
            next: () => {
              this.organizationForm.reset();
              this.onClose(true);
              this.commonservice.toastSuccess("Organization updated successfully");
            },
            error: () => {},
          });
      } else {
        this.commonservice.spinnerShow();
        this.organizationManageService
          .postMethod(payload)
          .pipe(
            takeUntil(this.destroy$),
            finalize(() => this.commonservice.spinnerHide()),
            catchError((error) => {
              const message =
                error?.error?.errorMessage ||
                error?.message ||
                "Something went wrong";
              this.commonservice.toastError(message);
              return throwError(() => error);
            })
          )
          .subscribe({
            next: () => {
              this.organizationForm.reset();
              this.onClose(true);
              this.commonservice.toastSuccess("Organization created successfully");
            },
            error: () => {},
          });
      }
    } else {
      this.organizationForm.markAllAsTouched();
    }
  }

  onClose(isReload: boolean = false): void {
    this.organizationForm.reset();
    this.selectedOrganization = null;
    this.close.emit(isReload);

    const modalElement = document.getElementById("add-organization");
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.hide();
    }
  }
}
