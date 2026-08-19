import {
  ChangeDetectionStrategy,
  Component,
  Input,
  OnInit,
  Output,
  EventEmitter,
} from '@angular/core';
import { UntypedFormControl, Validators } from '@angular/forms';

@Component({
  selector: 'custom-input',
  templateUrl: './custom-input.component.html',
  styleUrls: ['./custom-input.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class CustomInputComponent implements OnInit {
  @Input() label: string = '';
  @Input() placeholder: string = '';
  @Input() formControlRef: UntypedFormControl = new UntypedFormControl('');
  @Input() errorMessage: string = '';
  @Input() type: 'text' | 'number' | 'email' | 'password' | 'checkbox' = 'text';
  @Input() maxLength: number | null = null;
  @Input() hideRequiredMarker: boolean = false;
  @Input() hideErrorMessage: boolean = false;

  @Input() suffixIcon: string | null = null;
  @Output() keydownTab = new EventEmitter<Event>();
  @Output() blur = new EventEmitter<FocusEvent>();

  @Input() canTogglePassword = false;
@Input() showPassword = false;
@Output() showPasswordChange = new EventEmitter<boolean>();


  ngOnInit(): void {
    this.placeholder = this.placeholder?.trim()
      ? this.placeholder
      : `Enter ${this.label}`;
  }

onKeydown(event: Event) {
  this.keydownTab.emit(event);
}


  onBlur(event: FocusEvent) {
    this.blur.emit(event);
  }

  get showError(): boolean {
    return (
      this.formControlRef.invalid &&
      (this.formControlRef.touched || this.formControlRef.dirty)
    );
  }

  get isRequired(): boolean {
    if (this.hideRequiredMarker) {
      return false;
    }
    const validator = this.formControlRef.validator
      ? this.formControlRef.validator({} as any)
      : null;
    return !!(validator && (validator as any)['required']);
  }

  isControlRequired = Validators.required;
}
