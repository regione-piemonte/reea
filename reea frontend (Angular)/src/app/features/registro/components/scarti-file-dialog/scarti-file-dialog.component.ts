import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ScartoFileDTO } from '../../../../api';

export interface ScartiDialogData {
  nomeFile: string;
  scarti: ScartoFileDTO[];
}

@Component({
  selector: 'app-scarti-file-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './scarti-file-dialog.component.html',
  styleUrl: './scarti-file-dialog.component.scss'
})
export class ScartiFileDialogComponent {
  data = inject<ScartiDialogData>(MAT_DIALOG_DATA);
  private dialogRef = inject(MatDialogRef<ScartiFileDialogComponent>);

  onChiudi(): void {
    this.dialogRef.close();
  }
}
