import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormBuilder, FormGroup } from '@angular/forms';
import { Subject, takeUntil, debounceTime } from 'rxjs';
import { Router } from '@angular/router';
import { routes, SidebarService, CommonService, PaginationService } from 'src/app/core.index';
import { AudioPromptsService, AudioPrompt } from './audio-prompts.service';
import { ConfirmDialogComponent } from 'src/app/core/shared/confirm-dialog/confirm-dialog.component';
import { MatDialog } from '@angular/material/dialog';

interface IPermission {
  create: boolean;
  edit: boolean;
  delete: boolean;
}

interface AudioPromptRow extends AudioPrompt {
  selected?: boolean;
  currentTime?: number;
  isMuted?: boolean;
  isPaused?: boolean;
  duration?: number;
}

@Component({
  selector: 'app-audio-prompts',
  templateUrl: './audio-prompts.component.html',
  styleUrl: './audio-prompts.component.scss',
  standalone: false,
})
export class AudioPromptsComponent implements OnInit, OnDestroy {
  public routes = routes;

  tableData: AudioPromptRow[] = [];
  pageSize = 10;
  totalData = 0;
  paginationSkip = 0;
  currentPage = 1;
  serialNumberArray: Array<number> = [];

  searchDataValue = new FormControl('');
  filtersForm: FormGroup;
  isCollapsed = false;
  isLoading = false;
  isSearchMode = false;
  isFiltersOpen = false;

  
  currentPlayingId: number | null = null;
  audioElement: HTMLAudioElement | null = null;

  private audioCache: Map<number, string> = new Map();
  private muteStates: Map<number, boolean> = new Map();
  private audioUpdateInterval: any = null;

  sortColumn: string = '';
  sortDirection: 'asc' | 'desc' = 'asc';

  private isUpdatingPagination = false;
  private isReloadingAfterDelete = false;

  private destroy$ = new Subject<void>();

  permission: IPermission = {
    create: true,
    edit: true,
    delete: true,
  };

  nameConditionOptions = [
    { label: 'Begin with', value: 'beginWith' },
    { label: 'Contains', value: 'contains' },
    { label: 'Ends with', value: 'endsWith' },
    { label: 'Equals', value: 'equals' },
  ];

  constructor(
    private router: Router,
    private fb: FormBuilder,
    private audioPromptsService: AudioPromptsService,
    private dialog: MatDialog,
    private sidebar: SidebarService,
    private commonService: CommonService,
    private paginationService: PaginationService
  ) {
    this.filtersForm = this.fb.group({
      name: [''],
      nameCondition: ['beginWith'],
    });
  }

  ngOnInit(): void {
    this.paginationService.tablePageSize
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(50)
      )
      .subscribe((res: any) => {
        if (this.isUpdatingPagination || this.isLoading) {
          return;
        }

        const newSkip = res.skip;
        const newPageSize = res.pageSize;
        const newPage = Math.floor(newSkip / newPageSize) + 1;

        if (newSkip === this.paginationSkip && newPageSize === this.pageSize) {
          return;
        }

        this.paginationSkip = newSkip;
        this.pageSize = newPageSize;
        this.currentPage = newPage;

        if (this.isSearchMode) {
          const searchTerm = (this.searchDataValue.value || '').toString().trim();
          this.performSearch(searchTerm);
        } else {
          this.loadAudioPrompts();
        }
      });

    this.loadAudioPrompts();
  }

  ngOnDestroy(): void {
    this.stopAudio();
    this.clearAudioUpdate();

    this.audioCache.forEach(url => {
      window.URL.revokeObjectURL(url);
    });
    this.audioCache.clear();
    this.muteStates.clear();

    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  private loadAudioPrompts(): void {
    this.isLoading = true;
    this.stopAudio();

  if (!this.isReloadingAfterDelete) {
    this.commonService.spinnerShow();
  }

    const page = Math.floor(this.paginationSkip / this.pageSize);

    this.audioPromptsService.getAll(page, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.tableData = response.data.map(item => ({
            ...item,
            selected: false,
            currentTime: 0,
            isMuted: false,
            isPaused: false,
            duration: 0
          }));

          this.totalData = response.pagination?.totalItems || response.data.length;

          this.serialNumberArray = Array.from(
            { length: this.tableData.length },
            (_, i) => this.paginationSkip + i + 1
          );

          this.isUpdatingPagination = true;

          this.paginationService.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: this.pageSize,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });

          setTimeout(() => {
            this.isUpdatingPagination = false;
          }, 100);

          this.preloadAudio(this.tableData);

          this.isLoading = false;

          if (!this.isReloadingAfterDelete) {
            this.commonService.spinnerHide();
          } else {
            this.isReloadingAfterDelete = false;
          }
        },
        error: (error) => {
          this.handleError(error);
        }
      });
  }

  searchData(): void {
    const searchTerm = (this.searchDataValue.value || '').toString().trim();

    if (!searchTerm) {
      this.isSearchMode = false;
      this.currentPage = 1;
      this.paginationSkip = 0;
      this.loadAudioPrompts();
      return;
    }

    this.isSearchMode = true;
    this.currentPage = 1;
    this.paginationSkip = 0;
    this.performSearch(searchTerm);
  }

  private performSearch(searchTerm: string): void {
    this.isLoading = true;
    this.stopAudio();

  if (!this.isReloadingAfterDelete) {
    this.commonService.spinnerShow();
  }

    const page = Math.floor(this.paginationSkip / this.pageSize);

    this.audioPromptsService.search(searchTerm, page, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.tableData = response.data.map(item => ({
            ...item,
            selected: false,
            currentTime: 0,
            isMuted: false,
            isPaused: false,
            duration: 0
          }));

          this.totalData = response.pagination?.totalItems || response.data.length;

          this.serialNumberArray = Array.from(
            { length: this.tableData.length },
            (_, i) => this.paginationSkip + i + 1
          );

          this.isUpdatingPagination = true;

          this.paginationService.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: this.pageSize,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });

          setTimeout(() => {
            this.isUpdatingPagination = false;
          }, 100);

          this.preloadAudio(this.tableData);

          this.isLoading = false;

          if (!this.isReloadingAfterDelete) {
            this.commonService.spinnerHide();
          } else {
            this.isReloadingAfterDelete = false;
          }
        },
        error: (error) => {
          this.handleError(error);
        }
      });
  }

  private handleError(error: any): void {
    this.commonService.spinnerHide();
    this.tableData = [];
    this.totalData = 0;
    this.serialNumberArray = [];

    this.paginationService.calculatePageSize.next({
      totalData: 0,
      pageSize: this.pageSize,
      tableData: [],
      serialNumberArray: [],
    });

    this.isLoading = false;
    this.isReloadingAfterDelete = false;

    if (error.status === 404) {
      this.commonService.toastInfo(error?.error?.message || 'No audio prompts found');
    } else {
      this.commonService.toastError(error?.error?.message || 'Failed to load audio prompts');
    }
  }

private preloadAudio(prompts: AudioPromptRow[]): void {
  prompts.forEach((prompt) => {
    if (!prompt.id) return;

    if (this.audioCache.has(prompt.id)) {
      
      if (!prompt.duration) {
        const cachedUrl = this.audioCache.get(prompt.id)!;
        const tempAudio = new Audio(cachedUrl);
        tempAudio.addEventListener('loadedmetadata', () => {
          prompt.duration = Math.floor(tempAudio.duration);
          tempAudio.src = '';
        });
        tempAudio.addEventListener('error', () => {});
      }
      return;
    }

    
    this.audioPromptsService.streamAudio(prompt.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob: Blob) => {
          const audioUrl = window.URL.createObjectURL(blob);
          this.audioCache.set(prompt.id, audioUrl);

          const tempAudio = new Audio(audioUrl);
          tempAudio.addEventListener('loadedmetadata', () => {
            prompt.duration = Math.floor(tempAudio.duration);
            tempAudio.src = '';
          });
          tempAudio.addEventListener('error', () => {});
        },
        error: () => {}
      });
  });
}

  

  playPrompt(prompt: AudioPromptRow): void {
    
    if (this.currentPlayingId === prompt.id && this.audioElement) {
      if (this.audioElement.paused) {
        this.audioElement.play()
          .then(() => {
            this.startAudioUpdate(prompt);
            prompt.isPaused = false;
          })
          .catch(() => {
            this.commonService.toastError('Unable to play audio');
          });
      }
      return;
    }

    
    this.stopAudio();

    const cachedUrl = this.audioCache.get(prompt.id);

    if (cachedUrl) {
      this.playAudioFromUrl(cachedUrl, prompt);
    } else {
      this.commonService.spinnerShow();
      this.currentPlayingId = prompt.id;

      this.audioPromptsService.streamAudio(prompt.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (blob: Blob) => {
            const audioUrl = window.URL.createObjectURL(blob);
            this.audioCache.set(prompt.id, audioUrl);
            this.commonService.spinnerHide();
            this.playAudioFromUrl(audioUrl, prompt);
          },
          error: () => {
            this.commonService.spinnerHide();
            this.commonService.toastError('Failed to load audio');
            this.currentPlayingId = null;
          },
        });
    }
  }

  private playAudioFromUrl(audioUrl: string, prompt: AudioPromptRow): void {
    this.currentPlayingId = prompt.id;
    this.audioElement = new Audio();
    this.audioElement.src = audioUrl;

    prompt.currentTime = 0;
    prompt.isPaused = false;

    const wasMutedBefore = this.muteStates.get(prompt.id) || false;
    prompt.isMuted = wasMutedBefore;
    this.audioElement.muted = wasMutedBefore;

    this.audioElement.preload = 'auto';
    this.audioElement.load();

    this.audioElement.play()
      .then(() => {
        this.startAudioUpdate(prompt);
      })
      .catch(() => {
        if (this.currentPlayingId === prompt.id) {
          this.commonService.toastError('Unable to play audio');
          this.currentPlayingId = null;
        }
      });

    this.audioElement.onended = () => {
      this.currentPlayingId = null;
      prompt.currentTime = 0;
      prompt.isPaused = false;
      this.clearAudioUpdate();
    };

    this.audioElement.onerror = () => {
      if (this.currentPlayingId === prompt.id) {
        this.commonService.toastError('Audio file not accessible');
        this.currentPlayingId = null;
        this.clearAudioUpdate();
      }
    };
  }

  pausePrompt(prompt: AudioPromptRow): void {
    if (
      this.currentPlayingId === prompt.id &&
      this.audioElement &&
      !this.audioElement.paused
    ) {
      this.audioElement.pause();
      this.clearAudioUpdate();
      prompt.isPaused = true;
    }
  }

  stopPrompt(prompt: AudioPromptRow): void {
    if (this.currentPlayingId === prompt.id) {
      this.stopAudio();
      prompt.currentTime = 0;
      prompt.isPaused = false;
    }
  }

  private stopAudio(): void {
    this.clearAudioUpdate();

    if (this.audioElement) {
      this.audioElement.pause();
      this.audioElement.src = '';
      this.audioElement = null;
    }

    if (this.currentPlayingId !== null) {
      const prompt = this.tableData.find(p => p.id === this.currentPlayingId);
      if (prompt) {
        prompt.currentTime = 0;
        prompt.isPaused = false;
      }
    }

    this.currentPlayingId = null;
  }

  

  isPlaying(audioId: number): boolean {
    return !!(
      this.currentPlayingId === audioId &&
      this.audioElement &&
      !this.audioElement.paused
    );
  }

  isPaused(audioId: number): boolean {
    return !!(
      this.currentPlayingId === audioId &&
      this.audioElement &&
      this.audioElement.paused
    );
  }

  getCurrentTime(audioId: number): string {
    const prompt = this.tableData.find(p => p.id === audioId);
    if (!prompt || !prompt.currentTime) return '00:00';

    const minutes = Math.floor(prompt.currentTime / 60);
    const seconds = prompt.currentTime % 60;
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }

  getDuration(prompt: AudioPromptRow): string {
    if (!prompt.duration) return '00:00';

    const minutes = Math.floor(prompt.duration / 60);
    const seconds = prompt.duration % 60;
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }

  getProgress(audioId: number): number {
    const prompt = this.tableData.find(p => p.id === audioId);
    if (!prompt || !prompt.duration || prompt.duration === 0) return 0;
    return ((prompt.currentTime || 0) / prompt.duration) * 100;
  }

  

  private startAudioUpdate(prompt: AudioPromptRow): void {
    this.clearAudioUpdate();

    this.audioUpdateInterval = setInterval(() => {
      if (this.audioElement && !this.audioElement.paused) {
        prompt.currentTime = Math.floor(this.audioElement.currentTime);
      }
    }, 100);
  }

  private clearAudioUpdate(): void {
    if (this.audioUpdateInterval) {
      clearInterval(this.audioUpdateInterval);
      this.audioUpdateInterval = null;
    }
  }

  

  seekAudio(audioId: number, event: any): void {
    const prompt = this.tableData.find(p => p.id === audioId);

    if (this.audioElement && prompt && this.currentPlayingId === audioId) {
      const seekTime = Number(event.target.value);
      this.audioElement.currentTime = seekTime;
      prompt.currentTime = seekTime;
    }
  }

  toggleMute(audioId: number): void {
    const prompt = this.tableData.find(p => p.id === audioId);
    if (!prompt) return;

    const newMuteState = !prompt.isMuted;
    prompt.isMuted = newMuteState;
    this.muteStates.set(audioId, newMuteState);

    if (this.audioElement && this.currentPlayingId === audioId) {
      this.audioElement.muted = newMuteState;
    }
  }

  isMuted(audioId: number): boolean {
    const prompt = this.tableData.find(p => p.id === audioId);
    return prompt?.isMuted || this.muteStates.get(audioId) || false;
  }

  

  downloadPrompt(prompt: AudioPromptRow): void {
    const fileName = `${prompt.name}.wav`;

    this.commonService.spinnerShow();

    this.audioPromptsService.downloadAudio(prompt.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob: Blob) => {
          this.commonService.spinnerHide();

          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = fileName;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          window.URL.revokeObjectURL(url);

          this.commonService.toastSuccess('Download started');
        },
        error: () => {
          this.commonService.spinnerHide();
          this.commonService.toastError('Failed to download audio');
        },
      });
  }

  

  sortBy(column: string): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }

    this.tableData.sort((a, b) => {
      const aVal = a[column as keyof AudioPromptRow];
      const bVal = b[column as keyof AudioPromptRow];

      if (aVal == null && bVal == null) return 0;
      if (aVal == null) return 1;
      if (bVal == null) return -1;

      if (aVal < bVal) return this.sortDirection === 'asc' ? -1 : 1;
      if (aVal > bVal) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }

  toggleAllSelection(event: any): void {
    const checked = event.target.checked;
    this.tableData.forEach(item => item.selected = checked);
  }

  isAllSelected(): boolean {
    return this.tableData.length > 0 && this.tableData.every(item => item.selected);
  }

  onRowSelectionChange(): void {}

  

  openAudioPromptAddEdit(item: AudioPromptRow | null = null): void {
    const state: any = {};
    if (item?.id) {
      state.id = item.id;
    }
    this.router.navigate([this.routes.audioprompts + '/add-edit'], { state });
  }

  

  confirmDelete(id: number): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      disableClose: true,
      data: {
        title: 'Delete Audio Prompt',
        message: 'Are you sure you want to delete this audio prompt?',
        confirmButtonText: 'Yes Delete',
        cancelButtonText: 'Cancel',
        iconClass: 'ti ti-trash fs-24 text-danger',
      },
    });

    dialogRef.afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed: boolean) => {
        if (confirmed) {
          this.deleteAudioPrompt(id);
        }
      });
  }

  private deleteAudioPrompt(id: number): void {
    this.commonService.spinnerShow();

    this.audioPromptsService.delete(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.commonService.toastSuccess('Audio prompt deleted successfully');

          this.isReloadingAfterDelete = true;

          const willBeEmpty = this.tableData.length === 1;
          const notFirstPage = this.currentPage > 1;

          if (willBeEmpty && notFirstPage) {
            this.currentPage--;
            this.paginationSkip = (this.currentPage - 1) * this.pageSize;

            this.isUpdatingPagination = true;

            this.paginationService.tablePageSize.next({
              skip: this.paginationSkip,
              limit: this.paginationSkip + this.pageSize,
              pageSize: this.pageSize
            });

            setTimeout(() => {
              this.isUpdatingPagination = false;
            }, 100);
          } else {
            if (this.isSearchMode) {
              const searchTerm = (this.searchDataValue.value || '').toString().trim();
              this.performSearch(searchTerm);
            } else {
              this.loadAudioPrompts();
            }
          }

          this.commonService.spinnerHide();
        },
        error: (error) => {
          this.commonService.spinnerHide();
          this.commonService.toastError(error?.error?.message || 'Failed to delete audio prompt');
        }
      });
  }

  private restoreDurationsFromCache(prompts: AudioPromptRow[]): void {
  prompts.forEach((prompt) => {
    if (prompt.id && this.audioCache.has(prompt.id) && !prompt.duration) {
      const cachedUrl = this.audioCache.get(prompt.id)!;
      const tempAudio = new Audio(cachedUrl);
      tempAudio.addEventListener('loadedmetadata', () => {
        prompt.duration = Math.floor(tempAudio.duration);
        tempAudio.src = '';
      });
      tempAudio.addEventListener('error', () => {});
    }
  });
}

  clearSearch(): void {
    this.searchDataValue.setValue('');
    this.isSearchMode = false;
    this.currentPage = 1;
    this.paginationSkip = 0;
    this.loadAudioPrompts();
  }
}