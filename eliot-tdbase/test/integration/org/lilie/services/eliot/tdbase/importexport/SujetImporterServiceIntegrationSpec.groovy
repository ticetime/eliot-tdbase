package org.lilie.services.eliot.tdbase.importexport

import grails.converters.JSON
import grails.plugin.spock.IntegrationSpec
import org.lilie.services.eliot.tdbase.QuestionService
import org.lilie.services.eliot.tdbase.QuestionTypeEnum
import org.lilie.services.eliot.tdbase.ReferentielEliot
import org.lilie.services.eliot.tdbase.ReferentielSujetSequenceQuestions
import org.lilie.services.eliot.tdbase.Sujet
import org.lilie.services.eliot.tdbase.SujetSequenceQuestions
import org.lilie.services.eliot.tdbase.SujetService
import org.lilie.services.eliot.tdbase.impl.open.OpenSpecification
import org.lilie.services.eliot.tdbase.importexport.dto.SujetDto
import org.lilie.services.eliot.tdbase.importexport.natif.marshaller.SujetMarshaller
import org.lilie.services.eliot.tdbase.importexport.natif.marshaller.factory.SujetMarshallerFactory
import org.lilie.services.eliot.tdbase.utils.TdBaseInitialisationTestService
import org.lilie.services.eliot.tice.AttachementService
import org.lilie.services.eliot.tice.CopyrightsTypeEnum
import org.lilie.services.eliot.tice.annuaire.Personne
import org.lilie.services.eliot.tice.scolarite.Matiere
import org.lilie.services.eliot.tice.scolarite.Niveau
import org.lilie.services.eliot.tice.utils.BootstrapService

/**
 * @author John Tranier
 */
class SujetImporterServiceIntegrationSpec extends IntegrationSpec {

  TdBaseInitialisationTestService tdBaseInitialisationTestService
  SujetService sujetService
  QuestionService questionService
  AttachementService attachementService
  SujetImporterService sujetImporterService
  BootstrapService bootstrapService

  Personne personne

  def setup() {
    personne = tdBaseInitialisationTestService.utilisateur1.personne
    bootstrapService.bootstrapForIntegrationTest()

    assert Matiere.first()
    assert Niveau.first()
  }

  void "testImporteSujet - Sujet OK"(ReferentielEliot referentielEliot, int nbQuestion) {
    given:
    Sujet sujet = creeSujet(referentielEliot, nbQuestion)
    assert sujet.id

    String sujetExporte = exporteSujet(sujet)
    assert sujetExporte

    SujetDto sujetDto = SujetMarshaller.parse(
        JSON.parse(sujetExporte)
    )

    when:
    Sujet sujetImporte = sujetImporterService.importeSujet(
        sujetDto,
        personne,
        referentielEliot
    )

    then:
    sujetImporte.titre == sujet.titre
    sujetImporte.titreNormalise == sujet.titreNormalise
    sujetImporte.presentation == sujet.presentation
    sujetImporte.presentationNormalise == sujet.presentationNormalise
    sujetImporte.nbQuestions == sujet.nbQuestions
    sujetImporte.dureeMinutes == sujet.dureeMinutes
    sujetImporte.noteMax == sujet.noteMax
    sujetImporte.noteAutoMax == sujet.noteAutoMax
    sujetImporte.noteEnseignantMax == sujet.noteEnseignantMax
    sujetImporte.accesSequentiel == sujet.accesSequentiel
    sujetImporte.ordreQuestionsAleatoire == sujet.ordreQuestionsAleatoire
    JSON.parse(sujetImporte.paternite) == JSON.parse(sujet.paternite)
    sujetImporte.copyrightsType == sujet.copyrightsType
    !sujetImporte.publie
    !sujetImporte.publication
    !sujetImporte.accesPublic

    if (nbQuestion > 0) {
      sujetImporte.questionsSequences.size() == nbQuestion
      sujetImporte.questionsSequences.eachWithIndex {
        SujetSequenceQuestions sujetSequenceQuestions, int i ->

          assert sujetSequenceQuestions.rang == sujet.questionsSequences[i].rang
          assert sujetSequenceQuestions.noteSeuilPoursuite == sujet.questionsSequences[i].noteSeuilPoursuite
          assert sujetSequenceQuestions.points == sujet.questionsSequences[i].points
          assert sujetSequenceQuestions.question.titre == sujet.questionsSequences[i].question.titre
      }
    }
    else {
      !sujetImporte.questionsSequences
    }

    where:
    referentielEliot << [
        null,
        null,
        new ReferentielEliot(
            matiere: Matiere.first(),
            niveau: Niveau.first()
        )
    ]
    nbQuestion << [0, 1, 3]
  }

  private Sujet creeSujet(ReferentielEliot referentielEliot, int nbQuestion) {
    Sujet sujet = sujetService.createSujet(personne, "Un Sujet")
    sujetService.updateProprietes(
        sujet,
        [
            titre: 'titre',
            versionSujet: 3,
            presentation: 'presentation',
            annotationPrivee: 'annotationPrivee',
            dureeMinutes: 30,
            noteMax: 20.0,
            noteAutoMax: 12.0,
            noteEnseignantMax: 8.0,
            accesSequentiel: true,
            ordreQuestionAleatoire: false,
            paternite: '{json: paternite}',
            copyrightsType: CopyrightsTypeEnum.CC_BY_NC.copyrightsType,
            matiere: referentielEliot?.matiere,
            niveau: referentielEliot?.niveau
        ],
        personne
    )

    nbQuestion.times {
      questionService.createQuestionAndInsertInSujet(
          [
              titre: "question-$it",
              type: QuestionTypeEnum.Open.questionType
          ],
          new OpenSpecification(
              libelle: 'libelle',
              nombreLignesReponse: 5
          ),
          sujet,
          personne,
          new ReferentielSujetSequenceQuestions(
              rang: it,
              noteSeuilPoursuite: 10.0 * it as Float,
              points: 20 * it
          )
      )
    }

    return sujet
  }

  private String exporteSujet(Sujet sujet) {
    SujetMarshallerFactory sujetMarshallerFactory = new SujetMarshallerFactory()
    SujetMarshaller sujetMarshaller = sujetMarshallerFactory.newInstance(attachementService)
    def convert = sujetMarshaller.marshall(sujet) as JSON
    return convert.toString(false)
  }
}
