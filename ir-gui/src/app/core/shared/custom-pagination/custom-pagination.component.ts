import { Component } from '@angular/core';
import {
  pageSelection,
  pageSize,
  pageSizeCal,
  PaginationService,
} from 'src/app/core.index';

@Component({
  selector: 'app-custom-pagination',
  templateUrl: './custom-pagination.component.html',
  styleUrls: ['./custom-pagination.component.scss'],
  standalone: false,
})
export class CustomPaginationComponent {
  public pageSize: number = 10;
  public tableData: Array<string> = [];

  public lastIndex = 0;
  public totalData = 0;
  public skip = 0;
  public limit: number = this.pageSize;
  public pageIndex = 0;
  public serialNumberArray: Array<number> = [];
  public currentPage = 1;
  public pageNumberArray: Array<number> = [];
  public pageSelection: Array<pageSelection> = [];
  public totalPages = 0;

  constructor(private pagination: PaginationService) {
    this.tableData = [];
    this.pagination.calculatePageSize.subscribe((res: pageSizeCal) => {
      this.calculateTotalPages(
        res.totalData,
        res.pageSize,
        res.tableData,
        res.serialNumberArray,
      );
      this.pageSize = res.pageSize;
    });

    this.pagination.changePagesize.subscribe((res: pageSize) => {
      this.changePageSize(res.pageSize);
    });
  }

  public getMoreData(event: string): void {
    if (event === 'next' && this.currentPage < this.totalPages) {
      this.currentPage++;
    } else if (event === 'previous' && this.currentPage > 1) {
      this.currentPage--;
    } else {
      return;
    }

    this.pageIndex = this.currentPage - 1;
    this.skip = this.pageSize * this.pageIndex;
    this.limit = this.skip + this.pageSize;

    this.pagination.tablePageSize.next({
      skip: this.skip,
      limit: this.limit,
      pageSize: this.pageSize,
    });
  }

  public moveToPage(pageNumber: number): void {
    if (
      pageNumber === this.currentPage ||
      pageNumber < 1 ||
      pageNumber > this.totalPages
    ) {
      return;
    }

    this.currentPage = pageNumber;
    this.pageIndex = this.currentPage - 1;
    this.skip = this.pageSize * this.pageIndex;
    this.limit = this.skip + this.pageSize;

    this.pagination.tablePageSize.next({
      skip: this.skip,
      limit: this.limit,
      pageSize: this.pageSize,
    });
  }

  public changePageSize(pageSize: number): void {
    this.pageSize = pageSize;
    this.pageSelection = [];
    this.limit = pageSize;
    this.skip = 0;
    this.currentPage = 1;

    this.pagination.tablePageSize.next({
      skip: this.skip,
      limit: this.limit,
      pageSize: this.pageSize,
    });
  }

  public calculateTotalPages(
    totalData: number,
    pageSize: number,
    tableData: Array<string>,
    serialNumberArray: Array<number>,
  ): void {
    this.tableData = tableData;
    this.serialNumberArray = serialNumberArray;
    this.totalData = totalData;
    this.totalPages = Math.ceil(totalData / pageSize);
    this.pageSelection = [];

    for (let i = 1; i <= this.totalPages; i++) {
      const limit = pageSize * i;
      const skip = limit - pageSize;
      this.pageSelection.push({ skip: skip, limit: limit });
    }
  }

public getVisiblePages(): number[] {
  const pages: number[] = [];

  if (this.totalPages <= 5) {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  pages.push(1);

  if (this.currentPage <= 3) {
    pages.push(2, 3, 4);
    pages.push(-1);
    pages.push(this.totalPages);
    return pages;
  }

  if (this.currentPage >= this.totalPages - 2) {
    pages.push(-1);
    pages.push(
      this.totalPages - 3,
      this.totalPages - 2,
      this.totalPages - 1,
      this.totalPages
    );
    return pages;
  }

  pages.push(-1);
  pages.push(
    this.currentPage - 1,
    this.currentPage,
    this.currentPage + 1
  );
  pages.push(-1);
  pages.push(this.totalPages);

  return pages;
}

}