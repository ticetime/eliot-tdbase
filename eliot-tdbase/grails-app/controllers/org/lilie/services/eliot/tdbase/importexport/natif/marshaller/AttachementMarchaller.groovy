package org.lilie.services.eliot.tdbase.importexport.natif.marshaller

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject
import org.lilie.services.eliot.tdbase.QuestionAttachement
import org.lilie.services.eliot.tdbase.importexport.dto.AttachementDto
import org.lilie.services.eliot.tdbase.importexport.dto.PrincipalAttachementDto
import org.lilie.services.eliot.tice.Attachement
import org.lilie.services.eliot.tice.AttachementService

/**
 * Marshaller qui permet de convertir des Attachement et des QuestionsAttachements dans une représentation
 * à base de List et de Map qui pourra ensuite être convertie en JSON ou en XML
 *
 * @author John Tranier
 */
class AttachementMarchaller {

  AttachementService attachementService

  @SuppressWarnings('ReturnsNullInsteadOfEmptyCollection')
  Map marshallPrincipalAttachement(Attachement principalAttachement, estInsereDansLaQuestion) {
    if(!principalAttachement) {
      return null
    }

    return [
        attachement: marshallAttachement(principalAttachement, estInsereDansLaQuestion)
    ]
  }

  List marshallQuestionAttachements(SortedSet<QuestionAttachement> questionAttachements) {
    if(!questionAttachements) {
      return []
    }

    return questionAttachements.collect {
      marshallAttachement(it.attachement, it.estInsereDansLaQuestion)
    }
  }

  private Map marshallAttachement(Attachement attachement, Boolean estInsereDansLaQuestion) {
    return [
        nom: attachement.nom,
        nomFichierOriginal: attachement.nomFichierOriginal,
        typeMime: attachement.typeMime,
        blob: attachementService.encodeToBase64(attachement),
        estInsereDansLaQuestion: estInsereDansLaQuestion
    ]
  }

  static PrincipalAttachementDto parsePrincipalAttachement(JSONElement jsonElement) {
    MarshallerHelper.checkIsJsonElement('principalAttachement.attachement', jsonElement.attachement)
    return new PrincipalAttachementDto(
        attachement: parseAttachement(jsonElement.attachement)
    )
  }

  static parsePrincipalAttachement(JSONObject.Null jsonElement) {
    return null
  }

  static List<AttachementDto> parseQuestionAttachements(JSONArray jsonArray) {
    jsonArray.collect {
      parseAttachement((JSONElement)it)
    }

  }

  static AttachementDto parseAttachement(JSONElement jsonElement) {
    MarshallerHelper.checkIsNotNull('principalAttachement.attachement.nom', jsonElement.nom)
    MarshallerHelper.checkIsNotNull('principalAttachement.attachement.nomFichierOriginal', jsonElement.nomFichierOriginal)
    MarshallerHelper.checkIsNotNull('principalAttachement.attachement.typeMime', jsonElement.typeMime)
    MarshallerHelper.checkIsNotNull('principalAttachement.attachement.blob', jsonElement.blob)

    return new AttachementDto(
        nom: jsonElement.nom,
        nomFichierOriginal: jsonElement.nomFichierOriginal,
        typeMime: jsonElement.typeMime,
        blob: jsonElement.blob,
        estInsereDansLaQuestion: jsonElement.estInsereDansLaQuestion
    )
  }
}


