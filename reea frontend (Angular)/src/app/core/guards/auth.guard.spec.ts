import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { firstValueFrom, isObservable, of } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let auth: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', [
      'isAuthenticated', 'needsProfiloSelection', 'getTipoAuth'
    ]);
    auth.isAuthenticated.and.returnValue(false);
    auth.needsProfiloSelection.and.returnValue(false);
    auth.getTipoAuth.and.returnValue(of({ accesso_pua: 'ON' }));
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }]
    });
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });

  async function activate(url: string) {
    const result = TestBed.runInInjectionContext(() => authGuard(
      {} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot
    ));
    return isObservable(result) ? firstValueFrom(result) : result;
  }

  it('inoltra il token PUA fuori da returnUrl', async () => {
    const result = await activate('/registro-amianto?token=test-pua-token') as UrlTree;
    // Verifica i parametri effettivamente visibili al callback dopo la navigazione.
    const callback = router.parseUrl(router.serializeUrl(result));
    expect(callback.queryParamMap.get('token')).toBe('test-pua-token');
    expect(callback.queryParamMap.get('returnUrl')).toBe('/registro-amianto');
    expect(callback.root.children['primary'].segments.map(s => s.path))
      .toEqual(['registro-amianto', 'reea-pua']);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('inoltra cf e conserva filtri e fragment nella destinazione', async () => {
    const result = await activate(
      '/registro-amianto/assistiti?token=test-token&cf=CF_TEST&filtro=a&filtro=b#elenco'
    ) as UrlTree;
    expect(result.queryParamMap.get('cf')).toBe('CF_TEST');
    const destination = router.parseUrl(result.queryParamMap.get('returnUrl')!);
    expect(destination.queryParamMap.has('token')).toBeFalse();
    expect(destination.queryParamMap.has('cf')).toBeFalse();
    expect(destination.queryParamMap.getAll('filtro')).toEqual(['a', 'b']);
    expect(destination.fragment).toBe('elenco');
  });

  it('senza token lascia al callback PUA la gestione dell accesso mancante', async () => {
    const result = await activate('/registro-amianto') as UrlTree;
    expect(result.queryParamMap.has('token')).toBeFalse();
    expect(result.queryParamMap.get('returnUrl')).toBe('/registro-amianto');
  });

  it('mantiene il login interno quando PUA e disabilitato', async () => {
    auth.getTipoAuth.and.returnValue(of({ accesso_pua: 'OFF' }));
    expect(await activate('/registro-amianto/assistiti')).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/registro-amianto/assistiti' }
    });
  });

  it('mantiene l accesso dell utente gia autenticato', async () => {
    auth.isAuthenticated.and.returnValue(true);
    expect(await activate('/registro-amianto')).toBeTrue();
    expect(auth.getTipoAuth).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('mantiene la scelta profilo per l utente autenticato', async () => {
    auth.isAuthenticated.and.returnValue(true);
    auth.needsProfiloSelection.and.returnValue(true);
    expect(await activate('/registro-amianto')).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/registro-amianto/scelta-profilo']);
    expect(auth.getTipoAuth).not.toHaveBeenCalled();
  });
});
