/**
 * DateMaskDirective
 * -----------------
 * Si applica automaticamente a TUTTI gli input con [matDatepicker].
 * Permette di digitare la data come cifre pure (es. 09032026 → 09/03/2026)
 * inserendo gli slash "/" in automatico, oltre a supportare il calendario.
 */
import {
  AfterViewInit,
  Directive,
  ElementRef,
  HostListener,
  OnDestroy,
  Optional,
  Self
} from '@angular/core';
import { NgControl } from '@angular/forms';
import { Subscription } from 'rxjs';

@Directive({
  selector: 'input[matDatepicker]',
  standalone: true
})
export class DateMaskDirective implements AfterViewInit, OnDestroy {

  /** Solo le cifre già inserite (max 8) */
  private rawValue = '';
  /** Flag per evitare loop quando settiamo il valore dal picker */
  private updating = false;
  private sub?: Subscription;

  constructor(
    private el: ElementRef<HTMLInputElement>,
    @Optional() @Self() private ngControl: NgControl
  ) {}

  ngAfterViewInit(): void {
    // Quando il calendario seleziona una data, aggiorna il display nel formato dd/MM/yyyy
    const ctrl = this.ngControl?.control;
    if (!ctrl) return;

    this.sub = ctrl.valueChanges.subscribe(value => {
      if (this.updating) return;

      if (value instanceof Date && !isNaN(value.getTime())) {
        const d = String(value.getDate()).padStart(2, '0');
        const m = String(value.getMonth() + 1).padStart(2, '0');
        const y = String(value.getFullYear());
        this.rawValue = d + m + y;
        // Piccolo delay: Angular Material aggiorna il display subito dopo;
        // con setTimeout(0) sovrascriviamo nel formato corretto.
        setTimeout(() => {
          this.el.nativeElement.value = `${d}/${m}/${y}`;
        }, 0);
      } else if (value === null || value === undefined || value === '') {
        this.rawValue = '';
        // Non azzeriamo il display mentre l'utente sta digitando
      }
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  // ─── Keyboard input ────────────────────────────────────────────────────────

  @HostListener('keydown', ['$event'])
  onKeyDown(event: KeyboardEvent): void {
    // Consentiamo Ctrl/Meta (Ctrl+A, Ctrl+C, Ctrl+V, Ctrl+Z…)
    if (event.ctrlKey || event.metaKey) return;

    const allowed = [
      'Tab', 'Enter', 'Escape',
      'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown',
      'Home', 'End', 'F1', 'F2', 'F3', 'F4', 'F5',
      'F6', 'F7', 'F8', 'F9', 'F10', 'F11', 'F12'
    ];
    const isDigit = /^[0-9]$/.test(event.key);

    // Blocca qualsiasi tasto non gestito (inclusi lettere, /, etc.)
    if (!isDigit && !allowed.includes(event.key) &&
        event.key !== 'Backspace' && event.key !== 'Delete') {
      event.preventDefault();
      return;
    }

    if (event.key === 'Backspace') {
      event.preventDefault();
      if (this.rawValue.length > 0) {
        this.rawValue = this.rawValue.slice(0, -1);
        this.refreshDisplay();
        if (this.rawValue.length === 0) {
          this.setControlValue(null);
        }
      }
      return;
    }

    if (event.key === 'Delete') {
      event.preventDefault();
      this.rawValue = '';
      this.refreshDisplay();
      this.setControlValue(null);
      return;
    }

    if (isDigit) {
      event.preventDefault();
      if (this.rawValue.length < 8) {
        this.rawValue += event.key;
        this.refreshDisplay();
      }
      // se già 8 cifre, ignoriamo ulteriori input
    }
  }

  // ─── Paste ─────────────────────────────────────────────────────────────────

  @HostListener('paste', ['$event'])
  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const text = event.clipboardData?.getData('text') ?? '';
    const digits = text.replace(/\D/g, '').slice(0, 8);
    this.rawValue = digits;
    this.refreshDisplay();
  }

  // ─── Helpers ───────────────────────────────────────────────────────────────

  /** Aggiorna il valore visualizzato nell'input (dd/MM/yyyy progressivo) */
  private refreshDisplay(): void {
    const v = this.rawValue;
    let display: string;

    if (v.length <= 2) {
      display = v;
    } else if (v.length <= 4) {
      display = `${v.slice(0, 2)}/${v.slice(2)}`;
    } else {
      display = `${v.slice(0, 2)}/${v.slice(2, 4)}/${v.slice(4)}`;
    }

    this.el.nativeElement.value = display;

    if (v.length === 8) {
      this.tryParseAndSet(v);
    }
  }

  /** Parsa 8 cifre come ddMMyyyy e imposta il Date sul form control */
  private tryParseAndSet(digits: string): void {
    const day   = parseInt(digits.slice(0, 2), 10);
    const month = parseInt(digits.slice(2, 4), 10) - 1; // 0-indexed
    const year  = parseInt(digits.slice(4, 8), 10);
    const date  = new Date(year, month, day);

    const valid =
      !isNaN(date.getTime()) &&
      date.getDate()     === day   &&
      date.getMonth()    === month &&
      date.getFullYear() === year;

    this.setControlValue(valid ? date : null);
  }

  /** Aggiorna il FormControl senza innescare il nostro valueChanges */
  private setControlValue(value: Date | null): void {
    const ctrl = this.ngControl?.control;
    if (!ctrl) return;
    this.updating = true;
    ctrl.setValue(value);
    ctrl.markAsDirty();
    this.updating = false;
  }
}
