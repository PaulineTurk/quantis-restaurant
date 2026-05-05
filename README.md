# Partie 1 - RestoConnect

Application de mise en contact de restaurateurs et de clients sur place ou à emporter.

**Objectif** : Maximiser la valeur numérique (gain de temps, lisibilité des offres) sans complexité logistique.

**Livraison exclue** :

- Coûts élevés (commissions, logistique)

- Risques de fraude accrus (tarifs réduits)

- Marges insuffisantes

## Personas

| Client                     | Profil                            | Citation                                                                              | Frustrations                                                                                    | Besoins MVP                                                      | Priorité  |
|----------------------------|-----------------------------------|---------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|------------------------------------------------------------------|-----------|
| **Parent Débordé**         | 56 ans,<br> 4 enfants             | "Je finis tard le travail et n'ai plus le courage de cuisiner pour toute la tribu"    | • Prix identique quel que soit l'âge<br>• Budget très élevé<br>• Pas de reconnaissance fidélité | • Tarifs enfants<br>• Programme de fidélité                      | **HAUTE** |
| **Étudiant Pressé**        | 20 ans,<br>Habitudes alimentaires | "Je mange souvent la même chose, mais je dois retaper la même commande à chaque fois" | • Pas de tarif étudiant<br>• Ressaisie quotidienne                                              | • Tarif étudiant<br>• Commandes favorites<br>• Commande "1 clic" | **HAUTE** |
| **Chasseur de bons plans** | 30 ans,<br>Optimise ses dépenses  | "J'adore optimiser mes dépenses, il me faut un suivi visuel de mes gains"             | • Manque de visibilité sur les gains et paliers à atteindre<br>• Expérience plate               | • Gamification<br>• Suivi des gains<br>• Progression visible     | **BASSE** | 

---

| Restaurateur      | Profil                                 | Citation                                                                                  | Frustrations                                                                                  | Besoins MVP                                                                                                | Priorité  |
|-------------------|----------------------------------------|-------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------|-----------|
| **L'Innovatrice** | 35 ans,<br> Cuisine de saison          | "Ma carte change tout le temps selon les arrivages du marché"                             | • Difficulté de mise à jour de la carte en temps réel                                         | • Création/modification plats facilitée<br>• Switch ON/OFF plats                                           | **HAUTE** |
| **Le Formateur**  | 42 ans,<br> Équipe internationale      | "Mes saisonniers perdent les fiches recettes et les clients ne comprennent pas les menus" | • Barrière de la langue <br>• gestion complexe des ingrédients et allergènes pour les clients | • Recettes numériques<br>• Menu multilingue<br>• Affichage auto ingrédients<br>• Détection auto allergènes | **HAUTE** |
| **L'Analyste**    | 27 ans,<br> Gestion rigoureuse         | "C'est la loterie pour l'approvisionnement : je commande souvent trop ou pas assez"       | • Gaspillage alimentaire<br>• Manque de données sur les habitudes de consommation             | • Historique analytique<br>• Stats de vente/plat<br>• Aide anticipation préparations                       | **HAUTE** |
| **Le Routier**    | 51 ans,<br> Zone de passage            | "Mes clients sont de passage, les cartes de fidélité papier finissent perdues"            | • Clientèle volatile<br>• impossibilité de fidéliser sans support digital                     | • Fidélité 100% digitale<br>                                                                               | **HAUTE** |
| **L'Astucieux**   | 21 ans,<br> Optimisation des commandes | "Si j'avais su que cette commande arrivait, j'aurais groupé mes préparations"             | • Manque d'anticipation des commandes                                                         | • Conseils automatiques à la préparation                                                                   | **BASSE** |
| **La Chimiste**   | 28 ans,<br> Visibilité niche           | "Ma cuisine moléculaire est noyée par les fast food !"                                    | • Manque de visibilité                                                                        | • Moteur de recherche de restaurant par catégorie fine                                                     | **BASSE** |

## Fonctionnalités

**V1 – Essentiel (MVP)**

Client

- Compte & identité numérique
- Recherche restaurants / menus
- Commande + paiement sécurisé
- Factures & historique
- Validation statut (étudiant/enfant)
- Moteur de calcul des remises
- Fidélité digitale automatisée
- Extraction allergènes / ingrédients

Restaurateur

- Compte & identité numérique
- Souscription abonnements
- Gestion menus (plats, recettes)
- Suivi commandes temps réel
- Back-office simple (recettes + allergènes)

**V2 – Améliorations**

Client

- Gamification (progression récompenses)
- Détection de fraudes
    - 3 commandes/jour/utilisateur max
    - Détection de comportements suspects
- Traduction automatique des menus
- Favoris & commande rapide

Restaurateur

- Dashboard analytics + recommandations (stocks, affluence)
- Gestion des Risques
    - Plafond de remise : max 50% par commande

**Backlog (optionnel)**

Client

- Commande multi-restaurants (≤5 km)
- Précommande avec créneau

Restaurateur

- Gestion multi-restaurants
- Smart prep (optimisation cuisine)
- Catégorisation avancée & publicité locale

## Specificités additionnelles

- Remises cumulées (non basées sur montant résiduel)
- ≥ 1 menu obligatoire par commande
- Multi-restaurant :
    - Fidélité : +1 point plateforme +1 point / restaurant
    - Rayon max : 5 km

## Architecture

- API REST : utiliser [api](api.yaml) → https://editor.swagger.io/
- Base de données : utiliser [bdd-schema](bdd-schema.dbml) → https://dbdiagram.io