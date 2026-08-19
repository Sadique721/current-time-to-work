import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { routes, CommonService } from 'src/app/core.index';
import { CallScript, CallScriptService } from '../call-script.service';

interface ScriptVariable {
  label: string;
  value: string;
}

@Component({
  selector: 'app-call-script-add-edit',
  templateUrl: './call-script-add-edit.component.html',
  styleUrl: './call-script-add-edit.component.scss',
  standalone: false,
})
export class CallScriptAddEditComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  form: FormGroup;
  
  submitted = false;
  isCollapsed = false;
  selectedCallScript: any = null;
  isLoading = false;

  public routes = routes;

  scriptVariables: ScriptVariable[] = [
    { label: '+ First Name', value: '{{first_name}}' },
    { label: '+ Last Name', value: '{{last_name}}' },
    { label: '+ Phone Code', value: '{{phone_code}}' },
    { label: '+ Phone Number', value: '{{phone_number}}' },
    { label: '+ Gender', value: '{{gender}}' },
    { label: '+ Country', value: '{{country}}' },
    { label: '+ Address', value: '{{address}}' },
    { label: '+ City', value: '{{city}}' },
    { label: '+ State', value: '{{state}}' },
    { label: '+ Postal Code', value: '{{postal_code}}' },
    { label: '+ DOB', value: '{{dob}}' },
    { label: '+ Alt. Phone', value: '{{alternate_phone}}' },
    { label: '+ Email', value: '{{email}}' },
    { label: '+ Description', value: '{{description}}' },
    { label: '+ Lead Group', value: '{{lead_group}}' },
    { label: '+ Lead Status', value: '{{lead_status}}' },
    { label: '+ Province', value: '{{province}}' },
    { label: '+ User', value: '{{user}}' },
  ];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private callScriptService: CallScriptService,
    private commonService: CommonService
  ) {
    this.form = this.fb.group({
      callScriptName: ['', [Validators.required]],
      description: ['', [Validators.required]],
      script: ['', [Validators.required]],
      status: [true, Validators.required],
    });
  }

  ngOnInit(): void {
    const id = history.state?.id;
    if (id) {
      this.selectedCallScript = { id: +id };
      this.loadCallScriptFromApi(+id);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadCallScriptFromApi(id: number): void {
    this.isLoading = true;
    this.commonService.spinnerShow();

    this.callScriptService.getById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (callScript: CallScript) => {
          this.selectedCallScript = callScript;
          this.patchFormForEdit(callScript);
          this.isLoading = false;
          this.commonService.spinnerHide();
        },
        error: (error) => {
          this.commonService.toastError('Failed to load call script');
          this.isLoading = false;
          this.commonService.spinnerHide();
          this.onCancel();
        }
      });
  }

  private patchFormForEdit(callScript: CallScript): void {
    this.form.patchValue({
      callScriptName: callScript.callScriptName,
      description: callScript.description,
      script: callScript.script,
      status: callScript.status,
    });
  }

  insertVariable(variable: ScriptVariable): void {
    const scriptControl = this.form.get('script');
    if (!scriptControl) return;

    const currentValue = scriptControl.value || '';
    const newValue = currentValue + variable.value;
    
    scriptControl.setValue(newValue);
    scriptControl.markAsDirty();
    
    this.commonService.toastSuccess(`Variable ${variable.label} inserted`);
  }

  onSubmit(): void {
    this.submitted = true;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.commonService.toastError('Please fill all required fields correctly');
      return;
    }

    const payload = this.form.value;

    this.isLoading = true;
    this.commonService.spinnerShow();

    if (this.selectedCallScript?.id) {
      this.callScriptService.update(this.selectedCallScript.id, payload)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.commonService.toastSuccess('Call script updated successfully');
            this.commonService.spinnerHide();
            this.isLoading = false;
            setTimeout(() => this.onCancel(), 500);
          },
          error: (error) => {
            this.commonService.toastError('Failed to update call script');
            this.commonService.spinnerHide();
            this.isLoading = false;
          }
        });
    } else {
      this.callScriptService.create(payload)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.commonService.toastSuccess('Call script created successfully');
            this.commonService.spinnerHide();
            this.isLoading = false;
            setTimeout(() => this.onCancel(), 500);
          },
          error: (error) => {
            this.commonService.toastError('Failed to create call script');
            this.commonService.spinnerHide();
            this.isLoading = false;
          }
        });
    }
  }

  clearForm(): void {
    if (this.isLoading) return;

    this.form.reset({ 
      callScriptName: '', 
      description: '',
      script: '',
      status: true 
    });
    
    this.submitted = false;
    this.form.markAsUntouched();
    this.form.markAsPristine();
    
    this.commonService.toastSuccess('Form cleared');
  }

  onCancel(): void {
    if (this.isLoading) return;
    this.router.navigate([this.routes.callscript], { replaceUrl: true });
  }

  toggleCollapse(): void {
    this.isCollapsed = !this.isCollapsed;
  }
}