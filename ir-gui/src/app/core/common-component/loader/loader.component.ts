import { Component } from '@angular/core';
import { CommonService } from 'src/app/core.index';

@Component({
  selector: 'app-loader',
  templateUrl: './loader.component.html',
  styleUrls: ['./loader.component.scss'],
  standalone: false,
})
export class LoaderComponent {
  constructor(public commonService: CommonService) {}

  loading$ = this.commonService.loading$;
}
