
# Prodotto
      La soluzione informatizzata, denominata “REEA – Registro Ex Esposti
      Amianto”, rappresenta un sistema integrato all’interno dell’ecosistema dei
      servizi della sanità digitale del Piemonte, interoperabile con la base
      dati anagrafica nazionale e rispondente alla normativa in corso relativa
      allo specifico tema, finalizzato alla gestione di un archivio regionale
      dei soggetti ex esposti ad amianto avviati all’attività di sorveglianza
      sanitaria da parte delle ASL piemontesi.

      Gli attori principali che concorrono alla gestione completa dell’attività
      di sorveglianza sono il CRPT dell’AOU CDSS di Torino, titolare del
      Registro, e gli SPreSAL, deputati alla presa in carico degli assistiti di
      competenza di ciascuna ASL.
	  
# Descrizione del prodotto 
Il prodotto è composto attualmente dalle seguenti componenti 
| Componente |Descrizione  |Versione |
|--|--|--|
| reeafe         | Componente di frontend Angular
| reeabe         | Applicazione principale Springboot che espone le API di backend al frontend Angular
| reeadb         | Scripts PostreSQL contenenti le DDL e ilk popolamento dei dati necessari all'avvio


# Prerequisiti di sistema 

## Software
- [Apache 2.4](https://www.apache.org/)
- [OpenJDK 17 distribution from Adoptium](https://adoptium.net/temurin/releases/?version=17) 


## Dipendenze da sistemi esterni

### Sistema di autenticazione
Il sistema di autenticazione su cui si basa il PGMEAS è esterno al presente prodotto ed è basato sul framework SHIBBOLETH composto da Service Provider e Identity Provider. 
Gli operatori che accedono ai servizi online della Sanità regionale piemontese si basano su 
- credenziali della PA Piemontese
- certificati digitali
- SPID O CIE
E' altresì possibile configarare lìapplicativo per login e password e profilare gli utenti sul database 8si rimanda a tale scopo a le indicaziojni contenute in  TO COMPLETA)




# Authors
La lista delle persone che hanno partecipato alla realizzazione del software sono:
- Davide Elia
- Laura Lo Russo
- Giuliano Iunco
- Gaetano Sorresso
- Christian Porri

# Copyrights
© Copyright Regione Piemonte – 2026


# License
EUPL-1.2 Compatibile
Vedere il file LICENSE.txt per i dettagli.
