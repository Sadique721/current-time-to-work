import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { StaffManagementService } from '../staff-management.service';

@Injectable({ providedIn: 'root' })
export class AgentListResolver implements Resolve<any[]> {
  constructor(private staffManagementService: StaffManagementService) {}

  resolve(): Observable<any[]> {
    return this.staffManagementService.getAllAgent().pipe(
      map((res: any) => res.agentlist || []),
      catchError((error) => {
        return of([]);
      }),
    );
  }
}
