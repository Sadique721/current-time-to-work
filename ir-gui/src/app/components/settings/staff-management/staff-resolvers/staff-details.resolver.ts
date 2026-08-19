import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot, Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { StaffManagementService } from '../staff-management.service';
import { CommonService } from 'src/app/core.index';

@Injectable({ providedIn: 'root' })
export class StaffDetailsResolver implements Resolve<any> {
  constructor(
    private staffManagementService: StaffManagementService,
    private commonService: CommonService,
  ) {}

  resolve(route: ActivatedRouteSnapshot): Observable<any> {
    const id = route.paramMap.get('id');
    return this.staffManagementService.getStaff(id as string).pipe(
      map((response: any) => {
        this.commonService.spinnerHide();
        return response.Staff;
      }),
      catchError((error) => {
        this.commonService.spinnerHide();
        this.commonService.toastError(
          error?.error?.ERROR || 'Error fetching staff detail',
        );
        return throwError(() => error);
      }),
    );
  }
}
