import { CommonModule } from "@angular/common";
import { Component, OnDestroy, OnInit } from "@angular/core";
import { FormControl, UntypedFormControl } from "@angular/forms";
import {
  CommonService,
  IPermission,
  pageSelection,
  PaginationService,
  sharedModule,
  SidebarService,
  tablePageSize,
} from "src/app/core.index";
import { CustomElementModule } from "src/app/core/shared/custom-elements/custom-elemets.module";
import { NotificationTemplateListService } from "./notification-template-list.service";
import { ChildMenuEnum, MenuEnum } from "src/app/core/enums/sidebar-menu.enum";
import {
  catchError,
  debounceTime,
  EMPTY,
  finalize,
  of,
  Subject,
  takeUntil,
} from "rxjs";
import { INotificationTemplate } from "./notification-template-list.interface";
import { HttpParams } from "@angular/common/http";
import { SelectModule } from 'primeng/select';
import { FloatLabel } from 'primeng/floatlabel';

type INotificationTemplateExtend = INotificationTemplate & {
  smsEventConfiguredControl: UntypedFormControl;
  emailEventConfiguredControl: UntypedFormControl;
  emailTemplateDataControl: UntypedFormControl;
  smsTemplateDataControl: UntypedFormControl;
};

@Component({
  selector: "app-notification-template",
  imports: [sharedModule, CustomElementModule, CommonModule, SelectModule, FloatLabel],
  templateUrl: "./notification-template-list.component.html",
  styleUrl: "./notification-template-list.component.scss",
})
export class NotificationTemplateListComponent implements OnInit, OnDestroy {
  tableData: Array<INotificationTemplateExtend> = [];
  pageSize = 10;
  totalData = 0;
  serialNumberArray: number[] = [];
  paginationSkip = 0;
  searchDataValue = new FormControl("");
  isCollapsed = false;
  private destroy$ = new Subject<void>();
  private lastPayload: string = "";
  permission: IPermission;

  searchOptionSelect = [
    { label: 'Event Name', value: 'eventName' },
    { label: 'BU Name', value: 'buName' },
    { label: 'Append URL', value: 'appendUrl' },
    { label: 'SMS Template', value: 'smsTemplateData' },
    { label: 'Email Template', value: 'emailTemplateData' },
  ];

  searchDetailControl = new FormControl('');
  searchOptionControl = new FormControl(null);

  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private notifTempService: NotificationTemplateListService,
    private commonService: CommonService
  ) {
    this.permission = this.commonService.hasPermission([
      MenuEnum.SETTING,
      ChildMenuEnum.NOTIFICATION_TEMPLATE,
      "template_save",
    ]);
  }

  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        const searchValue = this.searchDetailControl.value?.trim() || "";
        const currentPayload = JSON.stringify({
          skip: res.skip,
          limit: res.pageSize,
          search: searchValue,
          searchOption: this.searchOptionControl.value,
        });

        
        if (this.lastPayload !== currentPayload) {
          this.lastPayload = currentPayload;
          this.getTableData({ skip: res.skip, limit: res.pageSize });
          this.pageSize = res.pageSize;
        }
      });
  }

  
  getSearchPlaceholder(): string {
    if (!this.searchOptionControl.value) {
      return 'Select a search option first';
    }
    
    const selectedOption = this.searchOptionSelect.find(
      option => option.value === this.searchOptionControl.value
    );
    
    return selectedOption ? `Search by ${selectedOption.label}` : 'Search';
  }

  private getTableData(pageOption: pageSelection): void {
    const page = pageOption.skip / pageOption.limit;
    const pageSize = pageOption.limit;
    const searchValue = this.searchDetailControl.value?.trim() || "";
    const searchOption = this.searchOptionControl.value;
    const url = `search?page=${page}&pageSize=${pageSize}&sortBy=Active&sortOrder=10`;

    let params = new HttpParams()
      .set("page", String(page))
      .set("pageSize", String(pageSize))
      .set("sortBy", "Active")
      .set("sortOrder", "10");

    this.commonService.spinnerShow();

    if (searchValue && searchOption) {
      const body = {
        filter: [
          {
            filterDataType: "",
            templateName: searchValue,
            filterColumn: searchOption, 
            filterOperator: "equalto",
            filterCondition: "and",
          },
        ],
      };

      this.notifTempService
        .searchMethod(url, body)
        .pipe(
          takeUntil(this.destroy$),
          finalize(() => this.commonService.spinnerHide()),
          catchError((error) => {
            this.commonService.toastError(
              error?.error?.error || "Something went wrong while fetching data"
            );
            return of({ dataList: [], totalRecords: 0, responseCode: 500 });
          })
        )
        .subscribe((res: any) => {
          if (res?.responseCode === 404) {
            this.tableData = [];
            this.totalData = 0;
            this.pagination.calculatePageSize.next({
              totalData: 0,
              pageSize: this.pageSize,
              tableData: [],
              serialNumberArray: [],
            });
            this.commonService.toastInfo(
              res?.responseMessage || "No Record Found!"
            );
            return;
          }

          const responseList: INotificationTemplate[] = res?.dataList || [];
          this.totalData = res?.totalRecords || 0;
          this.tableData = responseList.map(this.addControls);
          this.serialNumberArray = this.tableData.map((_, i) => i + 1);
          this.pagination.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: this.pageSize,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });
        });
      return;
    } else {
      this.notifTempService
        .searchTemplate(params)
        .pipe(
          takeUntil(this.destroy$),
          finalize(() => this.commonService.spinnerHide()),
          catchError((error) => {
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
                error?.error?.error ||
                  "Something went wrong while fetching data"
              );
            }
            return of({ templateList: [] });
          })
        )
        .subscribe((res) => {
          const responseList: INotificationTemplate[] = res.templateList || [];
          this.totalData = res.pageDetails?.totalRecords || 0;
          this.tableData = responseList.map(this.addControls);
          this.serialNumberArray = this.tableData.map((_, i) => i + 1);
          this.pagination.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: this.pageSize,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });
        });
    }
  }

  private addControls(
    data: INotificationTemplate
  ): INotificationTemplateExtend {
    const item = {
      ...data,
      smsEventConfiguredControl: new UntypedFormControl(
        data.smsEventConfigured || false
      ),
      smsTemplateDataControl: new UntypedFormControl(
        data.smsTemplateData || ""
      ),
      emailEventConfiguredControl: new UntypedFormControl(
        data.emailEventConfigured || false
      ),
      emailTemplateDataControl: new UntypedFormControl(
        data.emailTemplateData || ""
      ),
    };
    return item;
  }

  searchData(): void {
    
    if (!this.searchOptionControl.value) {
      this.commonService.toastInfo('Please select a search option');
      return;
    }

    
    if (!this.searchDetailControl.value?.trim()) {
      this.commonService.toastInfo('Please enter a search value');
      return;
    }

    
    this.lastPayload = "";
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  clearSearch(): void {
    this.searchDataValue.setValue("");
    this.searchDetailControl.setValue('');
    this.searchOptionControl.setValue(null);
    this.lastPayload = "";
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  saveOn(data: INotificationTemplateExtend): void {
    const {
      emailEventConfiguredControl,
      emailTemplateDataControl,
      smsEventConfiguredControl,
      smsTemplateDataControl,
      ...payload
    } = data;

    payload.smsEventConfigured = smsEventConfiguredControl.value;
    payload.smsTemplateData = smsTemplateDataControl.value;
    payload.emailEventConfigured = emailEventConfiguredControl.value;
    payload.emailTemplateData = emailTemplateDataControl.value;

    this.notifTempService
      .updateTemplate(data.templateId, payload)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.commonService.toastError(
            error?.error?.error ?? "Something went wrong while update data"
          );
          return EMPTY;
        })
      )
      .subscribe((res) => {
        if (res.status == 200) {
          this.commonService.toastSuccess(res?.message);
          this.searchData();
        } else {
          this.commonService.toastError(res?.message);
        }
      });
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}