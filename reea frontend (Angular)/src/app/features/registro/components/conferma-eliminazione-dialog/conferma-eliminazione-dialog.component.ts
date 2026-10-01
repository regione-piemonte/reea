import { Component, inject } from '@angular/core';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-conferma-eliminazione-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <div class="conferma-dialog">
      <div class="conferma-body">
        <p>Confermi di cancellare la nota?</p>
      </div>
      <div class="conferma-footer">
        <button class="btn-si" (click)="onSi()">SI</button>
        <button class="btn-no" (click)="onNo()">NO</button>
      </div>
    </div>
  `,
  styles: [`
    .conferma-dialog {
      padding: 24px;
      min-width: 300px;
    }
    .conferma-body p {
      margin: 0 0 24px;
      font-size: 16px;
      color: #333;
    }
    .conferma-footer {
      display: flex;
      gap: 12px;
      justify-content: flex-end;
    }
    .btn-si {
      background: #0073E6;
      color: #fff;
      border: none;
      padding: 8px 24px;
      border-radius: 4px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
    }
    .btn-si:hover { background: #005BB5; }
    .btn-no {
      background: #f5f5f5;
      color: #333;
      border: 1px solid #ccc;
      padding: 8px 24px;
      border-radius: 4px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
    }
    .btn-no:hover { background: #e0e0e0; }
  `]
})
export class ConfermaEliminazioneDialogComponent {
  private dialogRef = inject(MatDialogRef<ConfermaEliminazioneDialogComponent>);

  onSi(): void { this.dialogRef.close(true); }
  onNo(): void { this.dialogRef.close(false); }
}
