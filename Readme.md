# stock-pignon 🚲

Application Android (4.2+) de gestion de stock et d'aide à la vente à prix libre. Conçue pour l'[**Atelier du Pignon**](https://www.atelierdupignon.fr/), atelier d'auto-réparation à Nantes.

![Pignon1](https://github.com/user-attachments/assets/1e43e8f2-872c-41c2-9e12-405e41db5ca1)

## 🛠 Fonctionnalités
- **Catalogue visuel** : Permet aux adhérent·es de noter rapidement les pièces qu'iels ont emportées.
- **Prix libre & conscient** : Calculateur de panier affichant une fourchette de prix suggérée.
- **Clé en main** : Par défaut, l'application installera un catalogue par défaut, utilisable tout de suite pour un atelier d'autoréparation de vélo.
- **Modulable** : Le catalogue de pièces peut-être modifié sans connaissance informatique préalable et sans recompilation de l'application, via un fichier JSON en entrée et un dossier d'images.
- **Traçabilité** : Les quantités emportées est consignée dans un fichier JSON en sortie.
- **Réemploi** : L'application a été développé pour réutiliser de vieilles tablettes Android laissées à l'abandon. Ainsi, la compatibilité est assurée pour les versions Android Jelly Bean 4.2 et supérieures.

## ⚠️Avertissement
- Cette application a été développée pour fonctionner uniquement avec la tablette **Acer Iconia A3-A10**.
- L'application devrait fonctionner sur une tablette similaire : Android 4.2 ou supérieur + écran équivalent (10.1" - 1280 x 800). Cela n'a cependant pas encore été testé.

## 📱 Utilisation
1. [Télécharger l'APK](https://github.com/lucasroyerdev/stock-pignon/releases/tag/v0.1.0) sur la tablette et installer.
2. À la première utilisation, un dossier contenant un dossier d'exemple `stock_pignon` est créé dans la mémoire interne.
3. Si vous souhaitez modifier les objets présentés, vous pouvez modifier `stock_pignon/pieces.json` ainsi que les images dans le dossier `stock_pignon/images`.
4. Après la validation du premier panier, un fichier `stock_pignon/stock.json` sera créé afin de comptabiliser les pièces emportées.

## 💻 Développement
- **Langage** : Java 8 (Android Natif)
- **SDK Android** : API 17 (Android 4.2 Jelly Bean)
- **Environnement** : Android Studio Otter | 2025.2.1 Patch 1

## ⚖️ Licence

Ce projet est sous licence **CC BY-NC-SA 4.0**.  
Cela signifie que vous êtes libre de partager et d'adapter le code, tant que vous citez l'auteur original, que vous n'en faites pas un usage commercial, et que vous diffusez vos modifications sous la même licence.

## 👀 Screenshots
![Pignon2](https://github.com/user-attachments/assets/78439dff-cde4-4d75-8a8f-f6ee38a69abf)
![Pignon3](https://github.com/user-attachments/assets/51694675-ec5f-4a59-95a9-7b7e0dd1caf3)
![Pignon4](https://github.com/user-attachments/assets/f402250c-045c-4b3d-bbdc-11ae22bd62b2)

