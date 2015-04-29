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

import org.lilie.services.eliot.competence.Competence
import org.lilie.services.eliot.tice.Attachement
import org.lilie.services.eliot.tice.CopyrightsType
import org.lilie.services.eliot.tice.CopyrightsTypeEnum
import org.lilie.services.eliot.tice.Publication
import org.lilie.services.eliot.tice.annuaire.Personne
import org.lilie.services.eliot.tice.utils.StringUtils
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.transaction.annotation.Transactional

import static org.lilie.services.eliot.tdbase.QuestionTypeEnum.*

/**
 * Service de gestion des questions
 * @author franck silvestre
 */
class QuestionService implements ApplicationContextAware {

    static transactional = false
    ApplicationContext applicationContext

    SujetService sujetService
    QuestionAttachementService questionAttachementService
    QuestionCompetenceService questionCompetenceService
    ArtefactAutorisationService artefactAutorisationService

    /**
     * Récupère le service de gestion de spécification de question correspondant
     * au type de question passé en paramètre
     * @param questionType le type de question
     * @return le service ad-hoc pour le type de question
     */
    QuestionSpecificationService questionSpecificationServiceForQuestionType(QuestionType questionType) {
        return (QuestionSpecificationService) applicationContext.getBean("question${questionType.code}SpecificationService")
    }

    /**
     * Créé une question
     * @param proprietes les propriétés hors specification
     * @param specificationObject l'objet specification
     * @param proprietaire le proprietaire
     * @return la question créée
     */
    @Transactional
    Question createQuestion(Map proprietes, def specificationObject, Personne proprietaire) {
        Question question = new Question(proprietaire: proprietaire,
                titreNormalise: StringUtils.normalise(proprietes.titre),
                publie: false,
                versionQuestion: 1,
                copyrightsType: CopyrightsTypeEnum.TousDroitsReserves.copyrightsType,
                specification: "{}"
        )

        question.properties = proprietes
        question.principalAttachementFichier = proprietes.principalAttachementFichier

        // mise à jour attachement
        if (question.principalAttachementId) {
            def attachement = Attachement.get(question.principalAttachementId)
            questionAttachementService.createPrincipalAttachementForQuestion(attachement, question)
        } else if (question.principalAttachementFichier && !question.principalAttachementFichier.isEmpty()) {
            questionAttachementService.createPrincipalAttachementForQuestionFromMultipartFile(question.principalAttachementFichier, question)
        }

        // mise à jour spécification
        updateQuestionSpecificationForObject(question, specificationObject)

        // La paternité n'est initialisée que si elle n'est pas fournie dans les propriétés de création
        if (!proprietes.containsKey('paternite')) {
            addPaterniteItem(proprietaire, question)
        }

        List<Competence> competenceList = parseCompetenceListFromParams(proprietes)
        questionCompetenceService.updateQuestionCompetenceList(question, competenceList)

        question.save(flush: true)
        return question
    }

    /**
     * Crée une question collaborative (pour un sujet collaboratif) à partir d'une
     * question qui est, soit non-collaborative, soit collaborative mais liée à un
     * sujet différent
     * @param personne l'utilisateur qui crée la nouvelle question collaborative
     * @param questionSource la question à recopier
     * @param sujet le sujet collaboratif pour lequel la question collaborative est créée
     * @return la question créée
     */
    @Transactional
    Question createQuestionCollaborativeFrom(Personne personne,
                                             Question questionSource,
                                             Sujet sujet) {
        assert sujet.estCollaboratif()
        assert artefactAutorisationService.utilisateurPeutModifierArtefact(
                personne,
                sujet
        )
        assert artefactAutorisationService.utilisateurPeutDupliquerArtefact(
                personne,
                questionSource
        )
        assert (questionSource.sujetLie?.id != sujet.id)

        // Recopie de la question
        Question questionCollaborative = recopieQuestion(
                questionSource,
                questionSource.proprietaire, // Lorsqu'on duplique une question pour la rendre collaborative, on laisse inchangé le propriétaire original
                questionSource.titre
        )

        // Rend la question collaborative pour le sujet
        questionCollaborative.collaboratif = true
        questionCollaborative.contributeurs = sujet.contributeurs

        questionCollaborative.save()

        // TODO *** Cas de la question composite : il faut rendre l'exercice collaboratif

        return questionCollaborative
    }

    void updateQuestionSpecificationForObject(Question question, QuestionSpecification specificationObject) {
        def specService = questionSpecificationServiceForQuestionType(question.type)
        specService.updateQuestionSpecificationForObject(question, specificationObject)
    }

    /**
     * Recopie une question dans un sujet
     * @param sujetQuestion la question à recopier et son sujet associé
     * @param proprietaire le proprietaire
     * @return la copie de la question
     */
    @Transactional
    Question recopieQuestionDansSujet(SujetSequenceQuestions sujetQuestion, Personne proprietaire) {

        def question = sujetQuestion.question
        def sujet = sujetQuestion.sujet

        assert (artefactAutorisationService.utilisateurPeutDupliquerArtefact(proprietaire, question))
        assert (artefactAutorisationService.utilisateurPeutModifierArtefact(proprietaire, sujet))

        Question questionCopie = recopieQuestion(question, proprietaire)

        if (!sujetService.isDernierAuteur(sujet, proprietaire)) {
            sujetService.addPaterniteItem(proprietaire, sujet)
        }

        sujetQuestion.question = questionCopie
        sujetQuestion.save()

        return questionCopie
    }

    /**
     * Recopie une question
     * @param question la question à recopier
     * @param proprietaire le proprietaire
     * @param nouveauTitre le titre la copie, si null la valeur par défaut sera
     * le titre de l'original suivi du suffixe " (Copie)"
     * @return la copie de la question
     */
    @Transactional
    Question recopieQuestion(Question question,
                             Personne proprietaire,
                             String nouveauTitre = null) {

        assert (
                artefactAutorisationService.utilisateurPeutDupliquerArtefact(
                        proprietaire,
                        question
                )
        )

        if(!nouveauTitre) {
            nouveauTitre = question.titre + " (Copie)"
        }

        Question questionCopie = new Question(
                proprietaire: proprietaire,
                titre: nouveauTitre,
                titreNormalise: StringUtils.normalise(nouveauTitre),
                specification: question.specification,
                specificationNormalise: question.specificationNormalise,
                publie: false,
                copyrightsType: CopyrightsTypeEnum.TousDroitsReserves.copyrightsType,
                estAutonome: question.estAutonome,
                type: question.type,
                matiereBcn: question.matiereBcn,
                niveau: question.niveau,
                principalAttachement: question.principalAttachement,
                paternite: question.paternite
        )
        questionCopie.save()

        // recopie les attachements (on ne duplique pas les attachements)
        QuestionSpecification questionSpecificationCopie = questionCopie.specificationObject
        question.questionAttachements.each { QuestionAttachement questionAttachement ->
            QuestionAttachement questionAttachementCopie =
                    recopieQuestionAttachement(questionCopie, questionAttachement)

            questionSpecificationCopie.remplaceQuestionAttachementId(
                    questionAttachement.id,
                    questionAttachementCopie.id
            )
        }
        updateQuestionSpecificationForObject(questionCopie, questionSpecificationCopie)

        // Recopie les QuestionCompetence
        question.allQuestionCompetence.each { QuestionCompetence questionCompetence ->
            recopieQuestionCompetence(questionCopie, questionCompetence)
        }

        questionCopie.save()
        return questionCopie
    }

    QuestionAttachement recopieQuestionAttachement(Question questionCible, QuestionAttachement questionAttachement) {
        QuestionAttachement copieQuestionAttachement = new QuestionAttachement(
                question: questionCible,
                attachement: questionAttachement.attachement
        )
        questionCible.addToQuestionAttachements(copieQuestionAttachement)
        questionCible.save()
        copieQuestionAttachement.save() // Sans ce save, l'id n'est pas généré à ce stade (étrange)

        return copieQuestionAttachement
    }

    private QuestionCompetence recopieQuestionCompetence(Question questionCible,
                                                         QuestionCompetence questionCompetence) {
        QuestionCompetence copieQuestionCompetence = new QuestionCompetence(
                question: questionCible,
                competence: questionCompetence.competence
        )
        questionCible.addToAllQuestionCompetence(copieQuestionCompetence)
        questionCible.save()
        copieQuestionCompetence.save()

        return copieQuestionCompetence
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
    Question updateProprietes(Question question,
                              Map proprietes,
                              def specificationObject,
                              Personne proprietaire) {

        assert (artefactAutorisationService.utilisateurPeutModifierArtefact(proprietaire, question))

        if (!isDernierAuteur(question, proprietaire)) {
            addPaterniteItem(proprietaire, question)
        }

        if (proprietes.titre && question.titre != proprietes.titre) {
            question.titreNormalise = StringUtils.normalise(proprietes.titre)
        }

        question.properties = proprietes
        question.principalAttachementFichier = proprietes.principalAttachementFichier
        // mise à jour de l'attachement
        if (question.principalAttachementId) {
            if (question.principalAttachementId != question.principalAttachement?.id) {
                if (question.principalAttachement) {
                    questionAttachementService.deletePrincipalAttachementForQuestion(question)
                }
                def attachement = Attachement.get(question.principalAttachementId)
                questionAttachementService.createPrincipalAttachementForQuestion(attachement, question)
            }
        } else if (question.principalAttachementFichier) {
            if (question.principalAttachement) {
                questionAttachementService.deletePrincipalAttachementForQuestion(question)
            }
            if (!question.principalAttachementFichier.isEmpty()) {
                questionAttachementService.createPrincipalAttachementForQuestionFromMultipartFile(question.principalAttachementFichier,
                        question)
            }
        }

        // mise à jour de la spécification
        def specService = questionSpecificationServiceForQuestionType(question.type)
        specService.updateQuestionSpecificationForObject(question, specificationObject)

        // Supprime les attachements qui ne sont plus utilisés
        Set<Long> attachementIdExistantSet = question.questionAttachements*.id as Set
        Set<Long> attachementIdUtiliseSet = specificationObject.allQuestionAttachementId as Set
        Set<Long> attachementIdASupprimerSet = attachementIdExistantSet - attachementIdUtiliseSet
        attachementIdASupprimerSet.each {
            questionAttachementService.deleteQuestionAttachement(QuestionAttachement.load(it))
        }

        List<Competence> competenceList = parseCompetenceListFromParams(proprietes)
        questionCompetenceService.updateQuestionCompetenceList(question, competenceList)

        question.save(flush: true)
        return question
    }

/**
 * Créé une question et l'insert dans le sujet
 * @param proprietesQuestion les propriétés de la question
 * @param specificationObject l'objet specification
 * @param sujet le sujet
 * @param proprietaire le propriétaire
 * @param rang le rang d'insertion
 * @return la question insérée
 */
    @Transactional
    Question createQuestionAndInsertInSujet(Map proprietesQuestion,
                                            def specificationObject,
                                            Sujet sujet,
                                            Personne proprietaire,
                                            ReferentielSujetSequenceQuestions referentielSujetSequenceQuestions = null) {

        assert (artefactAutorisationService.utilisateurPeutModifierArtefact(proprietaire, sujet))

        Question question = createQuestion(proprietesQuestion, specificationObject, proprietaire)
        if (!question.hasErrors()) {
            sujetService.insertQuestionInSujet(
                    question,
                    sujet,
                    proprietaire,
                    referentielSujetSequenceQuestions
            )
        }
        return question
    }

    /**
     * Supprime une question
     * @param question la question à supprimer
     * @param supprimeur la personne tentant la suppression
     */
    @Transactional
    def supprimeQuestion(Question laQuestion, Personne supprimeur) {
        assert (artefactAutorisationService.utilisateurPeutSupprimerArtefact(supprimeur, laQuestion))

        // supression des réponses et des sujetQuestions
        def sujetQuestions = SujetSequenceQuestions.findAllByQuestion(laQuestion)
        sujetQuestions.each {
            sujetService.supprimeQuestionFromSujet(it, supprimeur)
        }

        // supprimer les attachements si nécessaire
        def questionAttachements = QuestionAttachement.findAllByQuestion(laQuestion)
        questionAttachements.each {
            questionAttachementService.deleteQuestionAttachement(it)
        }

        // Supprime les compétences liées si nécessaire
        if (laQuestion.allQuestionCompetence) {
            new ArrayList<QuestionCompetence>(laQuestion.allQuestionCompetence).each {
                questionCompetenceService.deleteQuestionCompetence(it)
            }
        }

        // supprimer la publication si nécessaire
        if (laQuestion.estPartage()) {
            laQuestion.publication.delete()
        }

        laQuestion.delete()
    }

    /**
     *  Partage une question
     * @param question la question à partager
     * @param partageur la personne souhaitant partager
     */
    @Transactional
    def partageQuestion(Question question, Personne partageur) {
        assert (artefactAutorisationService.utilisateurPeutPartageArtefact(partageur, question))
        CopyrightsType ct = CopyrightsTypeEnum.CC_BY_NC.copyrightsType
        Publication publication = new Publication(
                dateDebut: new Date(),
                copyrightsType: ct)
        publication.save()
        question.copyrightsType = ct
        question.publication = publication
        question.publie = true

        // mise à jour de la paternite
        addPaterniteItem(partageur, question, publication.dateDebut)
    }

    private void addPaterniteItem(Personne partageur,
                                  Question question,
                                  Date datePublication = null) {
        CopyrightsType ct = question.copyrightsType

        PaterniteItem paterniteItem = new PaterniteItem(auteur: "${partageur.nomAffichage}",
                copyrightDescription: "${ct.presentation}",
                copyrighLien: "${ct.lien}",
                logoLien: ct.logo,
                datePublication: datePublication,
                oeuvreEnCours: true)
        Paternite paternite = new Paternite(question.paternite)
        paternite.paterniteItems.each {
            it.oeuvreEnCours = false
        }
        paternite.addPaterniteItem(paterniteItem)
        question.paternite = paternite.toString()
        question.save()
    }

    /**
     * Teste si un utilisateur est le dernier auteur d'un sujet (i.e. le dernier à
     * avoir modifié le sujet)
     * @param sujet
     * @param utilisateur
     * @return
     */
    private boolean isDernierAuteur(Question question, Personne utilisateur) {
        Paternite paternite = new Paternite(question.paternite)

        if (!paternite.paterniteItems) {
            return false
        }

        return paternite.paterniteItems.last()?.auteur == utilisateur.nomAffichage
    }

    /**
     * Recherche de questions
     * @param chercheur la personne effectuant la recherche
     * @param patternTitre le pattern saisi pour le titre
     * @param patternAuteur le pattern saisi pour l'auteur
     * @param patternPresentation le pattern saisi pour la presentation
     * @param matiereBcn la matiere
     * @param niveau le niveau
     * @param paginationAndSortingSpec les specifications pour l'ordre et
     * la pagination
     * @param uniquementQuestionsChercheur flag indiquant si on recherche que
     * les items du chercheur
     * @return la liste des questions
     */
    List<Question> findQuestions(Personne chercheur,
                                 String patternTitre,
                                 String patternAuteur,
                                 String patternSpecification,
                                 ReferentielEliot referentielEliot,
                                 QuestionType questionType,
                                 Boolean exclusComposites = false,
                                 Boolean uniquementQuestionsChercheur = false,
                                 Map paginationAndSortingSpec = null) {
        if (!chercheur) {
            throw new IllegalArgumentException("question.recherche.chercheur.null")
        }
        if (paginationAndSortingSpec == null) {
            paginationAndSortingSpec = [:]
        }

        def criteria = Question.createCriteria()
        List<Question> questions = criteria.list(paginationAndSortingSpec) {
            if (referentielEliot?.matiereBcn) {
                eq "matiereBcn", referentielEliot?.matiereBcn
            }
            if (referentielEliot?.niveau) {
                eq "niveau", referentielEliot?.niveau
            }
            if (questionType) {
                eq "type", questionType
            } else if (exclusComposites) {
                ne "type", QuestionTypeEnum.Composite.questionType
            }
            if (uniquementQuestionsChercheur) {
                eq 'proprietaire', chercheur
            } else {
                or {
                    eq 'proprietaire', chercheur
                    eq 'publie', true
                }
                if (patternAuteur) {
                    String patternAuteurNormalise = "%${StringUtils.normalise(patternAuteur)}%"
                    proprietaire {
                        or {
                            like "nomNormalise", patternAuteurNormalise
                            like "prenomNormalise", patternAuteurNormalise
                        }
                    }
                }
            }

            if (patternTitre) {
                like "titreNormalise", "%${StringUtils.normalise(patternTitre)}%"
            }
            if (patternSpecification) {
                like "specificationNormalise", "%${StringUtils.normalise(patternSpecification)}%"
            }


            if (paginationAndSortingSpec) {
                def sortArg = paginationAndSortingSpec['sort'] ?: 'lastUpdated'
                def orderArg = paginationAndSortingSpec['order'] ?: 'desc'
                if (sortArg) {
                    order "${sortArg}", orderArg
                }

            }
        }
        return questions
    }

    /**
     * Recherche de questions d'un proprietaire donné
     * @param proprietaire la personne effectuant la recherche
     * @param paginationAndSortingSpec les specifications pour l'ordre et
     * la pagination
     * @return la liste des questions
     */
    List<Question> findQuestionsForProprietaire(Personne proprietaire,
                                                Map paginationAndSortingSpec = null) {
        if (!proprietaire) {
            throw new IllegalArgumentException("question.recherche.chercheur.null")
        }
        if (paginationAndSortingSpec == null) {
            paginationAndSortingSpec = [:]
        }

        def criteria = Question.createCriteria()
        List<Question> questions = criteria.list(paginationAndSortingSpec) {
            eq 'proprietaire', proprietaire
            if (paginationAndSortingSpec) {
                def sortArg = paginationAndSortingSpec['sort'] ?: 'lastUpdated'
                def orderArg = paginationAndSortingSpec['order'] ?: 'desc'
                if (sortArg) {
                    order "${sortArg}", orderArg
                }

            }
        }
        return questions
    }

    /**
     *
     * @return la liste de tous les types de question
     */
    List<QuestionType> getAllQuestionTypes() {
        return QuestionType.getAll()
    }

    /**
     *
     * @return la liste des types de questions à interaction supportes par tdbase
     */
    List<QuestionType> getTypesQuestionsInteractionSupportes() {
        [MultipleChoice.questionType,
         ExclusiveChoice.questionType,
         QuestionTypeEnum.Integer.questionType,
         Decimal.questionType,
         Slider.questionType,
         FillGap.questionType,
         Associate.questionType,
         Order.questionType,
         FillGraphics.questionType,
         GraphicMatch.questionType,
         Open.questionType,
         FileUpload.questionType,
         BooleanMatch.questionType,
         Composite.questionType]
    }

    /**
     *
     * @return la liste des types de questions à interaction supportes par tdbase
     */
    List<QuestionType> getTypesQuestionsSupportes() {
        typesQuestionsInteractionSupportes +
                [Document.questionType,
                 Statement.questionType,]
    }

    /**
     *
     * @return la liste des types de questions à interaction supportes par tdbase
     * hors composite
     */
    List<QuestionType> getTypesQuestionsInteractionSupportesPourCreation() {
        typesQuestionsInteractionSupportes - Composite.questionType
    }

    /**
     *
     * @return la liste des types de questions  supportes par tdbase  hors
     * composite
     */
    List<QuestionType> getTypesQuestionsSupportesHorsComposite() {
        typesQuestionsSupportes - Composite.questionType
    }

    private List<Competence> parseCompetenceListFromParams(Map params) {
        if (!params.competenceAssocieeIdList) {
            return []
        }

        // Grails injecte une valeur atomique ou un tableau selon si le paramètre est multi-valué ou non
        List competenceAssocieeIdList = params.competenceAssocieeIdList instanceof String[] ?
                params.competenceAssocieeIdList :
                [params.competenceAssocieeIdList]

        List competenceList = Competence.getAll(
                competenceAssocieeIdList.collect {
                    Long.parseLong(it)
                }
        )

        // Garanti que tous les identifiants correspondent bien à une compétence en base
        competenceList.each { assert it != null }

        return competenceList
    }
}