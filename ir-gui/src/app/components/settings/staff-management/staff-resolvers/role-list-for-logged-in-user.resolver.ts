import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { StaffManagementService } from '../staff-management.service';

@Injectable({ providedIn: 'root' })
export class RoleListForLoggedInUser implements Resolve<any[]> {
  constructor(private staffManagementService: StaffManagementService) {}

  resolve(): Observable<any[]> {
    return this.staffManagementService.getAllRoleDataForLoggedInUser().pipe(
      map((res: any) => res.dataList || []),
      catchError((error) => {
        return of([]);
      }),
    );
  }
}
