import { Component, Input, OnInit } from '@angular/core';
import { UntypedFormControl, Validators } from '@angular/forms';

@Component({
  selector: 'custom-number-input',
  standalone: false,
  templateUrl: './custom-number-input.component.html',
  styleUrls: ['./custom-number-input.component.scss'],
})
export class CustomNumberInputComponent implements OnInit {
  @Input() label: string = '';
  @Input() formControlRef: UntypedFormControl = new UntypedFormControl('');
  @Input() errorMessage: string = '';
  @Input() placeholder: string = '';
  @Input() allowE: boolean = false;
  @Input() mode: 'decimal' | 'currency' | undefined = undefined;
  @Input() useGrouping: boolean = true;
  @Input() minFractionDigits: number = 0;
  @Input() maxFractionDigits: number = 0;
  @Input() min: number = Number.MIN_SAFE_INTEGER;
  @Input() max: number | null = null;
  @Input() locale: string | undefined = undefined;
  @Input() currency: string = '';
  @Input() prefix: string = '';
  @Input() suffix: string = '';
  @Input() showButtons: boolean = true;
  @Input() step: number = 1;

  isControlRequired = Validators.required;

  ngOnInit(): void {
    this.placeholder?.trim()
      ? null
      : (this.placeholder = `Enter ${this.label}`);
  }

  preventE(event: KeyboardEvent): void {
    if ((!this.allowE && (event.key === 'e' || event.key === 'E'))) {
      event.preventDefault();
    }
  }

  getErrorMessage(): string {
    if (this.formControlRef.hasError('required')) {
      return `${this.label} is required.`;
    }
    if (this.formControlRef.hasError('min')) {
      return `${this.label} must be at least ${this.min}.`;
    }
    if (this.formControlRef.hasError('max')) {
      return `${this.label} cannot exceed ${this.max}.`;
    }
    return this.errorMessage || 'Invalid value.';
  }
}
