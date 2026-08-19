import {
  ChangeDetectionStrategy,
  Component,
  Input,
  OnInit,
} from '@angular/core';
import { UntypedFormControl, Validators } from '@angular/forms';

@Component({
  selector: 'custom-textarea',
  templateUrl: './custom-textarea.component.html',
  styleUrls: ['./custom-textarea.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class CustomTextareaComponent implements OnInit {
  @Input() label: string = '';
  @Input() placeholder: string = '';
  @Input() formControlRef: UntypedFormControl = new UntypedFormControl('');
  @Input() errorMessage: string = '';
  @Input() rows: number = 3;
  @Input() cols: number = 30;
  @Input() autoResize: boolean = false;

  ngOnInit(): void {
    if (!this.placeholder?.trim()) {
      this.placeholder = `Enter ${this.label}`;
    }
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
