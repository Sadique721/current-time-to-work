import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SystemConfigAddEditComponent } from './system-config-add-edit.component';

describe('SystemConfigAddEditComponent', () => {
  let component: SystemConfigAddEditComponent;
  let fixture: ComponentFixture<SystemConfigAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SystemConfigAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SystemConfigAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
