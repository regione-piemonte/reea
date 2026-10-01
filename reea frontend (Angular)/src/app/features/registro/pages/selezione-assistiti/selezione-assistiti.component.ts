import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AppHeaderComponent } from '@shared/components/app-header/app-header.component';
import { ParametroDTO } from '@core/models';
import { SearchService, AuthService } from '@core/services';

@Component({
  selector: 'app-selezione-assistiti',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule, AppHeaderComponent],
  templateUrl: './selezione-assistiti.component.html',
  styleUrl: './selezione-assistiti.component.scss'
})
export class SelezioneAssistitiComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly searchService = inject(SearchService);
  private readonly authService = inject(AuthService);

  isPseudo          = this.authService.isPseudo;
  isInail           = this.authService.isInail;
  isCrpt            = this.authService.isCrpt;
  isSpresalNonPseudo = this.authService.isSpresalNonPseudo;

  // Signal per i pulsanti caricati dal backend
  pulsanti = signal<ParametroDTO[]>([]);
  loading = signal<boolean>(true);

  // Computed signals per dividere i pulsanti nelle colonne
  // Colonna sinistra (BLU): PULSANTE1-4 (Fonti)
  categorieSinistra = computed(() =>
    this.pulsanti().filter(p =>
      ['PULSANTE1', 'PULSANTE2', 'PULSANTE3', 'PULSANTE4'].includes(p.parametro_cod)
    )
  );

  // Colonna destra (ARANCIONE): PULSANTE5-7 (Sezioni)
  categorieDestra = computed(() =>
    this.pulsanti().filter(p =>
      ['PULSANTE5', 'PULSANTE6', 'PULSANTE7'].includes(p.parametro_cod)
    )
  );

  // Basso (GRIGIO): PULSANTE8-9 (Preadesioni da valutare/valutate)
  categorieBasso = computed(() =>
    this.pulsanti().filter(p =>
      ['PULSANTE8', 'PULSANTE9'].includes(p.parametro_cod)
    )
  );

  ngOnInit(): void {
    this.loadPulsanti();
  }

  /**
   * Carica i pulsanti dal backend
   */
  private loadPulsanti(): void {
    this.loading.set(true);

    this.searchService.getPulsantiHomepage().subscribe({
      next: (pulsanti) => {
        this.pulsanti.set(pulsanti);
        this.loading.set(false);
      },
      error: () => {
        this.pulsanti.set([]);
        this.loading.set(false);
      }
    });
  }

  /**
   * Determina se un pulsante è grigio chiaro (PULSANTE8)
   */
  isGrigioChiaro(pulsante: ParametroDTO): boolean {
    return pulsante.parametro_cod === 'PULSANTE8';
  }

  /**
   * Determina se un pulsante è grigio scuro (PULSANTE9)
   */
  isGrigioScuro(pulsante: ParametroDTO): boolean {
    return pulsante.parametro_cod === 'PULSANTE9';
  }

  /**
   * Mapping da codice pulsante a fonte_id (colonna BLU)
   * Secondo documento CDU-003 (tabella reea_d_fonte):
   * - PULSANTE1 = PREADESIONI → fonte_id=2
   * - PULSANTE2 = SEGNALAZIONI MMG → fonte_id=1
   * - PULSANTE3 = ELENCHI INAIL A e B → fonte_id=3
   * - PULSANTE4 = ELENCHI NPLA → fonte_id=4
   */
  private readonly pulsanteFonteMap: Record<string, string> = {
    'PULSANTE1': '2',  // Preadesione
    'PULSANTE2': '1',  // Segnalazione MMG
    'PULSANTE3': '3',  // INAIL
    'PULSANTE4': '4',  // NPLA
    'PULSANTE8': '2',  // Preadesioni da valutare → fonte Preadesioni
    'PULSANTE9': '2',  // Preadesioni valutate → fonte Preadesioni
  };

  /**
   * Mapping da codice pulsante a Sezione (colonna ARANCIONE)
   * Secondo documento CDU-003 (campo sezione in reea_t_registro):
   * - PULSANTE5 = SEZIONE PRIMA → sezione=1
   * - PULSANTE6 = SEZIONE SECONDA → sezione=2
   * - PULSANTE7 = SEZIONE TERZA → sezione=3
   */
  private readonly pulsanteSezioneMap: Record<string, string> = {
    'PULSANTE5': '1',
    'PULSANTE6': '2',
    'PULSANTE7': '3',
  };

  /**
   * Mapping da codice pulsante a soggetto_stato_id (pulsanti GRIGIO in basso)
   * Secondo documento CDU-003 (tabella reea_d_soggetto_stato):
   * - PULSANTE8 = PREADESIONI DA VALUTARE → soggetto_stato_id=1
   * - PULSANTE9 = PREADESIONI VALUTATE → soggetto_stato_id=2,3 (ELEGGIBILE + NON ELEGGIBILE)
   */
  private readonly pulsanteStatoMap: Record<string, string[]> = {
    'PULSANTE8': ['1'],      // DA VALUTARE
    'PULSANTE9': ['2', '3'], // ELEGGIBILE, NON ELEGGIBILE
  };

  /**
   * Naviga all'elenco assistiti passando i filtri preimpostati
   * Secondo il documento CDU-003, ogni pulsante apre la stessa pagina
   * ma con filtri diversi preimpostati
   */
  selezionaPulsante(pulsante: ParametroDTO): void {
    const queryParams: Record<string, string> = {};

    // Pulsanti BLU (Fonti): PULSANTE1-4
    const fonte = this.pulsanteFonteMap[pulsante.parametro_cod];
    if (fonte) {
      queryParams['fonte'] = fonte;
    }

    // Pulsanti ARANCIONE (Sezioni): PULSANTE5-7
    const sezione = this.pulsanteSezioneMap[pulsante.parametro_cod];
    if (sezione) {
      queryParams['sezione'] = sezione;
    }

    // Pulsanti GRIGIO (Stati): PULSANTE8-9
    const stati = this.pulsanteStatoMap[pulsante.parametro_cod];
    if (stati) {
      // Passa gli stati come stringa separata da virgola
      queryParams['stato'] = stati.join(',');
    }

    // Passa sempre il codice pulsante per riferimento
    queryParams['pulsante'] = pulsante.parametro_cod;

    this.router.navigate(['/registro-amianto/assistiti'], { queryParams });
  }
}
