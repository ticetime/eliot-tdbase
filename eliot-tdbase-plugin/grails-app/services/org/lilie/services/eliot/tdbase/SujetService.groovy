/*
 * Copyright © FYLAB and the Conseil Régional d'Île-de-France, 2009
 * This file is part of L'Interface Libre et Interactive de l'Enseignement (Lilie).
 *
 * Lilie is free software. You can redistribute it and/or modify since
 * you respect the terms of either (at least one of the both license) :
 * - under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * - the CeCILL-C as published by CeCILL-C; either version 1 of the
 * License, or any later version
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this software. View the full text of
 * the exception in file LICENSE.txt in the directory of this software
 * distribution.
 *
 * Lilie is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Licenses for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the CeCILL-C along with Lilie. If not, see :
 *  <http://www.gnu.org/licenses/> and
 *  <http://www.cecill.info/licences.fr.html>.
 */

package org.lilie.services.eliot.tdbase

import groovy.time.TimeCategory
import org.hibernate.SQLQuery
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.lilie.services.eliot.tice.Attachement
import org.lilie.services.eliot.tice.CopyrightsType
import org.lilie.services.eliot.tice.CopyrightsTypeEnum
import org.lilie.services.eliot.tice.Publication
import org.lilie.services.eliot.tice.annuaire.Personne
import org.lilie.services.eliot.tice.utils.StringUtils
import org.springframework.transaction.annotation.Transactional

class SujetService {

  static transactional = false

  QuestionService questionService
  ArtefactAutorisationService artefactAutorisationService
  CopieService copieService
  ReponseService reponseService
  SessionFactory sessionFactory

  def grailsApplication

  /**
   * Créé un sujet
   * @param proprietaire le proprietaire du sujet
   * @param titre le titre du sujet
   * @return le sujet créé
   */
  @Transactional
  Sujet createSujet(Personne proprietaire, String titre) {
    Sujet sujet = new Sujet(proprietaire: proprietaire,
        titre: titre,
        titreNormalise: StringUtils.normalise(titre),
        accesPublic: false,
        accesSequentiel: false,
        ordreQuestionsAleatoire: false,
        publie: false,
        versionSujet: 1,
        copyrightsType: CopyrightsTypeEnum.TousDroitsReserves.copyrightsType,
        sujetType: SujetTypeEnum.Sujet.sujetType)

    sujet.addPaterniteItem(proprietaire)

    sujet.save(flush: true)
    return sujet
  }

  @Transactional
  Sujet rendNouveauSujetCollaboratif(Personne personne,
                                    Sujet sujet,
                                    Set<Personne> contributeurs) {
    assert (sujet.id == null)

    // Rend le sujet collaboratif
    sujet.collaboratif = true
    sujet.termine = false


    if (sujet.estUnExercice()) {
      Question questionComposite = sujet.questionComposite
      questionComposite.collaboratif = true
      questionComposite.save()
    }

    return fusionneSujetCollaboratifContributeurs(sujet, contributeurs)
  }

  @Transactional
  Sujet createSujetCollaboratifFrom(Personne personne,
                                    Sujet sujetOriginal,
                                    Set<Personne> contributeurSet) {
    assert (
        artefactAutorisationService.utilisateurPeutDupliquerArtefact(
            personne,
            sujetOriginal
        )
    )

    // Recopie le sujet original
    Sujet sujetCollaboratif = new Sujet(
        proprietaire: personne,
        titre: sujetOriginal.titre,
        titreNormalise: sujetOriginal.titreNormalise,
        presentation: sujetOriginal.presentation,
        presentationNormalise: sujetOriginal.presentationNormalise,
        accesPublic: false,
        accesSequentiel: sujetOriginal.accesSequentiel,
        ordreQuestionsAleatoire: sujetOriginal.ordreQuestionsAleatoire,
        publie: false,
        copyrightsType: sujetOriginal.copyrightsType,
        sujetType: sujetOriginal.sujetType,
        paternite: sujetOriginal.paternite
    )
    sujetCollaboratif.save()

    // Rend le sujet collaboratif
    sujetCollaboratif.collaboratif = true
    sujetCollaboratif.termine = false
    contributeurSet.each {
      sujetCollaboratif.addToContributeurs(it)
    }
    sujetCollaboratif.addPaterniteItem(
        personne,
        null,
        contributeurSet.collect { it.nomAffichage }
    )
    sujetCollaboratif.save(flush: true, failOnError: true)

    // Si le sujet est un exercice, on crée la questionComposite associée
    if (sujetCollaboratif.estUnExercice()) {
      createQuestionCompositeForExercice(sujetCollaboratif, personne)
    }

    // recopie de la séquence de questions (copie en profondeur pour rendre les questions collaboratives pour le sujet)
    sujetOriginal.questionsSequences.each { SujetSequenceQuestions sujetQuestion ->
      Question questionCollaborative
      if (sujetQuestion.question.estComposite()) {
        questionCollaborative = createSujetCollaboratifFrom(
            personne,
            sujetQuestion.question.exercice,
            contributeurSet
        ).questionComposite
        questionCollaborative.sujetLie = sujetCollaboratif
        questionCollaborative.save()
      } else {
        questionCollaborative = questionService.createQuestionCollaborativeFrom(
            personne,
            sujetQuestion.question,
            sujetCollaboratif
        )
      }

      SujetSequenceQuestions copieSujetSequence = new SujetSequenceQuestions(
          question: questionCollaborative,
          sujet: sujetCollaboratif,
          noteSeuilPoursuite: sujetQuestion.noteSeuilPoursuite
      )
      sujetCollaboratif.addToQuestionsSequences(copieSujetSequence)
      copieSujetSequence.save(flush: true, failOnError: true)
    }

    return sujetCollaboratif
  }

  /**
   * Recopie un sujet
   * @param sujet le sujet à recopier
   * @param proprietaire le proprietaire
   * @param nouveauTitre le titre du sujet créé, si null le titre du sujet original
   * sera suffixé par " (Copie)"
   * @return la copie du sujet
   */
  @Transactional
  Sujet recopieSujet(Sujet sujet,
                     Personne proprietaire,
                     String nouveauTitre = null) {
    // verification securité
    assert (
        artefactAutorisationService.utilisateurPeutDupliquerArtefact(
            proprietaire,
            sujet
        )
    )

    if (!nouveauTitre) {
      nouveauTitre = sujet.titre + " (Copie)"
    }

    Sujet sujetCopie = new Sujet(
        proprietaire: proprietaire,
        titre: nouveauTitre,
        titreNormalise: StringUtils.normalise(nouveauTitre),
        presentation: sujet.presentation,
        presentationNormalise: sujet.presentationNormalise,
        accesPublic: false,
        accesSequentiel: sujet.accesSequentiel,
        ordreQuestionsAleatoire: sujet.ordreQuestionsAleatoire,
        publie: false,
        copyrightsType: sujet.copyrightsType,
        sujetType: sujet.sujetType,
        paternite: sujet.paternite
    )
    sujetCopie.save()


    // recopie de la séquence de questions (dans le cas d'un sujet non collaboratif, ce n'est pas une copie en profondeur ; pour un sujet collaboratif les questions sont dupliquées en version non collaboratives)
    sujet.questionsSequences.each { SujetSequenceQuestions sujetQuestion ->

      Question questionOriginale = sujetQuestion.question
      Question question

      if(sujet.estCollaboratif()) {
        if(questionOriginale.estComposite()) {
          Sujet exerciceRecopie = recopieSujet(
              questionOriginale.exercice,
              proprietaire
          )
          question = exerciceRecopie.questionComposite
        }
        else {
          question = questionService.recopieQuestion(
              questionOriginale,
              proprietaire
          )
        }
      }
      else {
        question = questionOriginale
      }

      SujetSequenceQuestions copieSujetSequence = new SujetSequenceQuestions(
          question: question,
          sujet: sujetCopie,
          noteSeuilPoursuite: sujetQuestion.noteSeuilPoursuite
      )
      sujetCopie.addToQuestionsSequences(copieSujetSequence)
      copieSujetSequence.save()
    }

    sujetCopie.save()
    if (sujetCopie.estUnExercice()) {
      createQuestionCompositeForExercice(sujetCopie, proprietaire)
    }
    return sujetCopie
  }

  /**
   * Change le titre du sujet
   * @param sujet le sujet à modifier
   * @param nouveauTitre le titre
   * @return le sujet
   */
  Sujet updateTitreSujet(Sujet sujet, String nouveauTitre, Personne proprietaire) {
    // verif securite
    assert (artefactAutorisationService.utilisateurPeutModifierArtefact(proprietaire, sujet))

    if (!isDernierAuteur(sujet, proprietaire)) {
      sujet.addPaterniteItem(proprietaire)
    }

    sujet.titre = nouveauTitre
    sujet.titreNormalise = StringUtils.normalise(nouveauTitre)
    sujet.save()
    if (sujet.estUnExercice()) {
      def question = sujet.questionComposite
      updateTitreQuestionComposite(nouveauTitre, question)
    }
    return sujet
  }

  /**
   * Modifie les proprietes du sujet passé en paramètre
   * @param sujet le sujet
   * @param proprietes les nouvelles proprietes
   * @param proprietaire le proprietaire
   * @return le sujet
   */
  @Transactional
  Sujet updateProprietes(Sujet sujet, Map proprietes, Personne proprietaire) {
    // verif securite
    if (sujet.id != null) { // sujet existant
      assert (artefactAutorisationService.utilisateurPeutModifierArtefact(proprietaire, sujet))
    } else { // sujet venant d'être créé
      sujet.proprietaire = proprietaire
      sujet.copyrightsType = CopyrightsType.getDefault()
    }

    if (!isDernierAuteur(sujet, proprietaire)) {
      sujet.addPaterniteItem(proprietaire)
    }

    if (proprietes.containsKey("contributeurIds")) {
      Collection contributeurIds = proprietes["contributeurIds"]

      // Vérification que tous les contributeurs actuels sont bien dans la liste des contributeurs fournis
      sujet.contributeurs?.each {
        if (!contributeurIds.contains(it.id)) {
          throw new IllegalStateException(
              "On ne peut pas retirer un contributeur d'un sujet collaboratif. " +
                  "Les contributeurs actuels sont : ${sujet.contributeurs*.id}, " +
                  "Les contributeurs fournis sont : $contributeurIds."
          )
        }
      }

      int nbContributeurActuel = sujet.contributeurs ? sujet.contributeurs.size() : 0
      if (contributeurIds.size() > nbContributeurActuel) {

        if (sujet.estUnExercice() && sujet.estCollaboratif()) {
          throw new IllegalStateException(
              "Il n'est pas possible de modifier les contributeurs d'un exercice collaboratif"
          )
        }

        Set<Personne> contributeurs = Personne.getAll(contributeurIds)

        if (sujet.id) {
          sujet = fusionneSujetContributeurs(proprietaire, sujet, contributeurs)
          // Le sujet initial sera dupliqué s'il n'est pas collaboratif
        } else {
          // Rend le nouveau sujet collaboratif
          sujet.collaboratif = true
          sujet.termine = false
          contributeurs.each {
            sujet.addToContributeurs(it)
          }
          sujet.addPaterniteItem(
              proprietaire,
              null,
              contributeurs.collect { it.nomAffichage }
          )
        }
      }
    }

    if (proprietes.titre && sujet.titre != proprietes.titre) {
      sujet.titreNormalise = StringUtils.normalise(proprietes.titre)
    }
    if (proprietes.presentation && sujet.presentation != proprietes.presentation) {
      sujet.presentationNormalise = StringUtils.normalise(proprietes.presentation)
    }
    sujet.properties = proprietes
    if (sujet.save(flush: true)) {

      // traitement de la question associee au sujet si le sujet est un exercice
      def question = sujet.questionComposite
      if (sujet.estUnExercice()) {
        if (!question) {
          createQuestionCompositeForExercice(sujet, proprietaire)
        } else {
          // il faut mettre a jour le titre de la question
          updateTitreQuestionComposite(sujet.titre, question)
        }
      } else {
        // si le sujet était un exercice mais ne l'est plus, suppression de
        // la question associée
        if (question) {
          supprimeQuestionComposite(question, proprietaire)
        }
      }
    }

    return sujet
  }

  private Sujet fusionneSujetContributeurs(Personne proprietaire, Sujet sujet, Set<Personne> contributeurs) {
    if (!contributeurs) {
      throw new IllegalArgumentException("La liste de contributeur est vide")
    }

    if (sujet.id == null) {
      return rendNouveauSujetCollaboratif(proprietaire, sujet, contributeurs)
    } else if (!sujet.collaboratif) {
      return createSujetCollaboratifFrom(proprietaire, sujet, contributeurs)
    } else {
      return fusionneSujetCollaboratifContributeurs(sujet, contributeurs)
    }
  }

  private Sujet fusionneSujetCollaboratifContributeurs(Sujet sujet, Set<Personne> contributeurs) {
    Set<Personne> nouveauContributeurSet = []

    contributeurs.each {
      if(!sujet.contributeurs*.id.contains(it.id)) {
        sujet.addToContributeurs(it)
        nouveauContributeurSet << it
      }

    }
    sujet.addPaterniteItem(
        sujet.proprietaire,
        null,
        nouveauContributeurSet.collect { it.nomAffichage }
    )
    sujet.save(flush: true, failOnError: true)

    if (sujet.estUnExercice()) {
      Question questionComposite = sujet.questionComposite
      nouveauContributeurSet.each {
        questionComposite.addToContributeurs(it)
      }
      questionComposite.save()
    }

    sujet.questionsSequences.each { SujetSequenceQuestions sujetQuestion ->
      if (sujetQuestion.question.estComposite()) {
        fusionneSujetCollaboratifContributeurs(sujetQuestion.question.exercice, nouveauContributeurSet)
      }

      nouveauContributeurSet.each {
        sujetQuestion.question.addToContributeurs(it)
      }
      sujetQuestion.question.addPaterniteItem(
          sujet.proprietaire,
          null,
          nouveauContributeurSet.collect { it.nomAffichage }
      )

      sujetQuestion.save()
    }

    return sujet
  }

  /**
   * Supprime un sujet
   * @param sujet la question à supprimer
   * @param supprimeur la personne tentant la suppression
   */
  @Transactional
  def supprimeSujet(Sujet sujet, Personne supprimeur) {
    assert (artefactAutorisationService.utilisateurPeutSupprimerArtefact(supprimeur, sujet))

    // si le sujet est un exercice, suppression de la question associée
    def question = sujet.questionComposite
    if (question) {
      supprimeQuestionComposite(question, supprimeur)
    }
    // suppression des copies jetables attachees au sujet
    copieService.supprimeCopiesJetablesForSujet(sujet)

    // suppression des sujetQuestions
    def sujetQuests = SujetSequenceQuestions.where {
      sujet == sujet
    }
    sujetQuests.deleteAll()

    // suppression de la publication si necessaire
    if (sujet.estPartage()) {
      sujet.publication.delete()
    }

    // Gestion des sujets masqués
    List<SujetMasque> sujetMasques = SujetMasque.findAllBySujet(sujet)
    if (!sujetMasques?.empty) {
      SujetMasque.deleteAll(sujetMasques)
    }

    // on supprime enfin le sujet
    sujet.delete()
  }

/**
 *  Partage un sujet
 * @param sujet le sujet à partager
 * @param partageur la personne souhaitant partager
 */
  @Transactional
  def partageSujet(Sujet sujet, Personne partageur) {
    assert (artefactAutorisationService.utilisateurPeutPartageArtefact(partageur, sujet))
    CopyrightsType ct = CopyrightsTypeEnum.CC_BY_NC.copyrightsType
    Publication publication = new Publication(dateDebut: new Date(),
        copyrightsType: ct)
    publication.save()
    sujet.copyrightsType = ct
    sujet.publication = publication
    sujet.publie = true
    // il faut partager les questions qui ne sont pas partagées
    sujet.questionsSequences.each {
      def question = it.question
      if (question.estComposite() && !question.exercice.estPartage()) {
        partageSujet(question.exercice, partageur)
      } else {
        if (!question.estPartage()) {
          questionService.partageQuestion(question, partageur)
        }
      }
    }

    sujet.addPaterniteItem(
        partageur,
        publication.dateDebut
    )

    // si le sujet est un exercice, partage de la question associee
    def question = sujet.questionComposite
    if (question) {
      partageQuestionComposite(question)
    }

    return sujet
  }

  /**
   * Teste si un utilisateur est le dernier auteur d'un sujet (i.e. le dernier à
   * avoir modifié le sujet)
   * @param sujet
   * @param utilisateur
   * @return
   */
  boolean isDernierAuteur(Sujet sujet, Personne utilisateur) {
    Paternite paternite = new Paternite(sujet.paternite)

    if (!paternite.paterniteItems) {
      return false
    }

    return paternite.paterniteItems.last()?.auteur == utilisateur.nomAffichage
  }

  /**
   * Récupère la liste des ids de tous les sujets masqués d'une
   * personne, ou seulement un sous ensemble dans la liste des
   * sujets fournis en paramètre
   * @param personne
   * @param sujets sous ensemble de sujets dans lesquels cherche
   * @return liste d'ids de sujets
   */
  Set<Long> listIdsSujetsMasques(Personne personne, List<Sujet> sujets = null) {

    if (sujets == null) {
      return SujetMasque.findAllByPersonne(personne)*.sujetId
    }
    else {
      return SujetMasque.findAllByPersonneAndSujetInList(personne, sujets)*.sujetId
    }
  }

/**
 * Recherche de sujets
 * @param chercheur la personne effectuant la recherche
 * @param patternTitre le pattern saisi pour le titre
 * @param patternAuteur le pattern saisi pour l'auteur
 * @param patternPresentation le pattern saisi pour la presentation
 * @param matiereBcn la matiere
 * @param niveau le niveau
 * @param paginationAndSortingSpec les specifications pour l'ordre et
 * la pagination
 * @param uniquementSujetsChercheur flag indiquant si la recherche ne porte
 * que sur les sujets du chercheur
 * @return la liste des sujets
 */
  List<Sujet> findSujets(Personne chercheur,
                         String patternTitre,
                         String patternAuteur,
                         String patternPresentation,
                         ReferentielEliot referentielEliot,
                         SujetType sujetType,
                         Boolean uniquementSujetsChercheur = false,
                         Map paginationAndSortingSpec = null,
                         Boolean afficheSujetMasque = false) {

    if (!chercheur) {
      throw new IllegalArgumentException("sujet.recherche.chercheur.null")
    }

    if (paginationAndSortingSpec == null) {
      paginationAndSortingSpec = [:]
    }

    Map parametres = [
        "proprietaire_id": chercheur.id
    ]

    String sql = """
      select sujet.id
      from td.sujet sujet,
        ent.personne proprietaire
      where sujet.proprietaire_id = proprietaire.id

        and (
          sujet.proprietaire_id = :proprietaire_id
          or exists (
            select 1 from td.sujet_contributeur sujet_contributeur
            where sujet_contributeur.sujet_id = sujet.id
            and sujet_contributeur.personne_id = :proprietaire_id)"""

    if (!uniquementSujetsChercheur) {
      sql += """
          or sujet.publie = true"""
    }

    sql += ")"

    if (patternAuteur) {
      parametres.put("pattern_auteur_normalise", "%${StringUtils.normalise(patternAuteur)}%".toString())

      sql += """
        and (
          proprietaire.nom_normalise like :pattern_auteur_normalise
          or proprietaire.prenom_normalise like :pattern_auteur_normalise
          or exists (
              select 1
              from td.sujet_contributeur sujet_contributeur,
                ent.personne contributeur
              where sujet_contributeur.sujet_id = sujet.id
                and sujet_contributeur.personne_id = contributeur.id
                and (
                  contributeur.nom_normalise like :pattern_auteur_normalise
                  or contributeur.prenom_normalise like :pattern_auteur_normalise)))"""
    }

    if (!afficheSujetMasque) {
      sql += """
        and not exists (
          select 1
          from td.sujet_masque sujet_masque
          where sujet_masque.sujet_id = sujet.id
            and sujet_masque.personne_id = :proprietaire_id)"""
    }

    if (referentielEliot?.matiereBcn) {
      parametres.put("matiere_bcn_id", referentielEliot.matiereBcn.id)
      sql += """
        and sujet.matiere_bcn_id = :matiere_bcn_id"""
    }

    if (referentielEliot?.niveau) {
      parametres.put("niveau_id", referentielEliot.niveau.id)
      sql += """
        and sujet.niveau_id = :niveau_id"""
    }

    if (sujetType) {
      parametres.put("sujet_type_id", sujetType.id)
      sql += """
        and sujet.sujet_type_id = :sujet_type_id"""
    }

    if (patternTitre) {
      parametres.put("titre_normalise", "%${StringUtils.normalise(patternTitre)}%".toString())
      sql += """
        and sujet.titre_normalise like :titre_normalise"""
    }

    if (patternPresentation) {
      parametres.put("presentation_normalise", "%${StringUtils.normalise(patternPresentation)}%".toString())
      sql += """
        and sujet.presentation_normalise like :presentation_normalise"""
    }

    Session session = sessionFactory.getCurrentSession()
    SQLQuery sqlQuery = session.createSQLQuery(sql)

    parametres.each { k, v ->
      sqlQuery.setParameter(k, v)
    }

    List sujetIds = sqlQuery.list().collect { it.longValue() }

    def criteria = Sujet.createCriteria()
    List<Sujet> sujets = criteria.list(paginationAndSortingSpec) {

      if (sujetIds.isEmpty()) {
        isNull 'id'
      }
      else {
        inList 'id', sujetIds
      }

      if (paginationAndSortingSpec) {
        def sortArg = paginationAndSortingSpec['sort'] ?: 'lastUpdated'
        def orderArg = paginationAndSortingSpec['order'] ?: 'desc'
        if (sortArg) {
          order "${sortArg}", orderArg
        }

      }
    }

    return sujets
  }

/**
 * Recherche de tous les sujet pour un proprietaire donné
 * @param proprietaire la personne effectuant la recherche
 * @param paginationAndSortingSpec les specifications pour l'ordre et
 * la pagination
 * @return la liste des sujets
 */
  List<Sujet> findSujetsForProprietaire(Personne proprietaire,
                                        Map paginationAndSortingSpec = null) {
    if (!proprietaire) {
      throw new IllegalArgumentException("sujet.recherche.chercheur.null")
    }
    if (paginationAndSortingSpec == null) {
      paginationAndSortingSpec = [:]
    }

    def contributionIds = Sujet.createCriteria().list({
      contributeurs {
        eq 'id', proprietaire.id
      }
    })*.id

    Set<Long> sujetsMasquesIds = listIdsSujetsMasques(proprietaire)

    def criteria = Sujet.createCriteria()
    List<Sujet> sujets = criteria.list(paginationAndSortingSpec) {

      or {
        eq 'proprietaire', proprietaire
        if (!contributionIds.isEmpty()) {
          inList 'id', contributionIds
        }
      }

      if (!sujetsMasquesIds.isEmpty()) {
        not {
          inList 'id', sujetsMasquesIds
        }
      }

      if (paginationAndSortingSpec) {
        def sortArg = paginationAndSortingSpec['sort'] ?: 'lastUpdated'
        def orderArg = paginationAndSortingSpec['order'] ?: 'desc'
        if (sortArg) {
          order "${sortArg}", orderArg
        }

      }
    }
    return sujets
  }

/**
 *
 * @return la liste de tous les types de sujet
 */
  List<SujetType> getAllSujetTypes() {
    return SujetType.getAll()
  }

/**
 * Insère une question dans un sujet sujet
 * Note 1 : dans le cas de l'insertion dans un sujet collaboratif, la question peut être dupliquée
 * Note 2 : si une erreur empêche l'insertion dans le sujet, la méthode retournera null, et les erreurs seront stockées
 * sur le sujet
 * @param question la question
 * @param sujet le sujet
 * @param proprietaire le propriétaire
 * @param rang le rang d'insertion
 * @return la question insérée
 */
  @Transactional
  Question insertQuestionInSujet(Question question,
                                 Sujet sujet,
                                 Personne proprietaire,
                                 ReferentielSujetSequenceQuestions referentielSujetSequenceQuestions = null) {

    // verif securite
    assert (artefactAutorisationService.utilisateurPeutModifierArtefact(proprietaire, sujet))
    assert (artefactAutorisationService.utilisateurPeutReutiliserArtefact(proprietaire, question))
    assert (!insertionQuestionCompositeInExercice(question, sujet))

    if (sujet.estCollaboratif() && question.sujetLieId != sujet.id) {
      if(question.estComposite()) {
        Sujet exercice = createSujetCollaboratifFrom(
            proprietaire,
            question.exercice,
            sujet.contributeurs
        )
        exercice.questionComposite.sujetLie = sujet
        question = exercice.questionComposite
      }
      else {
        question = questionService.createQuestionCollaborativeFrom(
            proprietaire,
            question,
            sujet
        )
      }
    } else if (!sujet.estCollaboratif() && question.estCollaboratif()) {
      if(question.estComposite()) {
        // On duplique l'exercice collaboratif en non collaboratif
        Sujet exercice = recopieSujet(question.exercice, proprietaire)
        question = exercice.questionComposite
      }
      else {
        question = questionService.createQuestionNonCollaborativeFrom(
            proprietaire,
            question
        )
      }
    }

    if (!isDernierAuteur(sujet, proprietaire)) {
      sujet.addPaterniteItem(proprietaire)
    }

    if (!question.estPartage() && sujet.estPartage()) {
      questionService.partageQuestion(question, proprietaire)
    }

    def sequence = new SujetSequenceQuestions(
        question: question,
        sujet: sujet,
        rang: sujet.questionsSequences?.size()
    )
    if (referentielSujetSequenceQuestions?.noteSeuilPoursuite != null) {
      sequence.noteSeuilPoursuite = referentielSujetSequenceQuestions?.noteSeuilPoursuite
    }
    if (referentielSujetSequenceQuestions?.points != null) {
      sequence.points = referentielSujetSequenceQuestions?.points
    }
    sujet.addToQuestionsSequences(sequence)
    sequence.save()
    if (sequence.hasErrors()) {
      sequence.errors.allErrors.each {
        sujet.errors.reject(it.code, it.arguments, it.defaultMessage)
      }
      sujet.removeFromQuestionsSequences(sequence)
      return null
    }

    sujet.lastUpdated = new Date()

    sujet.save(flush: true)
    Integer rang = referentielSujetSequenceQuestions?.rang
    if (rang != null && rang < sujet.questionsSequences.size() - 1) {
      // il faut insérer au rang correct
      def idxSujQuest = sujet.questionsSequences.size() - 1
      while (idxSujQuest != rang) {
        def idxSujQuestPrec = idxSujQuest - 1
        def sujQuest = sujet.questionsSequences[idxSujQuest]
        def sujQuestPrec = sujet.questionsSequences[idxSujQuestPrec]
        sujet.questionsSequences[idxSujQuest] = sujQuestPrec
        sujet.questionsSequences[idxSujQuestPrec] = sujQuest
        idxSujQuest = idxSujQuestPrec
      }
      sujet.save(flush: true)
    }
    sujet.refresh()
    return question
  }

/**
 * Inverse une question avec sa précédente dans un sujet
 * @param sujetQuestion la question à inverser
 * @param proprietaire le proprietaire du sujet
 * @return le sujet modifié
 */
  @Transactional
  Sujet inverseQuestionAvecLaPrecedente(SujetSequenceQuestions sujetQuestion,
                                        Personne proprietaire) {

    Sujet sujet = sujetQuestion.sujet
    // verif securite
    assert (artefactAutorisationService.utilisateurPeutModifierArtefact(proprietaire, sujet))

    if (!isDernierAuteur(sujet, proprietaire)) {
      sujet.addPaterniteItem(proprietaire)
    }

    sujetQuestion.refresh()
    def idx = sujetQuestion.rang
    if (idx == 0) { // on ne fait rien
      return sujetQuestion.sujet
    }
    def idxPrec = sujetQuestion.rang - 1

    def squestPrec = sujet.questionsSequences[idxPrec]
    def squest = sujet.questionsSequences[idx]
    sujet.lastUpdated = new Date()
    sujet.questionsSequences[idx] = squestPrec
    sujet.questionsSequences[idxPrec] = squest
    sujet.save(flush: true)
    // refresh sinon la collection n'est pas raffraichie : raison possible
    // pour suppression modelisation to many
    sujet.refresh()
    return sujet
  }

/**
 * Inverse une question avec sa suivante dans un sujet
 * @param sujetQuestion la question à inverser
 * @param proprietaire le proprietaire du sujet
 * @return le sujet modifié
 */
  @Transactional
  Sujet inverseQuestionAvecLaSuivante(SujetSequenceQuestions sujetQuestion,
                                      Personne proprietaire) {
    Sujet sujet = sujetQuestion.sujet
    // verif securite
    assert (artefactAutorisationService.utilisateurPeutModifierArtefact(proprietaire, sujet))

    if (!isDernierAuteur(sujet, proprietaire)) {
      sujet.addPaterniteItem(proprietaire)
    }

    sujetQuestion.refresh()
    def idx = sujetQuestion.rang
    if (idx == sujetQuestion.sujet.questionsSequences.size() - 1) { // on ne fait rien
      return sujetQuestion.sujet
    }
    def idxSuiv = sujetQuestion.rang + 1
    def squestSuiv = sujet.questionsSequences[idxSuiv]
    def squest = sujet.questionsSequences[idx]
    sujet.questionsSequences[idx] = squestSuiv
    sujet.questionsSequences[idxSuiv] = squest
    sujet.lastUpdated = new Date()
    sujet.save(flush: true)
    sujet.refresh()
    return sujet
  }

/**
 * Supprime une question d'un sujet
 * @param sujetQuestion le sujet et la question
 * @param proprietaire le propriétaire
 * @return le sujet modifié
 */
  @Transactional
  Sujet supprimeQuestionFromSujet(SujetSequenceQuestions sujetQuestion,
                                  Personne proprietaire) {
    // verif securite
    assert (
        artefactAutorisationService.utilisateurPeutModifierArtefact(
            proprietaire,
            sujetQuestion.sujet
        )
    )

    Sujet sujet = sujetQuestion.sujet

    if (!isDernierAuteur(sujet, proprietaire)) {
      sujet.addPaterniteItem(proprietaire)
    }

    sujet.removeFromQuestionsSequences(sujetQuestion)
    def reponses = Reponse.findAllBySujetQuestion(sujetQuestion)
    reponses.each {
      reponseService.supprimeReponse(it, proprietaire)
    }
    def question = sujetQuestion.question
    if (question.estComposite()) {
      def exercice = question.exercice
      def questSujts = []
      questSujts.addAll(exercice.questionsSequences)
      questSujts.each {
        exercice.removeFromQuestionsSequences(it)
        def reponsesEx = Reponse.findAllBySujetQuestion(it)
        reponsesEx.each {
          reponseService.supprimeReponse(it, proprietaire)
        }
      }
    }

    sujetQuestion.delete()
    sujet.lastUpdated = new Date()
    sujet.save(flush: true)
    sujet.refresh()
    return sujet
  }

/**
 * Modifie le nombre de points associé à une question dans un sujet
 * @param sujetQuestion le sujet et la question
 * @param proprietaire le propriétaire
 * @return le sujet modifié
 */
  @Transactional
  SujetSequenceQuestions updatePointsForQuestion(Float newPoints,
                                                 SujetSequenceQuestions sujetQuestion,
                                                 Personne proprietaire) {
    Sujet sujet = sujetQuestion.sujet
    assert (artefactAutorisationService.utilisateurPeutModifierArtefact(proprietaire, sujet))

    if (!isDernierAuteur(sujet, proprietaire)) {
      sujet.addPaterniteItem(proprietaire)
    }

    sujetQuestion.points = newPoints
    if (sujetQuestion.save()) {
      def leSujet = sujetQuestion.sujet
      leSujet.lastUpdated = new Date()
      leSujet.save()
    }
    if (sujetQuestion.hasErrors()) {
      log.error(sujetQuestion.errors.allErrors.toListString())
    }
    return sujetQuestion
  }

  /**
   * Recherche les attachements disponibles dans un sujet
   * @param sujet le sujet
   * @param personne la personne accedant aux attachements
   * @return la liste des attachements disponibles dans le sujet
   */
  Set<Attachement> findAttachementsDisponiblesForSujet(Sujet sujet, Personne personne) {
    assert (artefactAutorisationService.utilisateurPeutReutiliserArtefact(personne, sujet))
    Session session = sessionFactory.currentSession
    def res = [] as Set
    def query1 = session.createSQLQuery("\
        select attach.* from \
        tice.attachement attach, \
        td.question quest,\
        td.sujet_sequence_questions sujetQuest \
        where \
        attach.id = quest.attachement_id and\
        quest.id = sujetQuest.question_id and\
        sujetQuest.sujet_id = ?").addEntity("attach", Attachement.class)

    res.addAll(query1.setLong(0, sujet.id).list())

    def query2 = session.createSQLQuery("\
    select attach.* from \
    tice.attachement attach, \
    td.question_attachement questAttach, \
    td.sujet_sequence_questions sujetQuest \
    where \
    attach.id = questAttach.attachement_id and\
    questAttach.question_id = sujetQuest.question_id and\
    sujetQuest.sujet_id = ?").addEntity("attach", Attachement.class)
    res.addAll(query2.setLong(0, sujet.id).list())

    res

  }

  /**
   * Créé une question composite correspondant à un exercice
   * @param sujet l'exercice
   * @param proprietaire le proprietaire
   * @return la question créée
   */
  @Transactional
  private Question createQuestionCompositeForExercice(Sujet exercice,
                                                      Personne proprietaire) {
    Question question = new Question(
        proprietaire: proprietaire,
        titreNormalise: exercice.titreNormalise,
        titre: exercice.titre,
        publie: false,
        versionQuestion: 1,
        copyrightsType: CopyrightsTypeEnum.TousDroitsReserves.copyrightsType,
        specification: "{}",
        matiereBcn: exercice.matiereBcn,
        etablissement: exercice.etablissement,
        paternite: exercice.paternite,
        niveau: exercice.niveau,
        publication: exercice.publication
    )

    if (exercice.estCollaboratif()) {
      question.collaboratif = true
      exercice.contributeurs.each {
        question.addToContributeurs(it)
      }
    }

    question.type = QuestionTypeEnum.Composite.questionType
    question.exercice = exercice
    question.save(flush: true, failOnError: true)
    return question
  }

  /**
   * Supprime une question composite
   * @param question la question à supprimer
   * @param supprimeur la personne tentant la suppression
   */
  @Transactional
  private def supprimeQuestionComposite(Question laQuestion, Personne supprimeur) {
    assert (laQuestion.estComposite())

    // supression des réponses et des sujetQuestions
    def sujetQuestions = SujetSequenceQuestions.findAllByQuestion(laQuestion)
    sujetQuestions.each {
      supprimeQuestionFromSujet(it, supprimeur)
    }
    // on ne supprime pas les attachements (il n'y en a pas)
    // on ne supprime pas la publication, elle est attachée au sujet si
    // il y en une

    laQuestion.delete()
  }

  /**
   *  Partage une question
   * @param laQuestion la question à partager
   * @param partageur la personne souhaitant partager
   */
  @Transactional
  private def partageQuestionComposite(Question laQuestion) {
    assert (laQuestion.estComposite())
    def exercice = laQuestion.exercice
    laQuestion.copyrightsType = exercice.copyrightsType
    laQuestion.publication = exercice.publication
    laQuestion.publie = true
    laQuestion.paternite = exercice.paternite
    laQuestion.save()
  }

  /**
   * Modifie les proprietes de la question passée en paramètre
   * @param question la question
   * @param proprietes les nouvelles proprietes
   * @param specificationObject l'objet specification
   * @param proprietaire le proprietaire
   * @return la question
   */
  @Transactional
  private Question updateTitreQuestionComposite(String nvtitre, Question laQuestion) {

    assert (laQuestion.estComposite())

    if (nvtitre && laQuestion.titre != nvtitre) {
      laQuestion.titreNormalise = StringUtils.normalise(nvtitre)
      laQuestion.titre = nvtitre
    }

    laQuestion.save(flush: true)
    return laQuestion
  }

  /**
   * Indique si la question à insérer dans le sujet est une question composite
   * à insérer dans un sujet de type  exercice
   * @param question la question à insérer
   * @param sujet le sujet
   * @return true si la question est composite et le sujet est un exercice
   */
  private boolean insertionQuestionCompositeInExercice(Question question, Sujet sujet) {
    if (question.estComposite() && sujet.estUnExercice()) {
      return true
    }
    return false
  }

  public Boolean creeVerrou(Sujet sujet, Personne auteur) {
    assert sujet.estCollaboratif()
    Integer dureeMinute = grailsApplication.config.eliot.verrou.contributeurs.dureeMinute

    Date dateLimitVerrou = new Date()
    use(TimeCategory) {
      dateLimitVerrou = dateLimitVerrou - dureeMinute.minutes
    }

    boolean lockObtained = false
    Sujet.withNewSession {
      Sujet.withNewTransaction {

        lockObtained = Sujet.executeUpdate("""
        UPDATE Sujet s
        SET auteurVerrou=:auteur,
          dateVerrou=:dateVerrou
        WHERE s.id = :sujetId AND (
          s.auteurVerrou IS NULL OR
          s.auteurVerrou=:auteur OR
          s.dateVerrou < :dateLimiteVerrou
        )
      """,
            [
                auteur          : auteur,
                sujetId         : sujet.id,
                dateVerrou      : new Date(),
                dateLimiteVerrou: dateLimitVerrou
            ]
        ) as boolean
      }
    }

    if(lockObtained) {
      // Supprime tous les verrous détenus par cet utilisateurs sur les autres sujets
      Sujet.executeUpdate("""
        UPDATE Sujet s
        SET auteurVerrou=NULL,
          dateVerrou=NULL
        WHERE s.id != :sujetId and s.auteurVerrou=:auteur
      """,
          [sujetId: sujet.id, auteur: auteur]
      )
    }

    sujet.refresh() // On rafraichit le sujet dans la session courante
    return lockObtained
  }

  public void supprimeVerrou(Sujet sujet, Personne auteur) {

    Integer dureeMinute = grailsApplication.config.eliot.verrou.contributeurs.dureeMinute

    Date dateLimitVerrou = new Date()
    use(TimeCategory) {
      dateLimitVerrou = dateLimitVerrou - dureeMinute.minutes
    }

    boolean lockReleased = false
    Sujet.withNewSession {
      Sujet.withNewTransaction {
        lockReleased = Sujet.executeUpdate("""
        UPDATE Sujet s
        SET auteurVerrou=NULL,
          dateVerrou=NULL
        WHERE s.id = :sujetId AND (
          s.auteurVerrou IS NULL OR
          s.auteurVerrou=:auteur OR
          s.dateVerrou < :dateLimiteVerrou
        )
      """,
            [
                auteur          : auteur,
                sujetId         : sujet.id,
                dateLimiteVerrou: dateLimitVerrou
            ]
        ) as boolean
      }
    }

    sujet.refresh()

    if(!lockReleased) {
      log.error(
          "Le verrou n'a pas pu être libéré (" +
              "auteurVerrou: ${sujet.auteurVerrou}, " +
              "dateVerrou: ${sujet.dateVerrou}" +
              ")"
      )
    }


  }

  SujetMasque masque(Personne personne, Sujet sujet) {

    def sujetMasques = SujetMasque.findAllByPersonneAndSujet(personne, sujet)
    SujetMasque sujetMasque = sujetMasques.isEmpty() ? null : sujetMasques[0]

    if (sujetMasque == null) {
      sujetMasque = new SujetMasque(
          sujet: sujet,
          personne: personne
      )
      sujetMasque.save(flush: true, failOnError: true)
    }

    return sujetMasque
  }

  void annuleMasque(Personne personne, Sujet sujet) {
    def sujetMasques = SujetMasque.findAllByPersonneAndSujet(personne, sujet)

    sujetMasques.each { SujetMasque sujetMasque ->
      sujetMasque.delete(flush: true)
    }
  }

  @Transactional
  public void finalise(Sujet sujet, Date lastUpdated, Personne auteur) {
    Integer success = Sujet.executeUpdate("""
      UPDATE Sujet s
      SET termine = true
      WHERE s.id = :sujetId
        AND s.lastUpdated = :lastUpdated
        AND s.proprietaire = :proprietaire
    """,
    [
        sujetId: sujet.id,
        lastUpdated: lastUpdated,
        proprietaire: auteur
    ])

    if(success) { // Si le sujet à pu être marqué comme terminé, on fait de même pour toutes les questions du sujet
      Question.executeUpdate("""
        UPDATE Question q
        SET termine = true
        WHERE q.sujetLie = :sujet
      """,
          [
              sujet: sujet
          ]
      )
      sujet.refresh()

    }
  }

  @Transactional
  public void annuleFinalise(Sujet sujet, Personne auteur) {
    if (sujet.termine && sujet.proprietaireId == auteur.id && !sujet.estDistribue()) {
      sujet.termine = false;
      sujet.save(flush: true, failOnError: true)

      Question.executeUpdate("""
        UPDATE Question q
        SET termine = false
        WHERE q.sujetLie = :sujet
      """,
          [
              sujet: sujet
          ]
      )
    }
  }

  public void actualiseLastUpdate(Sujet sujet) {
    Sujet.executeUpdate("""
      UPDATE Sujet s
      SET lastUpdated = :lastUpdated
      WHERE s.id = :sujetId
    """,
        [
            sujetId: sujet.id,
            lastUpdated: new Date()
        ])
  }
}