# Build e parametrizzazione ambienti Angular con Maven



## Descrizione
Questo progetto utilizza Maven per orchestrare il build Angular e permette la parametrizzazione delle variabili ambiente tramite file template e resource filtering.

## Come funziona
- Le variabili ambiente sono definite in `src/environments/environment.template.ts` usando token Maven (`@VARIABILE@`).
- Durante il build, Maven sostituisce i token con i valori definiti nei profili (`prod`, `test`, ecc.) del `pom.xml`.
- Il file risultante viene usato come `environment.ts` per la build Angular.

## Esempio di variabile custom
Nel template:
```ts
export const environment = {
  ...
  customVar: "@CUSTOM_VAR@"
};
```
Nel `pom.xml` (profilo prod):
```xml
<profile>
  <id>prod</id>
  <properties>
    ...
    <CUSTOM_VAR>valoreCustom</CUSTOM_VAR>
  </properties>
</profile>
```

## Come eseguire il build parametrizzato

- **Produzione:**
  ```sh
  mvn clean install -Pprod
  ```
- **Test:**
  ```sh
  mvn clean install -Ptest
  ```

## Aggiungere nuove variabili
1. Inserisci il token nel template `environment.template.ts`.
2. Aggiungi la property corrispondente nel profilo desiderato del `pom.xml`.

## Note
- Puoi aggiungere tutti i profili che vuoi (es. dev, stage) seguendo lo stesso schema.
- Le variabili numeriche e booleane devono essere valorizzate senza virgolette nei profili Maven.
- Il file `environment.ts` viene sovrascritto ad ogni build.
