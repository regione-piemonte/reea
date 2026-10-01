import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { ParametriApiService } from '../../../../api/parametri-api.service';
import { AuthToken, User } from '../../../../core/models';
import { AuthService } from '../../../../core/services/auth.service';
import { ReeaPuaComponent } from './reea-pua.component';

describe('ReeaPuaComponent - PUA_URL da DB', () => {
  let auth: jasmine.SpyObj<AuthService>;
  let parametri: jasmine.SpyObj<ParametriApiService>;
  let router: jasmine.SpyObj<Router>;
  let redirect: jasmine.Spy;
  let route: { snapshot: { queryParamMap: ReturnType<typeof convertToParamMap> } };
  let component: ReeaPuaComponent;

  beforeEach(() => {
    auth = jasmine.createSpyObj('AuthService', [
      'getTipoAuth', 'checkAuth', 'loginWithToken', 'needsProfiloSelection'
    ]);
    auth.getTipoAuth.and.returnValue(of({ accesso_pua: 'ON' }));
    auth.checkAuth.and.returnValue(of(null));
    auth.needsProfiloSelection.and.returnValue(false);
    auth.loginWithToken.and.returnValue(of({ token: 'test-token' } as AuthToken));
    parametri = jasmine.createSpyObj('ParametriApiService', ['getValore']);
    parametri.getValore.and.returnValue(of('https://portale.example/pua/#/'));
    router = jasmine.createSpyObj('Router', ['navigate', 'navigateByUrl']);
    redirect = jasmine.createSpy('location.replace');
    route = { snapshot: { queryParamMap: convertToParamMap({}) } };
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: ParametriApiService, useValue: parametri },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: route },
        { provide: DOCUMENT, useValue: {
          baseURI: 'https://reea.example/registro-amianto/',
          location: { replace: redirect }
        } }
      ]
    });
    component = TestBed.runInInjectionContext(() => new ReeaPuaComponent());
  });

  it('legge PUA_URL e apre il portale preservando il fragment', () => {
    route.snapshot.queryParamMap = convertToParamMap({
      cf: 'CF_TEST', returnUrl: '/registro-amianto/assistiti'
    });
    component.ngOnInit();
    expect(parametri.getValore).toHaveBeenCalledOnceWith('PUA_URL');
    expect(redirect).toHaveBeenCalledOnceWith('https://portale.example/pua/#/');
    expect(auth.loginWithToken).not.toHaveBeenCalled();
  });

  it('con token verifica il login senza leggere PUA_URL o riaprire il portale', () => {
    route.snapshot.queryParamMap = convertToParamMap({ token: 'test-token', cf: 'CF_TEST' });
    auth.needsProfiloSelection.and.returnValue(true);
    component.ngOnInit();
    expect(auth.loginWithToken).toHaveBeenCalledOnceWith('test-token', 'CF_TEST');
    expect(router.navigate).toHaveBeenCalledWith(['/registro-amianto/scelta-profilo']);
    expect(auth.checkAuth).not.toHaveBeenCalled();
    expect(parametri.getValore).not.toHaveBeenCalled();
    expect(redirect).not.toHaveBeenCalled();
  });

  it('un token rifiutato mostra un errore senza creare un ciclo col portale', () => {
    route.snapshot.queryParamMap = convertToParamMap({ token: 'expired-token' });
    auth.loginWithToken.and.returnValue(throwError(() => new Error('Token scaduto')));
    component.ngOnInit();
    expect(component.errore()).toContain('verifica del token');
    expect(parametri.getValore).not.toHaveBeenCalled();
    expect(redirect).not.toHaveBeenCalled();
  });

  it('attende il ripristino della sessione senza mandare al PUA un utente autenticato', () => {
    const session = new Subject<User | null>();
    auth.checkAuth.and.returnValue(session);
    route.snapshot.queryParamMap = convertToParamMap({ returnUrl: '/registro-amianto/assistiti' });
    component.ngOnInit();
    expect(parametri.getValore).not.toHaveBeenCalled();
    expect(redirect).not.toHaveBeenCalled();
    session.next({ codiceFiscale: 'CF_TEST' } as User);
    session.complete();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/registro-amianto/assistiti');
    expect(parametri.getValore).not.toHaveBeenCalled();
    expect(redirect).not.toHaveBeenCalled();
  });

  it('con sessione scaduta recupera il portale per un nuovo accesso', () => {
    auth.checkAuth.and.returnValue(throwError(() => new Error('401')));
    component.ngOnInit();
    expect(redirect).toHaveBeenCalledOnceWith('https://portale.example/pua/#/');
  });

  it('con PUA disabilitato non legge la destinazione e non apre il portale', () => {
    auth.getTipoAuth.and.returnValue(of({ accesso_pua: 'OFF' }));
    component.ngOnInit();
    expect(component.errore()).toContain('Accesso non autorizzato');
    expect(auth.checkAuth).not.toHaveBeenCalled();
    expect(parametri.getValore).not.toHaveBeenCalled();
    expect(redirect).not.toHaveBeenCalled();
  });

  for (const value of ['', 'URL_NON_VALIDO', 'javascript:alert(1)', '/pua/',
    'https://utente:password@portale.example/pua/', 'https://reea.example/registro-amianto/']) {
    it('non naviga con una configurazione invalida: ' + value, () => {
      parametri.getValore.and.returnValue(of(value));
      component.ngOnInit();
      expect(component.errore()).toContain('non configurato correttamente');
      expect(redirect).not.toHaveBeenCalled();
    });
  }

  it('un errore nella lettura del parametro non produce un redirect', () => {
    parametri.getValore.and.returnValue(throwError(() => new Error('DB non disponibile')));
    component.ngOnInit();
    expect(component.errore()).toContain('Impossibile recuperare');
    expect(redirect).not.toHaveBeenCalled();
  });

  it('legge nuovamente il parametro per un accesso successivo', () => {
    parametri.getValore.and.returnValues(
      of('https://portale.example/pua/#/'), of('https://altro-portale.example/pua/#/')
    );
    component.ngOnInit();
    const next = TestBed.runInInjectionContext(() => new ReeaPuaComponent());
    next.ngOnInit();
    expect(parametri.getValore).toHaveBeenCalledTimes(2);
    expect(redirect.calls.allArgs()).toEqual([
      ['https://portale.example/pua/#/'], ['https://altro-portale.example/pua/#/']
    ]);
  });
});
