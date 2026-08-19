import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SourceConfigurationDetailsComponent } from './source-configuration-details.component';

describe('SourceConfigurationDetailsComponent', () => {
  let component: SourceConfigurationDetailsComponent;
  let fixture: ComponentFixture<SourceConfigurationDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SourceConfigurationDetailsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SourceConfigurationDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
