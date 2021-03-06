package org.lilie.services.eliot.tdbase.importexport.natif.marshaller

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONElement
import org.lilie.services.eliot.tdbase.Question
import org.lilie.services.eliot.tdbase.importexport.dto.QuestionAtomiqueDto
import org.lilie.services.eliot.tdbase.importexport.dto.QuestionDto

/**
 * Marshaller qui permet de convertir une question en une représentation à base de Map
 * qui pourra ensuite être convertie en XML ou en JSON
 *
 * @author John Tranier
 */
public class QuestionMarshaller {

  PersonneMarshaller personneMarshaller
  EtablissementMarshaller etablissementMarshaller
  MatiereBcnMarshaller matiereBcnMarshaller
  NiveauMarshaller niveauMarshaller
  CopyrightsTypeMarshaller copyrightsTypeMarshaller
  AttachementMarchaller attachementMarchaller
  QuestionCompositeMarshaller questionCompositeMarshaller

  Map marshall(Question question, AttachementDataStore attachementDataStore) {
    if (!question) {
      throw new IllegalArgumentException("La question ne peut pas être null")
    }

    if(question.exercice) { // Question composite
      return questionCompositeMarshaller.marshall(question, attachementDataStore)
    }

    Map representation = [
        class: ExportClass.QUESTION_ATOMIQUE.name(),
        type: question.type.code,
        titre: question.titre,
        id: question.id.toString(),
        metadonnees: [
            proprietaire: personneMarshaller.marshall(question.proprietaire),
            dateCreated: question.dateCreated,
            lastUpdated: question.lastUpdated,
            versionQuestion: question.versionQuestion,
            estAutonome: question.estAutonome,
            paternite: question.paternite ? JSON.parse(question.paternite) : null,
            copyrightsType: copyrightsTypeMarshaller.marshall(question.copyrightsType),
            referentielEliot: [
                etablissement: etablissementMarshaller.marshall(question.etablissement),
                matiereBcn: matiereBcnMarshaller.marshall(question.matiereBcn),
                niveau: niveauMarshaller.marshall(question.niveau)
            ]
        ],
        specification: JSON.parse(question.specification),
        principalAttachement: attachementMarchaller.marshallPrincipalAttachement(
            question.principalAttachement,
            question.principalAttachementEstInsereDansLaQuestion,
            attachementDataStore
        ),
        questionAttachements: attachementMarchaller.marshallQuestionAttachements(
            question.questionAttachements,
            attachementDataStore
        )
    ]

    return representation
  }

  static QuestionDto parse(JSONElement jsonElement,
                           AttachementDataStore attachementDataStore) {
    MarshallerHelper.checkClassIn(
        [
            ExportClass.QUESTION_ATOMIQUE,
            ExportClass.QUESTION_COMPOSITE
        ],
        jsonElement
    )

    if(jsonElement.class == ExportClass.QUESTION_COMPOSITE.name()) {
      return QuestionCompositeMarshaller.parse(jsonElement, attachementDataStore)
    }

    MarshallerHelper.checkIsNotNull('type', jsonElement.type)
    MarshallerHelper.checkIsNotNull('titre', jsonElement.titre)
    MarshallerHelper.checkIsJsonElement('metadonnees', jsonElement.metadonnees)
    MarshallerHelper.checkIsJsonElement('metadonnees.proprietaire', jsonElement.metadonnees.proprietaire)
    MarshallerHelper.checkIsJsonElement('metadonnees.copyrightsType', jsonElement.metadonnees.copyrightsType)
    MarshallerHelper.checkIsNotNull('specification', jsonElement.specification)
    MarshallerHelper.checkIsNullableJsonElement('principalAttachement', jsonElement.principalAttachement)
    MarshallerHelper.checkIsJsonArray('questionAttachements', jsonElement.questionAttachements)

    return new QuestionAtomiqueDto(
        type: jsonElement.type,
        titre: jsonElement.titre,
        proprietaire: PersonneMarshaller.parse(jsonElement.metadonnees.proprietaire),
        dateCreated: MarshallerHelper.parseDate(jsonElement.metadonnees.dateCreated),
        lastUpdated: MarshallerHelper.parseDate(jsonElement.metadonnees.lastUpdated),
        versionQuestion: jsonElement.metadonnees.versionQuestion,
        estAutonome: jsonElement.metadonnees.estAutonome,
        paternite: MarshallerHelper.jsonObjectToString(jsonElement.metadonnees.paternite),
        copyrightsType: CopyrightsTypeMarshaller.parse(jsonElement.metadonnees.copyrightsType),
        etablissement: jsonElement.metadonnees.referentielEliot ?
          EtablissementMarshaller.parse(jsonElement.metadonnees.referentielEliot.etablissement) :
          null,
        matiereBcn: jsonElement.metadonnees.referentielEliot ?
          MatiereBcnMarshaller.parse(jsonElement.metadonnees.referentielEliot.matiereBcn) :
          null,
        niveau: jsonElement.metadonnees.referentielEliot ?
          NiveauMarshaller.parse(jsonElement.metadonnees.referentielEliot.niveau) :
          null,
        specification: jsonElement.specification,
        principalAttachement: AttachementMarchaller.parsePrincipalAttachement(
            jsonElement.principalAttachement,
            attachementDataStore
        ),
        questionAttachements: AttachementMarchaller.parseAllQuestionAttachement(
            jsonElement.questionAttachements,
            attachementDataStore
        )
    )
  }
}

