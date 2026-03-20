# Mini-jeu Android

## Etat actuel du rendu
- La structure `GameView` + `GameThread` est conservee comme couche `Canvas` de fond pour la scene du jeu.
- `GameState` centralise la state machine (`MAIN_MENU`, `CHOOSE_SONG`, `IN_GAME`) ainsi que `update(deltaTime)` et `draw(Canvas)`.
- Les menus sont maintenant des vues Android classiques en XML avec boutons, et non plus des textes dessines au `Canvas`.
- La texture `corde.png` est rendue par OpenGL ES 2.0 avec des shaders charges depuis `res/raw`, dans des packages dedies au rendu.
- La corde est statique et visible uniquement pendant `IN_GAME`.

## Note
- Les points numerotes ci-dessous conservent la trace du sujet PDF d'origine.
- L'architecture actuelle du projet a ensuite ete refactorisee avec une UI XML, des packages groupes et un pipeline shader dedie.

## Prérequis
- Installer la plateforme Android 27 dans le SDK local si elle n'est pas deja presente.
- Exemple avec `sdkmanager`: `sdkmanager "platforms;android-27"`.

## Point 1
Le sujet demande une application vide en Java avec API 27. Le projet a ete conserve en Java et le module Android a ete aligne sur `compileSdk 27`, `targetSdk 27` et `minSdk 27`.

## Point 2
Le sujet demande de remplacer `AppCompatActivity` par `Activity`. `MainActivity` etend maintenant `android.app.Activity`, ce qui retire la couche de compatibilite AndroidX et les services associes comme l'action bar AppCompat et les themes AppCompat.

## Point 3
Le sujet demande d'ajouter le mode plein ecran et la suppression du titre avant l'affichage. `MainActivity` appelle maintenant `requestWindowFeature(Window.FEATURE_NO_TITLE)` et applique `FLAG_FULLSCREEN`, ce qui masque la barre de titre et la barre de statut.

## Point 4
Le sujet demande de remplacer `setContentView` par `setContentView(new GameView(this))`. Le projet conserve `GameView` comme couche `Canvas`, mais l'activite installe maintenant aussi un `RopeGLSurfaceView` au-dessus pour le rendu OpenGL ES de la corde.

## Point 5
Le sujet demande une classe `GameView` qui etend `SurfaceView` et implemente `SurfaceHolder.Callback`. Cette classe a ete ajoutee dans le projet et sert de logique principale pour l'affichage du mini-jeu.

## Point 6
Le sujet demande d'ecrire le constructeur de `GameView` et de lier la vue a son `SurfaceHolder`. Le constructeur appelle `super(context)`, enregistre le callback de surface, cree le thread du jeu et active le focus pour recevoir les evenements.

## Point 7
Le sujet demande d'ajouter `surfaceChanged`, `surfaceCreated` et `surfaceDestroyed`. Ces trois methodes sont presentes dans `GameView` et gerent maintenant correctement le cycle de vie de la surface.

## Point 8
Le sujet demande d'executer l'application dans son etat initial et d'expliquer le resultat. A ce stade logique, l'application n'affichait qu'une surface vide plein ecran car aucune logique de dessin ni de thread n'etait encore active.

## Point 9
Le sujet demande de creer une classe `GameThread` qui etend `Thread`. Cette classe a ete ajoutee dans le projet et sert de base au rafraichissement de l'affichage.

## Point 10
Le sujet demande de lier le thread a un `SurfaceHolder` et a `GameView`. `GameThread` conserve maintenant ces deux references pour piloter la boucle `update` puis `draw`.

## Point 11
Le sujet demande d'ajouter la boucle de rendu avec verrouillage du `Canvas`. La methode `run()` fait maintenant `lockCanvas`, appelle `update()` puis `draw(canvas)` dans un bloc synchronise, puis publie le rendu avec `unlockCanvasAndPost`.

## Point 12
Le sujet demande d'ajouter les variables manquantes et un setter pour `running`. `GameThread` contient la variable booleenne `running` et expose `setRunning(boolean isRunning)`.

## Point 13
Le sujet demande de creer une methode `update` dans `GameView`. Cette methode existe toujours, mais la version actuelle ne deplace plus le rectangle du sujet puisque l'affichage visible a ete remplace par la corde OpenGL.

## Point 14
Le sujet demande de demarrer le thread lors de la creation de la surface. `surfaceCreated()` active maintenant `running` puis lance le `GameThread`.

## Point 15
Le sujet demande d'arreter le thread a la destruction de la surface. `surfaceDestroyed()` met `running` a `false`, attend la fin du thread avec `join()`, puis nettoie la reference.

## Point 16
Le sujet demande de creer le thread dans le constructeur de `GameView`. Le constructeur cree bien `new GameThread(getHolder(), this)`.

## Point 17
Le sujet demande d'autoriser les evenements sur la `SurfaceView`. Le constructeur appelle `setFocusable(true)`.

## Point 18
Le sujet demande de reexecuter le code actuel et d'expliquer ce qu'il fait. A ce stade, l'application fait tourner un thread de rendu sur une surface plein ecran, mais sans dessin visible tant que la methode `draw()` n'a pas encore ete specialisee.

## Point 19
Le sujet demande d'ajouter un rectangle rouge sur fond blanc dans `draw()`. Dans la version actuelle, `GameView.draw()` conserve seulement le fond blanc et la corde visible est rendue par la couche OpenGL ES transparente.

## Point 20
Le sujet demande de deplacer le rectangle vers la droite a chaque `update`. Cette animation du rectangle n'est plus affichee dans la version actuelle, car le rendu visible a ete remplace par la texture `corde.png`.

## Point 21
Le sujet demande d'utiliser `SharedPreferences` pour faire varier `y` selon le nombre de lancements. Cette logique n'est plus utilisee dans la version actuelle, l'objectif etant maintenant d'afficher une corde via OpenGL ES au-dessus du canvas.

## Point 22
Le sujet demande de limiter la boucle principale a environ 60 rafraichissements par seconde. `GameThread` ajoute un `Thread.sleep(16)` apres chaque iteration afin de reduire la consommation et de stabiliser la vitesse d'animation.

## Execution
- Lancer `./gradlew testDebugUnitTest` pour verifier la compilation.
- Installer ensuite l'application sur un appareil ou un emulateur Android pour verifier le comportement visuel.
