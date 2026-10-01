import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { API_ENDPOINTS } from './api.config';
import { ParametriApiService } from './parametri-api.service';

describe('ParametriApiService - getValore', () => {
  it('legge PUA_URL come testo dal servizio esistente e non mantiene una cache', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(ParametriApiService);
    const http = TestBed.inject(HttpTestingController);
    const values: string[] = [];
    for (const url of ['https://portale.example/pua/#/', 'https://altro.example/pua/#/']) {
      service.getValore('PUA_URL').subscribe(value => values.push(value));
      const req = http.expectOne(API_ENDPOINTS.parametri.getValore('PUA_URL'));
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('text');
      expect(req.request.withCredentials).toBeTrue();
      req.flush(url);
    }
    expect(values).toEqual(['https://portale.example/pua/#/', 'https://altro.example/pua/#/']);
    http.verify();
  });
});
