import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { routes, CommonService } from 'src/app/core.index';
import { AudioPrompt, AudioPromptsService } from '../audio-prompts.service';

@Component({
  selector: 'app-audio-prompts-add-edit',
  templateUrl: './audio-prompts-add-edit.component.html',
  styleUrl: './audio-prompts-add-edit.component.scss',
  standalone: false,
})
export class AudioPromptsAddEditComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  form: FormGroup;
  fileDisplayControl: FormControl;
  
  submitted = false;
  isCollapsed = false;
  selectedPrompt: any = null;
  isLoading = false;

  selectedFile: File | null = null;
  selectedFileName: string = '';

  public routes = routes;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private audioPromptsService: AudioPromptsService,
    private commonService: CommonService
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required]],
    });

    this.fileDisplayControl = new FormControl({ value: '', disabled: true });
  }

  ngOnInit(): void {
    const id = history.state?.id;
    if (id) {
      this.selectedPrompt = { id: +id };
      this.loadPromptFromApi(+id);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadPromptFromApi(id: number): void {
    this.isLoading = true;
    this.commonService.spinnerShow();

    this.audioPromptsService.getById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.selectedPrompt = response.data;
          this.patchFormForEdit(response.data);
          this.isLoading = false;
          this.commonService.spinnerHide();
        },
        error: (error) => {
          this.commonService.toastError(error?.error?.message || 'Failed to load audio prompt');
          this.isLoading = false;
          this.commonService.spinnerHide();
          this.onCancel();
        }
      });
  }

  private patchFormForEdit(prompt: AudioPrompt): void {
    this.form.patchValue({
      name: prompt.name,
    });
    
    this.selectedFileName = prompt.originalName;
    this.fileDisplayControl.setValue(prompt.originalName);
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (!file) return;

    const allowedTypes = ['audio/mpeg', 'audio/mp3', 'audio/wav', 'audio/ogg'];
    if (!allowedTypes.includes(file.type)) {
      this.commonService.toastError('Invalid file type. Please select an audio file (MP3, WAV, OGG)');
      return;
    }

    const maxSize = 10 * 1024 * 1024;
    if (file.size > maxSize) {
      this.commonService.toastError('File size exceeds 10MB limit');
      return;
    }

    this.selectedFile = file;
    this.selectedFileName = file.name;
    
    this.fileDisplayControl.setValue(file.name);
  }

  removeFile(): void {
    this.selectedFile = null;
    this.selectedFileName = '';
    this.fileDisplayControl.setValue('');
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }

  onSubmit(): void {
    this.submitted = true;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.commonService.toastError('Please fill all required fields correctly');
      return;
    }

    if (!this.selectedPrompt?.id && !this.selectedFile) {
      this.commonService.toastError('Please select an audio file');
      return;
    }

    const formData = new FormData();
    formData.append('name', this.form.get('name')?.value);
    formData.append('status', '1');
    
    if (this.selectedFile) {
      formData.append('file', this.selectedFile);
    }

    this.isLoading = true;
    this.commonService.spinnerShow();

    if (this.selectedPrompt?.id) {
      this.audioPromptsService.update(this.selectedPrompt.id, formData)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.commonService.toastSuccess('Audio prompt updated successfully');
            this.commonService.spinnerHide();
            this.isLoading = false;
            setTimeout(() => this.onCancel(), 500);
          },
          error: (error) => {
            this.commonService.toastError(error?.error?.message || 'Failed to update audio prompt');
            this.commonService.spinnerHide();
            this.isLoading = false;
          }
        });
    } else {
      this.audioPromptsService.create(formData)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.commonService.toastSuccess('Audio prompt created successfully');
            this.commonService.spinnerHide();
            this.isLoading = false;
            setTimeout(() => this.onCancel(), 500);
          },
          error: (error) => {
            this.commonService.toastError(error?.error?.message || 'Failed to create audio prompt');
            this.commonService.spinnerHide();
            this.isLoading = false;
          }
        });
    }
  }

  clearForm(): void {
    if (this.isLoading) return;

    this.form.reset({ name: '' });
    this.selectedFile = null;
    this.selectedFileName = '';
    this.fileDisplayControl.setValue('');
    
    this.submitted = false;
    this.form.markAsUntouched();
    this.form.markAsPristine();
    
    this.commonService.toastSuccess('Form cleared');
  }

  onCancel(): void {
    if (this.isLoading) return;
    this.router.navigate([this.routes.audioprompts], { replaceUrl: true });
  }

  toggleCollapse(): void {
    this.isCollapsed = !this.isCollapsed;
  }
}