import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatoAssistito } from '../../../core/models';

/**
 * Componente Badge per visualizzare lo stato dell'assistito
 * con colori differenziati
 */
@Component({
  selector: 'app-status-badge',
  imports: [CommonModule],
  templateUrl: './status-badge.component.html',
  styleUrl: './status-badge.component.scss'
})
export class StatusBadgeComponent {
  // Input signal per lo stato
  stato = input.required<StatoAssistito>();

  /**
   * Determina la classe CSS in base allo stato
   * Basato sulla tabella reea_d_soggetto_stato
   */
  getStatusClass(): string {
    const stato = this.stato()?.toLowerCase().trim();

    switch (stato) {
      case StatoAssistito.DA_VALUTARE.toLowerCase():
        return 'badge-warning';
      case StatoAssistito.ELEGGIBILE.toLowerCase():
        return 'badge-success';
      case StatoAssistito.NON_ELEGGIBILE.toLowerCase():
        return 'badge-danger';
      case StatoAssistito.PRESO_IN_CARICO.toLowerCase():
        return 'badge-info';
      case StatoAssistito.AVVIATO_SORV.toLowerCase():
        return 'badge-primary';
      case StatoAssistito.ESCLUSO_SORV.toLowerCase():
        return 'badge-secondary';
      case StatoAssistito.CONCLU_SORV.toLowerCase():
        return 'badge-success';
      case StatoAssistito.ESCLUSO_DECESSO.toLowerCase():
        return 'badge-danger';
      case StatoAssistito.ESCLUSO_EMIGRAZIONE.toLowerCase():
        return 'badge-warning';
      case StatoAssistito.CARICATO.toLowerCase():
        return 'badge-secondary';
      case StatoAssistito.NON_PRESO_IN_CARICO.toLowerCase():
        return 'badge-danger';
      default:
        return 'badge-secondary';
    }
  }
}
