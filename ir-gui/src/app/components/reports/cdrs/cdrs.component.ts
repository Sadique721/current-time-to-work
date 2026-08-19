import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormBuilder, FormGroup } from '@angular/forms';
import { Subject, takeUntil, debounceTime } from 'rxjs';
import { Router } from '@angular/router';
import { routes, SidebarService, CommonService, PaginationService } from 'src/app/core.index';
import { CdrsService } from './cdrs.service';

interface CDRSRow {
  id: number;
  date: Date;
  callerIdNumber: string;
  phoneNumber: string;
  user?: string;
  duration: string;
  callMode: string;
  disposition?: string;
  hangupCause: string;
  direction: string;
  ivrGroup?: string;
  callType: string;
}

@Component({
  selector: 'app-cdrs',
  templateUrl: './cdrs.component.html',
  styleUrl: './cdrs.component.scss',
  standalone: false,
})
export class CdrsComponent implements OnInit, OnDestroy {
  public routes = routes;

  tableData: CDRSRow[] = [];
  pageSize = 10;
  totalData = 0;
  paginationSkip = 0;
  currentPage = 1;
  serialNumberArray: Array<number> = [];

  searchDataValue = new FormControl('');
  filtersForm: FormGroup;
  isCollapsed = false;
  isLoading = false;
  isSearchMode = false;
  isFiltersOpen = false;

  minDate = new Date();

  private isUpdatingPagination = false;
  private destroy$ = new Subject<void>();

  
  conditionOptions = [
    { label: 'Begin with', value: 'beginWith' },
    { label: 'Contains', value: 'contains' },
    { label: 'Ends with', value: 'endsWith' },
    { label: 'Equals', value: 'equals' },
  ];

  cdrsArchiveOptions = [
    { label: 'Archive 2024', value: 'archive_2024' },
    { label: 'Archive 2025', value: 'archive_2025' },
    { label: 'Archive 2026', value: 'archive_2026' },
  ];

  callerIdNameOptions = [
    { label: 'Caller ID 1', value: 'callerid1' },
    { label: 'Caller ID 2', value: 'callerid2' },
  ];

  userOptions = [
    { label: 'User 1', value: 'user1' },
    { label: 'User 2', value: 'user2' },
    { label: 'Admin', value: 'admin' },
  ];

  ivrGroupOptions = [
    { label: 'IVR Group 1', value: 'ivr1' },
    { label: 'IVR Group 2', value: 'ivr2' },
  ];

  callModeOptions = [
    { label: 'PBX', value: 'PBX' },
    { label: 'Dialer', value: 'Dialer' },
    { label: 'Manual', value: 'Manual' },
  ];

  hangupCauseOptions = [
    { label: 'NORMAL_CLEARING', value: 'NORMAL_CLEARING' },
    { label: 'CALLQUEUE_TIMEOUT', value: 'CALLQUEUE_TIMEOUT' },
    { label: 'USER_BUSY', value: 'USER_BUSY' },
    { label: 'NO_ANSWER', value: 'NO_ANSWER' },
  ];

  directionOptions = [
    { label: 'Inbound', value: 'Inbound' },
    { label: 'Outbound', value: 'Outbound' },
  ];

  callTypeOptions = [
    { label: 'DID', value: 'DID' },
    { label: 'Extension', value: 'Extension' },
    { label: 'External', value: 'External' },
  ];

  constructor(
    private router: Router,
    private fb: FormBuilder,
    private cdrsService: CdrsService,
    private sidebar: SidebarService,
    private commonService: CommonService,
    private paginationService: PaginationService
  ) {
    this.filtersForm = this.fb.group({
      cdrsArchive: [''],
      startDate: [null],
      endDate: [null],
      callerIdName: [''],
      callerIdNameCondition: ['beginWith'],
      callerIdNumber: [''],
      callerIdNumberCondition: ['beginWith'],
      phoneNumber: [''],
      phoneNumberCondition: ['beginWith'],
      user: [''],
      ivrGroup: [''],
      duration: [''],
      durationCondition: ['beginWith'],
      callMode: [''],
      hangupCause: [''],
      direction: [''],
      callType: ['']
    });
  }

  ngOnInit(): void {
        
    this.paginationService.tablePageSize
      .pipe(takeUntil(this.destroy$), debounceTime(50))
      .subscribe((res: any) => {
        if (this.isUpdatingPagination || this.isLoading) return;
        
        const newSkip = res.skip;
        const newPageSize = res.pageSize;
        const newPage = Math.floor(newSkip / newPageSize) + 1;
        
        if (newSkip === this.paginationSkip && newPageSize === this.pageSize) return;
        
        this.paginationSkip = newSkip;
        this.pageSize = newPageSize;
        this.currentPage = newPage;
        
        if (this.isSearchMode) {
          const searchTerm = (this.searchDataValue.value || '').toString().trim();
          this.performSearch(searchTerm);
        } else {
          this.loadCDRS();
        }
      });

    this.loadCDRS();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  private loadCDRS(): void {
    this.isLoading = true;
    this.commonService.spinnerShow();

    this.cdrsService.getAll(this.currentPage, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.tableData = response.data;

          this.totalData = response.totalCount;
          this.serialNumberArray = Array.from(
            { length: this.tableData.length },
            (_, i) => this.paginationSkip + i + 1
          );

          this.isUpdatingPagination = true;
          this.paginationService.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: this.pageSize,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });
          setTimeout(() => { this.isUpdatingPagination = false; }, 100);

          this.isLoading = false;
          this.commonService.spinnerHide();
        },
        error: (error) => {
                    this.handleError();
        }
      });
  }

  private handleError(): void {
    this.isLoading = false;
    this.commonService.spinnerHide();
    this.tableData = [];
    this.totalData = 0;
    this.isUpdatingPagination = true;
    this.paginationService.calculatePageSize.next({
      totalData: 0,
      pageSize: this.pageSize,
      tableData: [],
      serialNumberArray: [],
    });
    setTimeout(() => { this.isUpdatingPagination = false; }, 100);
  }

  searchData(): void {
    const searchTerm = (this.searchDataValue.value || '').toString().trim();
    if (!searchTerm) {
      this.isSearchMode = false;
      this.currentPage = 1;
      this.paginationSkip = 0;
      this.loadCDRS();
      return;
    }
    this.isSearchMode = true;
    this.currentPage = 1;
    this.paginationSkip = 0;
    this.performSearch(searchTerm);
  }

  private performSearch(searchTerm: string): void {
    this.isLoading = true;
    this.commonService.spinnerShow();

    this.cdrsService.search(searchTerm, this.currentPage, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.tableData = response.data;

          this.totalData = response.totalCount;
          this.serialNumberArray = Array.from(
            { length: this.tableData.length },
            (_, i) => this.paginationSkip + i + 1
          );

          this.isUpdatingPagination = true;
          this.paginationService.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: this.pageSize,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });
          setTimeout(() => { this.isUpdatingPagination = false; }, 100);

          this.isLoading = false;
          this.commonService.spinnerHide();
        },
        error: () => {
          this.commonService.toastError('Search failed');
          this.handleError();
        }
      });
  }

  toggleFilters(): void {
    this.isFiltersOpen = !this.isFiltersOpen;

    if (this.isFiltersOpen) {
      document.body.classList.add('filters-open');
    } else {
      document.body.classList.remove('filters-open');
    }
  }


  refreshData(): void {
    this.currentPage = 1;
    this.paginationSkip = 0;
    this.searchDataValue.setValue('');
    this.isSearchMode = false;
    
    this.filtersForm.reset({
      startDate: null,
      endDate: null,
      callerIdNameCondition: 'beginWith',
      callerIdNumberCondition: 'beginWith',
      phoneNumberCondition: 'beginWith',
      durationCondition: 'beginWith'
    });
    
    this.loadCDRS();
    this.commonService.toastSuccess('Data refreshed');
  }

  exportToCSV(): void {
    if (this.tableData.length === 0) {
      this.commonService.toastError('No data available to export');
      return;
    }

    this.generateCSV(this.tableData);
    this.commonService.toastSuccess('CSV exported successfully');
  }

  private generateCSV(data: CDRSRow[]): void {
    
    const headers = [
      'Date',
      'Caller Id Number',
      'Phone Number',
      'User',
      'Duration',
      'Call Mode',
      'Disposition',
      'Hangup Cause',
      'Direction',
      'IVR Group',
      'Call Type'
    ];

    
    const csvRows = data.map(row => [
      this.formatDate(row.date),
      row.callerIdNumber,
      row.phoneNumber,
      row.user || '-',
      row.duration,
      row.callMode,
      row.disposition || '-',
      row.hangupCause,
      row.direction,
      row.ivrGroup || '-',
      row.callType
    ]);

    
    const csvContent = [
      headers.join(','),
      ...csvRows.map(row => row.map(cell => `"${cell}"`).join(','))
    ].join('\n');

    
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    
    link.setAttribute('href', url);
    link.setAttribute('download', `cdrs-${this.formatDateForFilename(new Date())}.csv`);
    link.style.visibility = 'hidden';
    
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    
    URL.revokeObjectURL(url);
  }

  private formatDate(date: Date): string {
    if (!date) return '';
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    const seconds = String(d.getSeconds()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  }

  private formatDateForFilename(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}${month}${day}`;
  }


  clearSearch(): void {
        this.searchDataValue.setValue('');
    this.isSearchMode = false;
    this.currentPage = 1;
    this.paginationSkip = 0;
    this.loadCDRS();
  }
}