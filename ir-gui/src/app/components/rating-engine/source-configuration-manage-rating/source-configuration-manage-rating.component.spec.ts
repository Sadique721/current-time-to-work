import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SourceConfigurationManageRatingComponent } from './source-configuration-manage-rating.component';

describe('SourceConfigurationManageRatingComponent', () => {
  let component: SourceConfigurationManageRatingComponent;
  let fixture: ComponentFixture<SourceConfigurationManageRatingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SourceConfigurationManageRatingComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SourceConfigurationManageRatingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
