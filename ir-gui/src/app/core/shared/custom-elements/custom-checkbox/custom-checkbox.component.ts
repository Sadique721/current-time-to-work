import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import { UntypedFormControl, Validators } from '@angular/forms';

@Component({
  selector: 'custom-checkbox',
  templateUrl: './custom-checkbox.component.html',
  styleUrls: ['./custom-checkbox.component.scss'],
  standalone: false,
})
export class CustomCheckboxComponent implements OnInit, OnDestroy {
  @Input() label: string = '';
  @Input() formControlRef: UntypedFormControl = new UntypedFormControl('');
  @Output() onChange = new EventEmitter();

  ngOnInit(): void {}

  ngOnDestroy(): void {}

  isControlRequired = Validators.required;

  change(event: any): void {
    this.onChange.emit(event);
  }
}
