import { Component, OnDestroy, OnInit } from '@angular/core';
import { AuditLogModules, ModuleList } from './audit-log.constant';
import { SelectModule } from 'primeng/select';
import { PaginationService } from 'src/app/core/service/pagination.service';
import { SidebarService } from 'src/app/core/service/sidebar.service';
import { AuditLogService } from './audit-log.service';
import { CommonService } from 'src/app/core/service/common.service';
import {
IPermission,
pageSelection,
tablePageSize,
} from 'src/app/core/models/models';
import { ChildMenuEnum, MenuEnum } from 'src/app/core/enums/sidebar-menu.enum';
import { CommonModule } from '@angular/common';
import { FloatLabel } from 'primeng/floatlabel';
import { catchError, finalize, Subject, takeUntil, throwError } from 'rxjs';
import { UntypedFormControl } from '@angular/forms';
import { DatePicker } from 'primeng/datepicker';
import { CustomPaginationModule } from 'src/app/core/shared/custom-pagination/custom-pagination.module';
import { sharedModule } from 'src/app/core/shared/shared.module';
import { CustomElementModule } from 'src/app/core/shared/custom-elements/custom-elemets.module';
import { MatDialog } from '@angular/material/dialog';
import { SnapshotDetailsComponent } from './snapshot-details/snapshot-details.component';
import customDatetime from 'src/app/core/shared/custom-elements/custom-datetime.pipe';

@Component({
selector: 'app-audit-log',
imports: [
SelectModule,
CommonModule,
FloatLabel,
CustomPaginationModule,
sharedModule,
CustomElementModule,
],
templateUrl: './audit-log.component.html',
styleUrl: './audit-log.component.scss',
})
export class AuditLogComponent implements OnInit, OnDestroy {
private destroy$ = new Subject<void>();
moduleList = ModuleList;
selectedModule = AuditLogModules.COMMON;
selectedModuleName = 'Common';
searchKey = new UntypedFormControl('');
startDate = new UntypedFormControl(null);
endDate = new UntypedFormControl(null);
tableData: any[] = [];
serialNumberArray: number[] = [];
paginationSkip = 0;
pageSize = 10;
totalData = 0;
private lastPayload = '';
isCollapsed = false;
permission: IPermission;
constructor(
private pagination: PaginationService,
private sidebar: SidebarService,
private auditLogService: AuditLogService,
private commonService: CommonService,
private dialog: MatDialog,
) {
this.permission = this.commonService.hasPermission([
MenuEnum.AUDIT,
ChildMenuEnum.AUDIT_LOG,
]);
}
ngOnInit(): void {
this.setupPaginationListener();
}
private setupPaginationListener() {
this.pagination.tablePageSize
.pipe(takeUntil(this.destroy$))
.subscribe((res: tablePageSize) => {
this.paginationSkip = res.skip;
const payload = {
moduleName: this.getModuleName(this.selectedModule),
entityName: this.searchKey?.value.trim() || '',
startDate: this.startDate?.value
? customDatetime(this.startDate?.value?.toISOString(), {
format: 'YYYY-MM-DD',
})
: null,
endDate: this.endDate?.value
? customDatetime(this.endDate?.value?.toISOString(), {
format: 'YYYY-MM-DD',
})
: null,
pageIndex: res.skip / res.pageSize,
pageSize: res.pageSize,
};
const key = JSON.stringify(payload);
if (this.lastPayload !== key) {
this.lastPayload = key;
this.pageSize = res.pageSize;
if (!payload.entityName && !payload.startDate && !payload.endDate) {
this.loadAuditLogs(payload);
} else {
this.searchAudit({ ...payload, pageIndex: payload.pageIndex + 1 });
}
}
});
this.pagination.tablePageSize.next({
skip: 0,
limit: this.pageSize,
pageSize: this.pageSize,
});
}
private loadAuditLogs(payload: any) {
this.commonService.spinnerShow();
const url = `/auditTrail/all?pageIndex=${payload.pageIndex}&pageSize=${payload.pageSize}`;
const url1 = `/audit/all?page=${payload.pageIndex}&pageSize=${payload.pageSize}`;
let apiCall$;
switch (this.selectedModule) {
case AuditLogModules.CMS:
apiCall$ = this.auditLogService.getProductManagementLog(url);
break;
case AuditLogModules.INVENTORY:
apiCall$ = this.auditLogService.getInventoryLog(url);
break;
case AuditLogModules.TICKET:
apiCall$ = this.auditLogService.getTicketManagementLog(url);
break;
case AuditLogModules.REVENUE:
apiCall$ = this.auditLogService.generatePaymentReceiptLog(url);
break;
case AuditLogModules.NOTIFICATION:
apiCall$ = this.auditLogService.getNotificationLog(url);
break;
case AuditLogModules.CUSTOMER:
apiCall$ = this.auditLogService.getCustomerLog(url1);
break;
default:
apiCall$ = this.auditLogService.getCommonManagementLog(url);
}
apiCall$
.pipe(
takeUntil(this.destroy$),
finalize(() => this.commonService.spinnerHide()),
catchError((error) => {
this.commonService.toastError(
error?.error?.ERROR || 'Something went wrong please try again!',
);
return throwError(() => error);
}),
)
.subscribe((res: any) => {
if (this.selectedModule === AuditLogModules.CUSTOMER) {
if (res?.responseCode === 204 || res?.responseCode === 404) {
this.clearTableData();
this.commonService.toastInfo(res?.responseMessage || 'No data found');
} else if (res?.responseCode === 200) {
this.tableData =
(res.dataList || []).map((i: any, ind: number) => ({
...i,
entityName: i.profileName,
entityType: i.entityType,
moduleName: i.module,
authorUserName: i.username,
authorUserTeams: (i.userTeam || []).join(', '),
updatedOn: i.auditDate,
tempTrackBy: ind + 1,
})) || [];
this.totalData = res.totalRecords || 0;
this.updatePagination();
} else {
this.commonService.toastInfo(res?.responseMessage || 'No data found');
}
return;
}
if (res?.status === 204 || res?.status === 404) {
this.clearTableData();
this.commonService.toastInfo(res?.responseMessage || 'No data found');
} else if (res?.status === 200) {
this.tableData =
res.byObject?.map((i: any, ind: number) => ({
...i,
tempTrackBy: ind + 1,
})) || [];
this.totalData = res.totalRecords || 0;
this.updatePagination();
} else {
this.commonService.toastInfo(res?.responseMessage || 'No data found');
}
});
}
private searchAudit(payload: any) {
const url = '/auditTrail/byModule';
const url1 = '/audit/search';
this.commonService.spinnerShow();
let apiCall$;
switch (this.selectedModule) {
case AuditLogModules.CMS:
apiCall$ = this.auditLogService.postProductManagementLog(url, payload);
break;
case AuditLogModules.INVENTORY:
apiCall$ = this.auditLogService.postInventoryLog(url, payload);
break;
case AuditLogModules.TICKET:
apiCall$ = this.auditLogService.postTicketManagementLog(url, payload);
break;
case AuditLogModules.REVENUE:
apiCall$ = this.auditLogService.postRevenueLog(url, payload);
break;
case AuditLogModules.NOTIFICATION:
apiCall$ = this.auditLogService.postNotificationLog(url, payload);
break;
case AuditLogModules.CUSTOMER:
apiCall$ = this.auditLogService.postCustomerLog(url1, payload);
break;
default:
apiCall$ = this.auditLogService.postCommonManagementLog(url, payload);
}
apiCall$
.pipe(
takeUntil(this.destroy$),
finalize(() => this.commonService.spinnerHide()),
catchError((error) => {
this.commonService.toastError(error?.error?.ERROR || 'Search failed');
return throwError(() => error);
}),
)
.subscribe((res: any) => {
if (this.selectedModule === AuditLogModules.CUSTOMER) {
if (res?.responseCode === 204 || res?.responseCode === 404) {
this.clearTableData();
this.commonService.toastInfo(res?.responseMessage || 'No data found');
} else if (res?.responseCode === 200) {
this.tableData =
(res.dataList || []).map((i: any, ind: number) => ({
...i,
entityName: i.profileName,
entityType: i.entityType,
moduleName: i.module,
authorUserName: i.username,
authorUserTeams: (i.userTeam || []).join(', '),
updatedOn: i.auditDate,
tempTrackBy: ind + 1,
})) || [];
this.totalData = res.totalRecords || this.tableData.length;
this.serialNumberArray = this.tableData.map((_, i) => i + 1);
this.updatePagination();
} else {
this.commonService.toastInfo(res?.responseMessage || 'No data found');
}
return;
}
if (res?.responseCode === 204 || res?.responseCode === 404) {
this.clearTableData();
this.commonService.toastInfo(res?.responseMessage || 'No data found');
} else if (res?.responseCode === 200) {
this.tableData = res?.data || [];
this.totalData = res.totalRecords || 0;
this.serialNumberArray = this.tableData.map((_, i) => i + 1);
this.updatePagination();
} else {
this.commonService.toastInfo(res?.responseMessage);
}
});
}
private clearTableData() {
this.tableData = [];
this.totalData = 0;
this.serialNumberArray = [];
this.updatePagination();
}
private updatePagination() {
this.pagination.calculatePageSize.next({
totalData: this.totalData,
pageSize: this.pageSize,
tableData: this.tableData,
serialNumberArray: this.serialNumberArray,
});
}
private getModuleName(mod: AuditLogModules): string {
switch (mod) {
case AuditLogModules.CMS:
return 'CMS';
case AuditLogModules.INVENTORY:
return 'Inventory Management';
case AuditLogModules.TICKET:
return 'Ticket Management';
case AuditLogModules.REVENUE:
return 'Revenue Management';
case AuditLogModules.NOTIFICATION:
return 'Notification Management';
case AuditLogModules.CUSTOMER:
return 'Customer';
default:
return 'Common GateWay';
}
}
onModuleChange(event: any) {
this.selectedModule = event.value;
this.selectedModuleName = event.label;
this.lastPayload = '';
this.searchKey.setValue('');
this.startDate.setValue(null);
this.endDate.setValue(null);
this.pagination.tablePageSize.next({
skip: 0,
limit: this.pageSize,
pageSize: this.pageSize,
});
}
onSearch() {
this.lastPayload = '';
this.pagination.tablePageSize.next({
skip: 0,
limit: this.pageSize,
pageSize: this.pageSize,
});
}
clearSearch() {
this.searchKey.setValue('');
this.startDate.setValue(null);
this.endDate.setValue(null);
this.lastPayload = '';
this.pagination.tablePageSize.next({
skip: 0,
limit: this.pageSize,
pageSize: this.pageSize,
});
}
public openSnapshot(auditLogDetail: any): void {
let snapshot = '';
if (this.selectedModule === AuditLogModules.CUSTOMER && auditLogDetail?.entityState) {
try {
snapshot = JSON.stringify(JSON.parse(auditLogDetail.entityState), null, 2);
} catch {
snapshot = auditLogDetail.entityState || 'No snapshot data';
}
} else if (auditLogDetail.snapshot) {
snapshot = auditLogDetail.snapshot;
} else if (auditLogDetail.entityObject) {
snapshot = JSON.stringify(auditLogDetail.entityObject, null, 2);
} else {
snapshot = 'No snapshot data';
}
this.dialog.open(SnapshotDetailsComponent, {
width: '420px',
disableClose: true,
data: { snapshot },
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
