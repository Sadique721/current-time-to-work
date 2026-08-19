import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SipDevicesAddEditComponent } from './sip-devices-add-edit.component';

describe('SipDevicesAddEditComponent', () => {
  let component: SipDevicesAddEditComponent;
  let fixture: ComponentFixture<SipDevicesAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [SipDevicesAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SipDevicesAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
