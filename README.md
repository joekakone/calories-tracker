# 🥗 Calories Tracker

Calories Tracker est une application Android intelligente conçue pour vous aider à suivre vos apports nutritionnels quotidiens en toute simplicité. Fini les entrées manuelles fastidieuses : prenez une photo de votre repas, et l'intelligence artificielle s'occupe du reste !

## ✨ Fonctionnalités Principales

*   📸 **Analyse par Intelligence Artificielle** : Grâce à l'intégration de Gemini (Google AI), l'application analyse la photo de votre plat et estime automatiquement :
    *   Le nom du plat
    *   Les calories totales
    *   Les macronutriments : Protéines, Glucides, et Lipides
    *   Un badge de santé ("Bonne bouffe" / "Mal bouffe")
    *   Les ingrédients détaillés
*   📊 **Suivi des Macros** : Visualisez d'un coup d'œil vos apports journaliers.
*   🕒 **Historique des Repas** : Gardez une trace complète de vos précédents repas avec une interface claire, des badges par macronutriment et la composition détaillée.
*   👤 **Gestion de Profil** : Inscription et connexion simples pour suivre vos progrès personnels.

## 🚀 Technologies Utilisées

*   **Langage** : Kotlin
*   **Base de données** : Room (SQLite) pour le stockage local
*   **Intelligence Artificielle** : API Gemini (Google Generative AI)
*   **UI/UX** : XML Layouts avec Material Design Components
*   **Architecture** : Navigation Component (Single Activity, Multiple Fragments)

## 🗄️ Structure de la Base de Données

L'application utilise **Room** (SQLite) pour stocker les données localement. L'entité principale pour les repas est `MealEntity`, stockée dans la table `meals`.

### Table `meals`

| Champ | Type | Description |
| :--- | :--- | :--- |
| `id` | `Int` | Clé primaire (auto-incrémentée) |
| `userId` | `Int` | Identifiant de l'utilisateur ayant enregistré le repas |
| `name` | `String` | Nom du plat (ex: "Salade César") |
| `mealType` | `String` | Type de repas (ex: "Petit déjeuner", "Snack") |
| `calories` | `Int` | Nombre total de calories |
| `carbs` | `Int` | Glucides en grammes |
| `protein` | `Int` | Protéines en grammes |
| `fat` | `Int` | Lipides en grammes |
| `dateTimestamp` | `Long` | Timestamp de l'enregistrement du repas |
| `imageUri` | `String?` | Chemin local de l'image du repas (URI) |
| `healthBadge` | `String?` | Badge santé attribué (ex: "Bonne bouffe") |
| `ingredients` | `String?` | Liste des ingrédients sérialisée au format JSON |

## 🛠️ Configuration & Installation

1.  **Cloner le dépôt** :
    ```bash
    git clone https://github.com/joekakone/calories-tracker.git
    ```
2.  **Configurer la clé API Gemini** :
    *   Obtenez une clé API depuis le [Google AI Studio](https://aistudio.google.com/).
    *   Créez ou modifiez le fichier `local.properties` à la racine du projet et ajoutez votre clé :
        ```properties
        GEMINI_API_KEY=VOTRE_CLE_API_ICI
        ```
    *   *(Note : `local.properties` est ignoré par Git pour des raisons de sécurité).*
3.  **Compiler le projet** :
    *   Ouvrez le projet dans Android Studio.
    *   Synchronisez Gradle.
    *   Lancez l'application sur un émulateur ou un appareil physique.

## 📱 Captures d'écran

*(Ajoutez ici les captures d'écran de l'application : Écran de connexion, Dashboard, Scan de plat, Historique et Détail du repas).*

## 📄 Licence

Ce projet est réalisé dans un but éducatif et de portfolio.
