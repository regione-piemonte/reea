import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

export interface EsitoVisitaDialogData {
  titolo: string;
  testo: string;
  data?: string;
}

@Component({
  selector: 'app-esito-visita-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <div class="esito-dialog">
      <div class="esito-dialog-header">
        <span class="esito-dialog-title">{{ data.titolo }}{{ data.data ? ' — ' + data.data : '' }}</span>
        <button class="btn-close" (click)="chiudi()">✕</button>
      </div>
      <div class="esito-dialog-body">
        <p>{{ data.testo }}</p>
      </div>
      <div class="esito-dialog-footer">
        <button class="btn-ok" (click)="chiudi()">Chiudi</button>
      </div>
    </div>
  `,
  styles: [`
    .esito-dialog {
      min-width: 480px;
      max-width: 680px;
    }
    .esito-dialog-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px 20px 12px;
      border-bottom: 1px solid #e0e0e0;
    }
    .esito-dialog-title {
      font-size: 16px;
      font-weight: 700;
      color: #0073E6;
    }
    .btn-close {
      background: none;
      border: none;
      font-size: 18px;
      color: #666;
      cursor: pointer;
      line-height: 1;
      padding: 0 4px;
      &:hover { color: #333; }
    }
    .esito-dialog-body {
      padding: 20px;
      max-height: 400px;
      overflow-y: auto;
      p {
        margin: 0;
        font-size: 14px;
        color: #333;
        line-height: 1.6;
        white-space: pre-wrap;
      }
    }
    .esito-dialog-footer {
      display: flex;
      justify-content: flex-end;
      padding: 12px 20px 16px;
      border-top: 1px solid #e0e0e0;
    }
    .btn-ok {
      background: #0073E6;
      color: #fff;
      border: none;
      padding: 8px 24px;
      border-radius: 4px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      &:hover { background: #005BB5; }
    }
  `]
})
export class EsitoVisitaDialogComponent {
  data = inject<EsitoVisitaDialogData>(MAT_DIALOG_DATA);
  private dialogRef = inject(MatDialogRef<EsitoVisitaDialogComponent>);

  chiudi(): void { this.dialogRef.close(); }
}
