import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import {
  PaginationService,
  SidebarService,
  CommonService,
  tablePageSize,
  pageSelection,
} from "src/app/core.index";
import { InvoiceTemplateManageService } from "./invoice-template-manage.service";
import { UntypedFormControl, UntypedFormGroup, Validators } from "@angular/forms";
import { debounceTime, Subject, takeUntil } from "rxjs";
import { ConfirmDialogComponent } from "src/app/core/shared/confirm-dialog/confirm-dialog.component";

declare var bootstrap: any;

@Component({
  selector: "app-invoice-template-manage",
  templateUrl: "./invoice-template-manage.component.html",
  standalone: false,
})
export class InvoiceTemplateManageComponent implements OnInit, OnDestroy {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  tableData: any[] = [];
  pageSize = 10;
  totalData = 0;
  selectedTemplate: any = null;
  serialNumberArray: number[] = [];
  paginationSkip = 0;
  searchDataValue = new UntypedFormControl("");
  isCollapsed = false;
  selectedFile: any = null;
  private destroy$ = new Subject<void>();
  private lastPayload = "";

  lobOptions = [
    { label: "INTERCONNECT", value: "INTERCONNECT" },
    { label: "ROAMING", value: "ROAMING" },
  ];

  templateForm = new UntypedFormGroup({
    templateName: new UntypedFormControl("", [Validators.required]),
    templateDescription: new UntypedFormControl(""),
    lineOfBusiness: new UntypedFormControl("", [Validators.required]),
    isActive: new UntypedFormControl(true),
  });

  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private templateService: InvoiceTemplateManageService,
    private commonService: CommonService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        const searchValue = this.searchDataValue.value?.trim() || "";
        const currentPayload = JSON.stringify({
          skip: res.skip,
          limit: res.pageSize,
          search: searchValue,
        });

        if (this.lastPayload !== currentPayload) {
          this.lastPayload = currentPayload;
          this.getTableData({ skip: res.skip, limit: res.pageSize });
          this.pageSize = res.pageSize;
        }
      });
  }

  private getTableData(pageOption: pageSelection): void {
    const page = pageOption.skip / pageOption.limit + 1;
    const searchValue = this.searchDataValue.value?.trim() || "";

    const payload: any = searchValue
      ? {
          page,
          pageSize: pageOption.limit,
          searchCriteria: { searchTerm: searchValue },
        }
      : {
          page,
          pageSize: pageOption.limit,
        };

    this.commonService.spinnerShow();
    this.templateService
      .postPaginated(payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.commonService.spinnerHide();
          this.totalData = res.pageDetails?.totalRecords || 0;
          this.tableData = res.content || [];
          this.serialNumberArray = this.tableData.map((_, i) => i + 1);
          this.pagination.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: this.pageSize,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });
        },
        error: (error: any) => {
          this.commonService.spinnerHide();
          if (error.status === 404 && searchValue) {
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
              error?.error?.msg || "Something went wrong while fetching data"
            );
          }
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  public searchData(): void {
    this.lastPayload = "";
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  public clearSearch(): void {
    this.searchDataValue.setValue("");
    this.lastPayload = "";
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  onFileChange(event: any): void {
    if (event.target.files.length > 0) {
      const file = event.target.files[0];
      const extension = file.name.split('.').pop()?.toLowerCase();
      if (extension !== 'xsl' && extension !== 'xslt') {
        this.commonService.toastError("Only XSL and XSLT files are allowed.");
        this.selectedFile = null;
        this.resetFileInput();
        return;
      }
      this.selectedFile = file;
    } else {
      this.selectedFile = null;
    }
  }

  openAddEdit(item: any = null): void {
    if (item && item.templateId) {
      this.commonService.spinnerShow();
      this.templateService
        .getById(item.templateId)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (res: any) => {
            this.commonService.spinnerHide();
            this.selectedTemplate = res.content || res;
            this.selectedFile = null;
            this.resetFileInput();
            this.templateForm.reset({
              templateName: this.selectedTemplate?.templateName || "",
              templateDescription: this.selectedTemplate?.templateDescription || "",
              lineOfBusiness: this.selectedTemplate?.lineOfBusiness || item?.lineOfBusiness || "",
              isActive: this.selectedTemplate?.isActive !== false,
            });
            this.showModal();
          },
          error: (error: any) => {
            this.commonService.spinnerHide();
            this.commonService.toastError(
              error?.error?.msg || "Error fetching template details"
            );
          },
        });
    } else {
      this.selectedTemplate = {};
      this.selectedFile = null;
      this.resetFileInput();
      this.templateForm.reset({
        templateName: "",
        templateDescription: "",
        lineOfBusiness: "",
        isActive: true,
      });
      this.showModal();
    }
  }

  saveTemplate(): void {
    if (this.templateForm.invalid) {
      this.templateForm.markAllAsTouched();
      return;
    }

    const isEdit = this.selectedTemplate?.templateId;

    if (!isEdit && !this.selectedFile) {
      this.commonService.toastError("Please select a file to upload.");
      return;
    }

    const formData = new FormData();
    formData.append("templateName", this.templateForm.value.templateName);
    formData.append("templateDescription", this.templateForm.value.templateDescription || "");
    formData.append("lineOfBusiness", this.templateForm.value.lineOfBusiness || "");
    formData.append("isActive", this.templateForm.value.isActive);

    if (this.selectedFile) {
      formData.append("templateFile", this.selectedFile, this.selectedFile.name);
    }

    this.commonService.spinnerShow();

    const request$ = isEdit
      ? this.templateService.putMethod(this.selectedTemplate.templateId, formData)
      : this.templateService.postMethod(formData);

    request$.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.commonService.spinnerHide();
        this.commonService.toastSuccess(
          `Template ${isEdit ? "updated" : "created"} successfully`
        );
        this.onCloseAddEdit(true);
      },
      error: (err: any) => {
        this.commonService.spinnerHide();
        this.commonService.toastError(
          err?.error?.errorMessage || err?.error?.msg || `Failed to ${isEdit ? "update" : "create"} template`
        );
      },
    });
  }

  onCloseAddEdit(isReload: boolean): void {
    this.selectedTemplate = null;
    this.selectedFile = null;
    this.resetFileInput();
    const modalElement = document.getElementById("add-invoice-template");
    if (modalElement) {
      const modalInstance = bootstrap.Modal.getInstance(modalElement);
      if (modalInstance) modalInstance.hide();
    }
    if (isReload) {
      this.lastPayload = "";
      this.pagination.tablePageSize.next({
        skip: this.paginationSkip,
        limit: this.pageSize,
        pageSize: this.pageSize,
      });
    }
  }

  /** Clear the native file input so stale selections don't persist */
  private resetFileInput(): void {
    if (this.fileInput?.nativeElement) {
      this.fileInput.nativeElement.value = '';
    }
  }

  confirmDelete(id: any): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: "420px",
      disableClose: true,
      data: {
        title: "Delete Template",
        message: "Are you sure you want to delete this template?",
        confirmButtonText: "Yes Delete",
        cancelButtonText: "Cancel",
        iconClass: "ti ti-trash fs-24 text-danger",
      },
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.deleteTemplate(id);
      }
    });
  }

  deleteTemplate(id: any): void {
    if (!id) return;

    this.commonService.spinnerShow();
    this.templateService
      .deleteMethod(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.commonService.spinnerHide();
          if (
            this.totalData % this.pageSize === 1 &&
            this.totalData > this.pageSize
          ) {
            const previousPageSkip =
              (Math.ceil(this.totalData / this.pageSize) - 2) * this.pageSize;
            this.lastPayload = "";
            this.pagination.tablePageSize.next({
              skip: previousPageSkip,
              limit: this.pageSize,
              pageSize: this.pageSize,
            });
          } else {
            this.lastPayload = "";
            this.pagination.tablePageSize.next({
              skip: 0,
              limit: this.pageSize,
              pageSize: this.pageSize,
            });
          }
          this.commonService.toastSuccess(
            response?.statusMsg || "Template deleted successfully"
          );
        },
        error: (error: any) => {
          this.commonService.spinnerHide();
          this.commonService.toastError(
            error?.error?.ERROR || error?.error?.msg || "Failed to delete template"
          );
        },
      });
  }

  showModal(): void {
    const modalElement = document.getElementById("add-invoice-template");
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.show();
    }
  }
}
