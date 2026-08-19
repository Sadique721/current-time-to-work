import { Injectable } from '@angular/core';
import { Resolve } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { SystemConfigService } from '../../system-config/system-config.service';

@Injectable({ providedIn: 'root' })
export class SystemConfigResolver implements Resolve<any[]> {
  constructor(private systemConfigService: SystemConfigService) {}

  resolve(): Observable<any[]> {
    const url = '/system/configuration/';
    return this.systemConfigService.getMethod(url).pipe(
      map((response: any) => response.clientlist || []),
      catchError((error) => {
        return of([]);
      }),
    );
  }
}
