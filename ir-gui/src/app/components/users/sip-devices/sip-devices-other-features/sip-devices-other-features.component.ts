import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { routes, CommonService } from 'src/app/core.index';
import { SipDevice, SipDevicesService } from '../sip-devices.service';

@Component({
  selector: 'app-sip-devices-other-features',
  templateUrl: './sip-devices-other-features.component.html',
  styleUrl: './sip-devices-other-features.component.scss',
  standalone: false,
})
export class SipDevicesOtherFeaturesComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  
  callForwardingForm: FormGroup;
  voiceMailForm: FormGroup;
  followMeForm: FormGroup;
  dndForm: FormGroup;
  callRecordingForm: FormGroup;
  speedDialForm: FormGroup;

  isCollapsed = false;
  selectedDevice: SipDevice | null = null;
  isLoading = false;
  isSaving = false;
  activeTab = 'callForwarding';
  voiceMailPasswordVisible = false;

  public routes = routes;

  
  expandedSections = {
    alwaysForward: true,
    onBusy: false,
    noAnswer: false,
    notRegistered: false,
  };

  
  routingOptions = [
    { label: 'SIP Device', value: 'SIP Device' },
    { label: 'IVR', value: 'IVR' },
    { label: 'Time-Condition', value: 'Time-Condition' },
    { label: 'Conference', value: 'Conference' },
    { label: 'Call Queue', value: 'Call Queue' },
    { label: 'Ring Group', value: 'Ring Group' },
    { label: 'Voicemail', value: 'Voicemail' },
    { label: 'PSTN', value: 'PSTN' },
    { label: 'External User', value: 'External User' },
    { label: 'Gen Lead', value: 'Gen Lead' },
    { label: 'Send SMS', value: 'Send SMS' },
    { label: 'Custom IVR', value: 'Custom IVR' },
  ];

  
  sipDeviceOptions: Array<{ label: string; value: string }> = [];
  ivrOptions: Array<{ label: string; value: string }> = [];
  timeConditionOptions: Array<{ label: string; value: string }> = [];
  conferenceOptions: Array<{ label: string; value: string }> = [];
  callQueueOptions: Array<{ label: string; value: string }> = [];
  ringGroupOptions: Array<{ label: string; value: string }> = [];
  voicemailOptions: Array<{ label: string; value: string }> = [];
  pstnOptions: Array<{ label: string; value: string }> = [];
  externalUserOptions: Array<{ label: string; value: string }> = [];
  genLeadOptions: Array<{ label: string; value: string }> = [];
  sendSmsOptions: Array<{ label: string; value: string }> = [];
  customIvrOptions: Array<{ label: string; value: string }> = [];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private sipDevicesService: SipDevicesService,
    private commonService: CommonService
  ) {
    
    this.callForwardingForm = this.fb.group({
      alwaysForward: [false],
      alwaysForwardRouting: [''],
      alwaysForwardDestination: [''],
      onBusy: [false],
      onBusyRouting: [''],
      onBusyDestination: [''],
      noAnswer: [false],
      noAnswerRouting: [''],
      noAnswerDestination: [''],
      notRegistered: [false],
      notRegisteredRouting: [''],
      notRegisteredDestination: [''],
    });

    
    this.voiceMailForm = this.fb.group({
      voiceMailEnabled: [false],
      voiceMailPassword: [''],
      voiceMailEmail: [''],
      voiceMailAttachFile: [false],
    });

    
    this.followMeForm = this.fb.group({
      followMe: [false],
      followMeIgnoreBusy: [false],
      destination1: [''],
      destination2: [''],
      destination3: [''],
      destination4: [''],
      destination5: [''],
    });

    
    this.dndForm = this.fb.group({
      doNotDisturb: [false],
    });

    
    this.callRecordingForm = this.fb.group({
      callRecording: [false],
    });

    
    this.speedDialForm = this.fb.group({
      speedDialEntries: this.fb.array([])
    });

    
    for (let i = 0; i < 10; i++) {
      this.speedDialEntries.push(
        this.fb.group({
          digit: [i],
          routing: ['SIP Device'],
          destination: ['']
        })
      );
    }

    
    this.setupRoutingChangeListeners();
  }

  get speedDialEntries(): FormArray {
    return this.speedDialForm.get('speedDialEntries') as FormArray;
  }

    getSpeedDialControl(index: number, controlName: string): any {
    return this.speedDialEntries.at(index).get(controlName);
  }

    private setupRoutingChangeListeners(): void {
    
    this.callForwardingForm.get('alwaysForwardRouting')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.callForwardingForm.patchValue({ alwaysForwardDestination: '' });
      });

    
    this.callForwardingForm.get('onBusyRouting')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.callForwardingForm.patchValue({ onBusyDestination: '' });
      });

    
    this.callForwardingForm.get('noAnswerRouting')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.callForwardingForm.patchValue({ noAnswerDestination: '' });
      });

    
    this.callForwardingForm.get('notRegisteredRouting')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.callForwardingForm.patchValue({ notRegisteredDestination: '' });
      });
  }

    getDestinationOptions(routingType: string): Array<{ label: string; value: string }> {
    if (!routingType) {
      return [];
    }
    
    switch (routingType) {
      case 'SIP Device':
        return this.sipDeviceOptions;
      case 'IVR':
        return this.ivrOptions;
      case 'Time-Condition':
        return this.timeConditionOptions;
      case 'Conference':
        return this.conferenceOptions;
      case 'Call Queue':
        return this.callQueueOptions;
      case 'Ring Group':
        return this.ringGroupOptions;
      case 'Voicemail':
        return this.voicemailOptions;
      case 'PSTN':
        return this.pstnOptions;
      case 'External User':
        return this.externalUserOptions;
      case 'Gen Lead':
        return this.genLeadOptions;
      case 'Send SMS':
        return this.sendSmsOptions;
      case 'Custom IVR':
        return this.customIvrOptions;
      default:
        return [];
    }
  }

    getDestinationLabel(routingType: string): string {
    if (!routingType) {
      return 'Destination:';
    }
    
    switch (routingType) {
      case 'SIP Device':
        return 'SIP Device:';
      case 'IVR':
        return 'IVR:';
      case 'Time-Condition':
        return 'Time-Condition:';
      case 'Conference':
        return 'Conference:';
      case 'Call Queue':
        return 'Call Queue:';
      case 'Ring Group':
        return 'Ring Group:';
      case 'Voicemail':
        return 'Voicemail:';
      case 'PSTN':
        return 'PSTN:';
      case 'External User':
        return 'External User:';
      case 'Gen Lead':
        return 'Gen Lead:';
      case 'Send SMS':
        return 'Send SMS:';
      case 'Custom IVR':
        return 'Custom IVR:';
      default:
        return 'Destination:';
    }
  }

  ngOnInit(): void {
    
    this.loadAllRoutingOptions();

    
    const id = history.state?.id;
    if (id) {
      this.loadDeviceFromApi(+id);
    } else {
      
      this.commonService.toastError('Device ID not found');
      this.onCancel();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadAllRoutingOptions(): void {
    
    this.sipDevicesService.getSipDevicesForRouting()
      .pipe(takeUntil(this.destroy$))
      .subscribe((devices) => {
        this.sipDeviceOptions = devices;
      });

    
    
    this.ivrOptions = [
      { label: 'IVR 1', value: 'ivr_1' },
      { label: 'IVR 2', value: 'ivr_2' },
    ];

    this.timeConditionOptions = [
      { label: 'Business Hours', value: 'business_hours' },
      { label: 'After Hours', value: 'after_hours' },
    ];

    this.conferenceOptions = [
      { label: 'Conference Room 1', value: 'conf_1' },
      { label: 'Conference Room 2', value: 'conf_2' },
    ];

    this.callQueueOptions = [
      { label: 'Sales Queue', value: 'queue_sales' },
      { label: 'Support Queue', value: 'queue_support' },
    ];

    this.ringGroupOptions = [
      { label: 'Ring Group 1', value: 'ring_1' },
      { label: 'Ring Group 2', value: 'ring_2' },
    ];

    this.voicemailOptions = [
      { label: 'Voicemail Box 1', value: 'vm_1' },
      { label: 'Voicemail Box 2', value: 'vm_2' },
    ];

    this.pstnOptions = [
      { label: 'PSTN Line 1', value: 'pstn_1' },
      { label: 'PSTN Line 2', value: 'pstn_2' },
    ];

    this.externalUserOptions = [
      { label: 'External User 1', value: 'ext_user_1' },
      { label: 'External User 2', value: 'ext_user_2' },
    ];

    this.genLeadOptions = [
      { label: 'Lead Gen 1', value: 'lead_1' },
      { label: 'Lead Gen 2', value: 'lead_2' },
    ];

    this.sendSmsOptions = [
      { label: 'SMS Template 1', value: 'sms_1' },
      { label: 'SMS Template 2', value: 'sms_2' },
    ];

    this.customIvrOptions = [
      { label: 'Custom IVR 1', value: 'custom_ivr_1' },
      { label: 'Custom IVR 2', value: 'custom_ivr_2' },
    ];
  }

    private loadDeviceFromApi(id: number): void {
    this.isLoading = true;
    this.commonService.spinnerShow();

    this.sipDevicesService.getById(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (device) => {
          this.selectedDevice = device;
          
          this.isLoading = false;
          this.commonService.spinnerHide();
        },
        error: (error) => {
                    this.isLoading = false;
          this.commonService.spinnerHide();
          this.commonService.toastError('Failed to load device data');
          this.onCancel();
        }
      });
  }

    setActiveTab(tab: string): void {
    this.activeTab = tab;
  }

    toggleSection(section: keyof typeof this.expandedSections): void {
    this.expandedSections[section] = !this.expandedSections[section];
  }

    onSave(): void {
    if (!this.selectedDevice?.id) {
      this.commonService.toastError('Device ID not found');
      return;
    }

    
    const payload = {
      ...this.callForwardingForm.value,
      ...this.voiceMailForm.value,
      ...this.followMeForm.value,
      ...this.dndForm.value,
      ...this.callRecordingForm.value,
      speedDial: this.speedDialEntries.value.filter((entry: any) => entry.destination),
    };

    this.isSaving = true;
    this.commonService.spinnerShow();

    this.sipDevicesService.update(this.selectedDevice.id, payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
                    this.commonService.toastSuccess('Device features updated successfully');
          this.commonService.spinnerHide();
          this.isSaving = false;
          
          setTimeout(() => this.onCancel(), 500);
        },
        error: (error) => {
                    this.commonService.spinnerHide();
          this.isSaving = false;
          this.commonService.toastError('Failed to update device features');
        }
      });
  }

  onCancel(): void {
      this.router.navigate([this.routes.sipdevices], { replaceUrl: true });
  }

  toggleCollapse(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  toggleVoiceMailPasswordVisibility(): void {
    this.voiceMailPasswordVisible = !this.voiceMailPasswordVisible;
  }
}