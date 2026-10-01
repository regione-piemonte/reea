import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  title = 'reeafe';
  private auth = inject(AuthService);

  ngOnInit(): void {
    // Se siamo sulla route reea-pua, il token viene gestito da ReeaPuaComponent
    if (window.location.pathname.includes('reea-pua')) {
      return;
    }

    this.auth.checkAuth().subscribe();
  }
}
