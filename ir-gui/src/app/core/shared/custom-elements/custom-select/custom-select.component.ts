import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  TemplateRef,
  ContentChild
} from '@angular/core';
import { UntypedFormControl, Validators } from '@angular/forms';
import { DropdownFilterEvent } from 'primeng/dropdown';
import { MultiSelectChangeEvent } from 'primeng/multiselect';

@Component({
  selector: 'custom-select',
  templateUrl: './custom-select.component.html',
  styleUrls: ['./custom-select.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class CustomSelectComponent implements OnInit {
  @ContentChild('item', { static: false }) itemTemplateRef!: TemplateRef<any>;
  @ContentChild('selectedItem', { static: false }) selectedItemTemplateRef!: TemplateRef<any>;

  @Input() label: string = '';
  @Input() options: any[] = [];
  @Input() optionValue: string = 'value';
  @Input() optionLabel: string = 'label';
  @Input() formControlRef: UntypedFormControl = new UntypedFormControl('');
  @Input() isAddAllow: boolean = false;
  @Input() dataBsTarget: string = '';
  @Input() errorMessage: string = '';
  @Input() filter: boolean = false;
  @Input() showClear: boolean = false;
  @Input() placeholder: string = '';
  @Input() filterBy: string = '';
  @Input() virtualScroll: boolean = false;
  @Input() virtualScrollItemSize: number = 30;
  @Output() onSearch = new EventEmitter<DropdownFilterEvent>();
  @Output() onOptionChange = new EventEmitter<MultiSelectChangeEvent>();
  @Input() isMultiSelect: boolean = false;

  ngOnInit(): void {
    if (!this.placeholder?.trim()) {
      this.placeholder = `Select ${this.label}`;
    }
    if (this.virtualScrollItemSize < 10) {
      this.virtualScrollItemSize = 30;
    }
    if (!this.filterBy) {
      this.filterBy = this.optionLabel;
    }

    if (this.isMultiSelect && !Array.isArray(this.formControlRef.value)) {
          }
  }

  isRequired(): boolean {
    return this.formControlRef?.hasValidator?.(Validators.required);
  }

  isControlRequired = Validators.required;

  getSelectedLabels(items: any[]): string {
    return (items || [])
      .map(item => item?.[this.optionLabel])
      .join(', ');
  }

  get shouldShowClear(): boolean {
    if (!this.showClear) return false;
    
    const value = this.formControlRef?.value;
    
    if (this.isMultiSelect) {
      return Array.isArray(value) && value.length > 0;
    }
    return value !== null && value !== undefined && value !== '';
  }
}