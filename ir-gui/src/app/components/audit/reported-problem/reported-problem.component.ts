import { Component } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { catchError, debounceTime, finalize, Subject, takeUntil } from 'rxjs';
import { ChildMenuEnum, MenuEnum } from 'src/app/core/enums/sidebar-menu.enum';
import {
  IPermission,
  pageSelection,
  tablePageSize,
} from 'src/app/core/models/models';
import { CommonService } from 'src/app/core/service/common.service';
import { PaginationService } from 'src/app/core/service/pagination.service';
import { SidebarService } from 'src/app/core/service/sidebar.service';
import { ReportedProblemService } from './reported-problem.service';
import { CustomPaginationModule } from 'src/app/core/shared/custom-pagination/custom-pagination.module';

@Component({
  selector: 'app-reported-problem',
  imports: [CustomPaginationModule, ReactiveFormsModule],
  templateUrl: './reported-problem.component.html',
  styleUrl: './reported-problem.component.scss',
})
export class ReportedProblemComponent {
  tableData: any[] = [];
  pageSize = 10;
  totalData = 0;
  serialNumberArray: number[] = [];
  paginationSkip = 0;
  searchDataValue = new FormControl('');
  isCollapsed = false;
  private destroy$ = new Subject<void>();
  private lastPayload: string = '';
  permission: IPermission;

  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private commonService: CommonService,
    private reportedProblemService: ReportedProblemService,
  ) {
    this.permission = this.commonService.hasPermission([
      MenuEnum.AUDIT,
      ChildMenuEnum.REPORTED_PROBLEM,
    ]);
  }

  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        const searchValue = this.searchDataValue.value?.trim() || '';
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
    const searchValue = this.searchDataValue.value?.trim();
    let url = '';
    if (searchValue) {
      url = `/reportproblem/pagination?page=${page}&pageSize=${pageOption.limit}&sortBy=report_id&sortOrder=0`;
    } else {
      url = '/reportproblem';
    }
    const payload: any = searchValue
      ? {
          filter: [
            {
              filterDataType: '',
              filterValue: searchValue,
              filterColumn: 'phno',
              filterOperator: '',
              filterCondition: '',
            },
          ],
        }
      : {
          page,
          pageSize: pageOption.limit,
        };
    this.commonService.spinnerShow();
    this.reportedProblemService
      .postMethod(url, payload)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.commonService.spinnerHide();
        }),
        catchError((error) => {
          this.commonService.toastError(error?.error?.ERROR);
          return error;
        }),
      )
      .subscribe((res: any) => {
        if (res?.responseCode == 204 || res?.responseCode == 404) {
          this.tableData = [];
          this.totalData = 0;

          this.pagination.calculatePageSize.next({
            totalData: 0,
            pageSize: this.pageSize,
            tableData: [],
            serialNumberArray: [],
          });
          this.commonService.toastInfo(res?.responseMessage);
        } else if (res?.responseCode == 200) {
          this.commonService.spinnerHide();
          const responseList: any[] = res.dataList || [];
          this.totalData = res?.totalRecords || 0;
          this.tableData = responseList;
          this.serialNumberArray = this.tableData.map((_, i) => i + 1);
          this.pagination.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: this.pageSize,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });
        } else {
          this.commonService.toastInfo(res?.responseMessage);
        }
      });
  }

  public searchData(): void {
    this.lastPayload = '';
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  public clearSearch(): void {
    this.searchDataValue.setValue('');
    this.lastPayload = '';
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

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
