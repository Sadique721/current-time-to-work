import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SourceConfigurationAddEditComponent } from './source-configuration-add-edit.component';

describe('SourceConfigurationAddEditComponent', () => {
  let component: SourceConfigurationAddEditComponent;
  let fixture: ComponentFixture<SourceConfigurationAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SourceConfigurationAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SourceConfigurationAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
