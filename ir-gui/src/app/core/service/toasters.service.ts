import { Injectable } from '@angular/core';
import { ConfirmationService, MessageService } from 'primeng/api';

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  private life = 3000;

  constructor(
    private _msg: MessageService,
    private _conf: ConfirmationService,
  ) {}

  toastError(msg: string, title: string = '', life: number = 0): void {
    this._msg.add({
      severity: 'error',
      summary: title || 'Error',
      detail: msg ?? 'No message',
      life: life || this.life,
      styleClass: 'danger-light-popover',
    });
  }

  toastSuccess(msg: string, title: string = '', life: number = 0): void {
    this._msg.add({
      severity: 'success',
      summary: title || 'Success',
      detail: msg ?? 'No message',
      life: life || this.life,
      styleClass: 'success-light-popover',
    });
  }

  toastInfo(msg: string, title: string = '', life: number = 0): void {
    this._msg.add({
      severity: 'info',
      summary: title || 'Info',
      detail: msg ?? 'No message',
      life: life || this.life,
      styleClass: 'info-light-popover',
    });
  }

  toastWarn(msg: string, title: string = '', life: number = 0): void {
    this._msg.add({
      severity: 'warn',
      summary: title || 'Warning',
      detail: msg ?? 'No message',
      life: life || this.life,
      styleClass: 'primary-light-popover',
    });
  }
}
