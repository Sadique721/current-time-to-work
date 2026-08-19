import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

@Component({
  selector: 'app-snapshot-details',
  imports: [],
  templateUrl: './snapshot-details.component.html',
  styleUrl: './snapshot-details.component.scss',
})
export class SnapshotDetailsComponent implements OnInit {
  snapshotDetails: string = '';
  constructor(
    public dialogRef: MatDialogRef<SnapshotDetailsComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {}

  ngOnInit(): void {
    this.snapshotDetails = this.data?.snapshot;
  }

  closeDialog(): void {
    this.dialogRef.close();
  }
}
