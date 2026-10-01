import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'registro-amianto',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/pages/login/login.component').then(
        m => m.LoginComponent
      )
  },
  {
    path: 'auth/callback',
    loadComponent: () =>
      import('./features/auth/pages/callback/auth-callback.component').then(
        m => m.AuthCallbackComponent
      )
  },
  {
    path: 'registro-amianto/reea-pua',
    loadComponent: () =>
      import('./features/auth/pages/reea-pua/reea-pua.component').then(
        m => m.ReeaPuaComponent
      )
  },
  {
    path: 'registro-amianto/scelta-profilo',
    loadComponent: () =>
      import('./features/auth/pages/scelta-profilo/scelta-profilo.component').then(
        m => m.SceltaProfiloComponent
      )
  },
  {
    path: 'registro-amianto',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/registro/pages/selezione-assistiti/selezione-assistiti.component').then(
        m => m.SelezioneAssistitiComponent
      )
  },
  {
    path: 'registro-amianto/assistiti',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/registro/pages/elenco-assistiti/elenco-assistiti.component').then(
        m => m.ElencoAssistitiComponent
      )
  },
  {
    path: 'registro-amianto/nuovo-assistito',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/registro/pages/nuovo-assistito/nuovo-assistito.component').then(
        m => m.NuovoAssistitoComponent
      )
  },
  {
    path: 'registro-amianto/assistiti/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/registro/pages/dettaglio-assistito/dettaglio-assistito.component').then(
        m => m.DettaglioAssistitoComponent
      )
  },
  { 
    path: 'registro-amianto/assistiti/:id/anagrafica',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/registro/pages/anagrafica-completa/anagrafica-completa.component').then(
        m => m.AnagraficaCompletaComponent
      )
  },
  {
    path: '**',
    redirectTo: 'registro-amianto'
  }
];
