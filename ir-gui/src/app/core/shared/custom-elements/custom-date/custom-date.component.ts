import {
  ChangeDetectionStrategy,
  Component,
  Input,
  OnInit,
} from '@angular/core';
import { UntypedFormControl, Validators } from '@angular/forms';
import { DatePickerTypeView } from 'primeng/datepicker';

@Component({
  selector: 'custom-date',
  standalone: false,
  templateUrl: './custom-date.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomDateComponent implements OnInit {
  @Input() label: string = '';
  @Input() formControlRef: UntypedFormControl = new UntypedFormControl('');
  @Input() errorMessage: string = '';
  @Input() placeholder: string = '';
  @Input() showIcon: boolean = true;
  @Input() minDate: Date | null = null;
  @Input() maxDate: Date | null = null;
  @Input() disabledDays: number[] = [];
  @Input() readonlyInput: boolean = false;
  @Input() selectionMode: 'single' | 'multiple' | 'range' = 'single';
  @Input() showButtonBar: boolean = true;
  @Input() hourFormat: '12' | '24' = '12';
  @Input() showTime: boolean = false;
  @Input() timeOnly: boolean = false;
  @Input() view: DatePickerTypeView = 'date';
  @Input() isAutoCustomize: boolean = true;
  @Input() dateFormat: string = '';
  @Input() showLabel: boolean = true;
  @Input() showRequiredStar: boolean = true;

  dayFormat: 'd' | 'dd' | 'D' | 'DD' = 'dd';
  monthformat: 'm' | 'mm' | 'M' | 'MM' = 'mm';
  yearFormat: 'y' | 'yy' = 'yy';
  seprator: '-' | '/' | '|' | '.' | ',' | ' ' = '/';

  ngOnInit(): void {
    this.placeholder?.trim()
      ? null
      : (this.placeholder = `Select ${this.label}`);

    if (!this.dateFormat) {
      this.dateFormat = `${this.dayFormat}${this.seprator}${this.monthformat}${this.seprator}${this.yearFormat}`;
    }

    if (this.isAutoCustomize && typeof this.formControlRef.value == 'string') {
      this.formControlRef.setValue(new Date(this.formControlRef.value), {
        emitEvent: false,
      });
    }
  }

  onNowClick() {
    const now = new Date();
    this.formControlRef.patchValue(now);
  }

  isControlRequired = Validators.required;

  get showError(): boolean {
    return (
      this.formControlRef?.hasValidator(this.isControlRequired) &&
      this.formControlRef?.invalid &&
      this.formControlRef?.touched
    );
  }
}
