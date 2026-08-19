import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { StaffManagementService } from '../staff-management.service';
import { CommonService } from 'src/app/core/service/common.service';

@Injectable({ providedIn: 'root' })
export class CurrencyConfigurationResolver implements Resolve<string | null> {
  constructor(
    private staffManagementService: StaffManagementService,
    private commonService: CommonService,
  ) {}

  resolve(): Observable<string | null> {
    return this.staffManagementService
      .getConfigurationByName('CURRENCY_FOR_PAYMENT')
      .pipe(
        map((res: any) => {
          this.commonService.spinnerHide();
          return res?.data?.value || null;
        }),
        catchError((error) => {
          return of(null);
        }),
      );
  }
}
