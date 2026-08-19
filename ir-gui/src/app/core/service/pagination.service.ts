import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { pageSize, pageSizeCal, tablePageSize } from '../../core.index';

@Injectable({
  providedIn: 'root',
})
export class PaginationService {
  tablePageSize: BehaviorSubject<tablePageSize> =
    new BehaviorSubject<tablePageSize>({ skip: 0, limit: 10, pageSize: 10 });

  calculatePageSize: BehaviorSubject<pageSizeCal> =
    new BehaviorSubject<pageSizeCal>({
      totalData: 0,
      pageSize: 10,
      tableData: [],
      tableDataCopy: [],
      serialNumberArray: [],
    });
  changePagesize: BehaviorSubject<pageSize> = new BehaviorSubject<pageSize>({
    pageSize: 10,
  });
}
