import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SipDevicesOtherFeaturesComponent } from './sip-devices-other-features.component';

describe('SipDevicesOtherFeaturesComponent', () => {
  let component: SipDevicesOtherFeaturesComponent;
  let fixture: ComponentFixture<SipDevicesOtherFeaturesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [SipDevicesOtherFeaturesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SipDevicesOtherFeaturesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
