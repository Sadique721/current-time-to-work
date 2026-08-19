import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SipDevicesComponent } from './sip-devices.component';

describe('SipDevicesComponent', () => {
  let component: SipDevicesComponent;
  let fixture: ComponentFixture<SipDevicesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [SipDevicesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SipDevicesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
