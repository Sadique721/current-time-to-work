import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EmailSmtpComponent } from './email-smtp.component';

describe('EmailSmtpComponent', () => {
  let component: EmailSmtpComponent;
  let fixture: ComponentFixture<EmailSmtpComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [EmailSmtpComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EmailSmtpComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
