import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TapConfigurationComponent } from './tap-configuration.component';

describe('TapConfigurationComponent', () => {
  let component: TapConfigurationComponent;
  let fixture: ComponentFixture<TapConfigurationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TapConfigurationComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TapConfigurationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
