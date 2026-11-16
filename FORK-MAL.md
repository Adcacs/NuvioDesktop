# Nuvio Desktop — tri de la bibliothèque par note MyAnimeList

Modification indépendante basée sur `NuvioMedia/NuvioDesktop`, branche `Dev`,
commit `c8ee7621542c2d55e3650e3bb263ed69027cbaa4`. Licence GPL-3.0 conservée.

## Application Windows disponible — 16 novembre 2026

La [compilation Windows complète](https://github.com/Adcacs/NuvioDesktop/actions/runs/43845951303)
a réussi sur le commit `62a48aed8cd266ab9e17915437cb7cc21f4b74d8`.
Les **27 tests desktop passent**, sans échec ni test ignoré.

- [Version portable](https://github.com/Adcacs/NuvioDesktop/actions/runs/43845951303/artifacts/14215213910) : extraire tout le ZIP, puis ouvrir `Nuvio.exe`.
- [Installateur MSI](https://github.com/Adcacs/NuvioDesktop/actions/runs/43845951303/artifacts/14215836667).
- SHA-256 du ZIP portable : `5331906b98cb8ded1c6cf8726ccf563ecb9d2dbaaee728dfaccab63c4c256a0b`.

Le ZIP portable téléchargé a été vérifié et contient l'exécutable et son runtime
Java. Un lancement local de 12 secondes atteint le code Compose, mais l'environnement
de vérification interdit l'accès au registre Windows utilisé par les préférences
Java/Supabase. L'interface, la connexion et la lecture vidéo ne sont donc pas validées
par cet essai. Aucun compte personnel n'a été utilisé pour ce test.

Les résultats du 15 novembre plus bas décrivent les essais locaux antérieurs.

## Utilisation

1. Dans Paramètres → Intégrations, activer les notes MDBList et renseigner une
   clé API personnelle, ou connecter un compte MDBList si le fork a été compilé
   avec sa configuration OAuth.
2. Ouvrir Bibliothèque, puis le menu de tri.
3. Choisir **MyAnimeList : meilleures notes** ou **MyAnimeList : notes croissantes**.

Le tri concerne la note communautaire MAL, pas une note personnelle ni IMDb.
Il fonctionne dans les rangées, la grille et « Tout voir », pour les bibliothèques
locale, Trakt, Simkl et MDBList. Les notes manquantes ou invalides sont placées à
la fin dans les deux sens ; les égalités sont départagées par titre puis identifiant.
Le choix est sauvegardé dans les réglages du profil.

Les appels ne démarrent que si un tri MAL est sélectionné. Le chargement utilise
des vagues bornées de 40 titres, la déduplication, les requêtes groupées et le cache
en mémoire du service MDBList existant. Un indicateur affiche la progression et
le nombre de titres pour lesquels une note a été reçue. Le badge MAL n'a pas besoin
d'être activé sur les fiches pour que ce tri fonctionne.

## Limites explicites

- Les titres doivent avoir un identifiant IMDb (`tt…`) connu dans la bibliothèque
  et une note MAL disponible via MDBList. Un titre identifié uniquement par `mal:`,
  `kitsu:` ou `tmdb:` sans identifiant IMDb reste sans note ; aucune correspondance
  approximative par titre n'est inventée.
- Lors de l'ajout depuis une fiche, l'identifiant IMDb explicite de cette fiche est
  maintenant conservé, même si son identifiant principal vient d'un autre catalogue.
- Le cache existant est un cache de session, pas une synchronisation permanente.
  Une grande bibliothèque peut consommer le quota MDBList. Une erreur réseau ou de
  quota peut laisser certains titres sans note ; revenir à un autre tri puis
  sélectionner à nouveau MAL permet de réessayer les requêtes échouées.
- L'application reste fondée sur la version alpha de Nuvio Desktop. Ne pas remplacer
  son installation habituelle sans sauvegarder les données. Une mise à jour officielle
  pourrait remplacer cette modification.

## Vérification et compilation

Les tests ajoutés couvrent le tri numérique, les notes absentes et invalides,
les égalités, la séparation film/série, les filtres, la persistance du choix,
la déduplication, la concurrence bornée, les échecs, l'annulation et les mises à
jour de la liste « Tout voir ».

Commande de validation dans un environnement disposant des dépendances du projet :

```powershell
.\gradlew.bat :composeApp:desktopTest --tests '*LibraryDisplaySettingsTest' --tests '*LibraryMalRatingsTest' --tests '*LibraryCatalogStateTest' --tests '*MdbListRatingsDecoderTest' --no-configuration-cache
```

Le workflow **Build Windows fork with MAL sorting** effectue ces tests,
compile une application portable et le MSI, puis les dépose dans les artefacts de l'exécution. Il ne publie pas de
release et ne requiert pas les secrets des développeurs de Nuvio. Il est prévu pour
être lancé sur la branche `feat/library-mal-rating-sort` après publication du fork.
Il démarre également sur les pushes vers cette branche si GitHub Actions est activé.

L'artefact `Nuvio-Desktop-MAL-Portable-Windows` contient `Nuvio.exe` et son runtime
Java. Extraire tout le ZIP dans un dossier puis ouvrir `Nuvio.exe` ; conserver les
sous-dossiers `app` et `runtime` à côté de l'exécutable. Cette version portable
ne nécessite pas l'installation du MSI. Voir le résultat de compilation ci-dessus.

Pour copier les sources dans `C:\codes\NuvioDesktop-MAL`, lancer manuellement
`Copier-vers-Codes.ps1` depuis le dossier actuel dans PowerShell. Le script refuse
de remplacer un dossier existant et conserve la copie d'origine. Il ne compile pas
l'application. Cette session ne dispose pas des droits d'écriture dans `C:\codes`.

Les intégrations nécessitant des identifiants de développeur (connexion cloud Nuvio,
Trakt OAuth, MDBList OAuth…) exigent une configuration propre au fork. Les clés des
mainteneurs ne sont pas incluses. Le secret facultatif `FORK_LOCAL_PROPERTIES_BASE64`
peut fournir un fichier `local.properties` à la compilation ; ne jamais y mettre des
identifiants de connexion personnels à distribuer dans l'application. Une clé API
MDBList utilisateur se renseigne dans l'application, après installation.

## Résultat des vérifications locales — 15 novembre 2026

- **27 tests réussis** : les quatre classes citées ci-dessus ont été compilées avec
  Kotlin 2.4.10 puis exécutées via JUnit 4.13.2, sur le vrai code partagé concerné.
  Ce test isolé remplace les services de plateforme, le stockage et les accès réseau
  par des doublures ; il ne valide pas le rendu Compose ni un appel réel à MDBList.
- Ressources XML françaises et anglaises : analysées sans erreur.
- `git diff --check` : aucune erreur.
- **Compilation complète non validée** : Gradle ne peut pas joindre les dépôts Maven
  depuis Java dans cet environnement (`java.net.SocketException: Permission denied:
  getsockopt`). Le lancement échoue pendant la résolution du plugin Foojay, avant
  la compilation du projet. Aucun MSI n'a donc été produit ou installé localement.
- Le workflow GitHub est préparé, mais n'a pas encore été exécuté.

Avant distribution, exécuter le workflow complet puis vérifier dans l'application :
une bibliothèque mêlant anime notés et non notés, les deux sens du tri, les vues
rangées/grille/« Tout voir », le changement de profil, les réglages MDBList désactivés
et une connexion interrompue. Vérifier que les notes correspondent aux fiches MAL.
