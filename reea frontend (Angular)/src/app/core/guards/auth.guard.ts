import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { map, of, switchMap } from 'rxjs';

export const authGuard: CanActivateFn = (
  _route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    if (auth.needsProfiloSelection()) {
      router.navigate(['/registro-amianto/scelta-profilo']);
      return false;
    }
    return true;
  }

  // Se arriviamo da un ingresso protetto da Shibboleth/SPID (es. reea-spid.ruparpiemonte.it),
  // il backend riconosce l'header Shib-Identita-CodiceFiscale e logga direttamente, senza
  // passare da PUA. Se l'header non c'è (accesso "normale"), risponde 401 → null, e si
  // procede con il flusso esistente (PUA o login interno).
  return auth.loginShibboleth().pipe(
    switchMap(authToken => {
      if (authToken) {
        if (auth.needsProfiloSelection()) {
          router.navigate(['/registro-amianto/scelta-profilo']);
          return of(false);
        }
        return of(true);
      }

      return auth.getTipoAuth().pipe(
        map(({ accesso_pua }) => {
          if (accesso_pua === 'ON') {
            const destinazione = router.parseUrl(state.url);
            const token = destinazione.queryParamMap.get('token');
            const cf = destinazione.queryParamMap.get('cf');
            // Il callback PUA legge token e cf direttamente dalla query string.
            // Non riportare le credenziali nell'URL della pagina dopo il login.
            delete destinazione.queryParams['token'];
            delete destinazione.queryParams['cf'];

            return router.createUrlTree(['/registro-amianto/reea-pua'], {
              queryParams: {
                returnUrl: router.serializeUrl(destinazione),
                ...(token ? { token } : {}),
                ...(cf ? { cf } : {})
              }
            });
          } else {
            // Accesso tramite login interno
            router.navigate(['/login'], {
              queryParams: { returnUrl: state.url }
            });
          }
          return false;
        })
      );
    })
  );
};
