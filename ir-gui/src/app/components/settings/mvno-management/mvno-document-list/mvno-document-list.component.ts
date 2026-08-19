import { CommonModule } from '@angular/common';
import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import {
  catchError,
  debounceTime,
  EMPTY,
  finalize,
  of,
  Subject,
  takeUntil,
} from 'rxjs';
import {
  CommonService,
  pageSelection,
  PaginationService,
  routes,
  sharedModule,
  SidebarService,
  tablePageSize,
} from 'src/app/core.index';
import { CustomElementModule } from 'src/app/core/shared/custom-elements/custom-elemets.module';
import { MvnoManagementService } from '../mvno-management.service';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { saveAs as importedSaveAs } from 'file-saver';
import { UntypedFormControl } from '@angular/forms';

@Component({
  selector: 'app-mvno-document-list',
  imports: [sharedModule, CustomElementModule, CommonModule],
  templateUrl: './mvno-document-list.component.html',
  styleUrl: './mvno-document-list.component.scss',
})
export class MvnoDocumentListComponent implements OnInit, OnDestroy {
  tableData: Array<any> = [];
  pageSize = 10;
  totalData = 0;
  paginationSkip = 0;
  serialNumberArray: number[] = [];
  isCollapsed = false;
  private destroy$ = new Subject<void>();
  private lastPayload: string = '';
  data: any = {};
  approveRejectModal: boolean = false;
  ifApproveStatus: boolean = false;
  selectedDocument: any = null;
  approveRejectRemark = new UntypedFormControl('');

  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private mvnoService: MvnoManagementService,
    private commonService: CommonService,
    private dialog: MatDialog,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.data.id = this.route.snapshot.paramMap.get('id')!;

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
          this.getTableData({ skip: res.skip, limit: res.pageSize });
          this.pageSize = res.pageSize;
        }
      });
  }

  private getTableData(pageOption: pageSelection): void {
    const page = pageOption.skip / pageOption.limit + 1;

    this.commonService.spinnerShow();
    this.mvnoService
      .getdocumentList(this.data?.id)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
        catchError((error) => {
          this.commonService.toastError(
            error?.error?.msg ||
              error?.error?.error ||
              error?.error?.ERROR ||
              'Something went wrong while fetching data',
          );

          return of({ dataList: [] });
        }),
      )
      .subscribe((res: any) => {
        const responseList: any[] = res.dataList || [];
        this.tableData = responseList.map((i, ind) => ({
          ...i,
          tempId: ind + 1,
        }));

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

  @HostListener('window:beforeunload', ['$event'])
  handleBeforeUnload(event: BeforeUnloadEvent) {
    if (this.data && this.data?.isMvnoDocument) {
      event.preventDefault();
    } else {
      this.onClose();
    }
  }

  canDeactivate(): boolean {
    if (this.data == null || !this.data?.isMvnoDocument) return true;

    const confirmLeave = window.confirm(
      'Changes will be lost. Do you want to go back?',
    );

    if (!confirmLeave) {
      this.commonService.spinnerHide();
    }

    return confirmLeave;
  }

  downloadDoc(filename: string, docId: number, mvnoId: number): void {
    this.mvnoService
      .downloadFile(docId, mvnoId)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
        catchError((error) => {
          this.commonService.toastError(
            error?.error?.msg ||
              error?.error?.error ||
              error?.error?.ERROR ||
              'Something went wrong while fetching data',
          );

          return EMPTY;
        }),
      )
      .subscribe((blob) => {
        importedSaveAs(blob, filename);
        this.commonService.toastSuccess('Download Successfully.');
      });
  }

  onClose() {
    this.data?.isMvnoDocument ? (this.data.isMvnoDocument = false) : null;
    this.router.navigateByUrl(routes.mvnoManagement, {
      replaceUrl: true,
    });
  }

  openAddEdit(item: any = {}): void {
    this.router.navigate(
      [`${routes.mvnoManagement}/document`, this.data.id, 'add-edit'],
      {
        state: {
          ...(item ?? {}),
          isAddEdit: true,
          mvnoId: this.data.id,
          isEdit: !!item.docId,
        },
      },
    );
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  openApproveRejectModal(document: any, isApprove: boolean): void {
    if (this.isDocumentDisabled(document)) return;

    this.selectedDocument = document;
    this.ifApproveStatus = isApprove;
    this.approveRejectRemark.setValue('');
    this.approveRejectModal = true;
  }

  closeApproveRejectModal(): void {
    this.approveRejectModal = false;
    this.selectedDocument = null;
    this.approveRejectRemark.setValue('');
  }

  statusApproveReject(): void {
    if (!this.approveRejectRemark.value || !this.selectedDocument) return;

    this.commonService.spinnerShow();

    const url = `/mvno/mvnoDoc/approveUploadMvnoDoc?docId=${this.selectedDocument.docId}&remarks=${this.approveRejectRemark.value}&isApproveRequest=${this.ifApproveStatus}`;

    const payload = {
      agentPaymentId: this.selectedDocument.docId,
      nextStaffId: '',
      flag: this.ifApproveStatus ? 'approved' : 'rejected',
      remark: this.approveRejectRemark.value,
      staffId: localStorage.getItem('userId'),
    };

    this.mvnoService
      .updateDocumentStatus(url, payload)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
      )
      .subscribe((response: any) => {
        if (response.dataList != null && response.dataList.length > 0) {
          this.approveRejectModal = false;
        } else {
          this.approveRejectModal = false;
          this.getTableData({ skip: 0, limit: this.pageSize });
        }

        this.commonService.toastSuccess(
          response.responseMessage || 'Document status updated successfully',
        );
      });
  }

  private isDocumentDisabled(document: any): boolean {
    return (
      document.docStatus == 'Rejected' ||
      (document.docStatus == 'Verified' && document.mode === 'Online') ||
      document.docStatus == 'verified'
    );
  }
}
