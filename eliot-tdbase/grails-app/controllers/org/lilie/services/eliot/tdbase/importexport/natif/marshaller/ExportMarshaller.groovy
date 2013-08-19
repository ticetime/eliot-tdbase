package org.lilie.services.eliot.tdbase.importexport.natif.marshaller

import org.codehaus.groovy.grails.web.json.JSONElement
import org.lilie.services.eliot.tdbase.Artefact
import org.lilie.services.eliot.tdbase.Question
import org.lilie.services.eliot.tdbase.Sujet
import org.lilie.services.eliot.tdbase.importexport.dto.ArtefactDto
import org.lilie.services.eliot.tdbase.importexport.dto.ExportDto
import org.lilie.services.eliot.tice.annuaire.Personne

/**
 * TODO
 * @author John Tranier
 */
class ExportMarshaller {
  QuestionMarshaller questionMarshaller
  SujetMarshaller sujetMarshaller
  PersonneMarshaller personneMarshaller

  Map marshall(Artefact artefact,
               Date date,
               Personne exporteur,
               String formatVersion = "1.0") {
    def representationArtefact = null

    if(artefact instanceof Question) {
      representationArtefact = questionMarshaller.marshall(artefact)
    }
    else if(artefact instanceof Sujet) {
      representationArtefact = sujetMarshaller.marshall(artefact)
    }

    return [
        class: ExportClass.EXPORT.name(),
        metadonnees: [
            date: date,
            exporteur: personneMarshaller.marshall(exporteur),
            formatVersion: formatVersion
        ],
        artefact: representationArtefact
    ]
  }

  static ExportDto parse(JSONElement jsonElement) {
    MarshallerHelper.checkClass(ExportClass.EXPORT, jsonElement)
    MarshallerHelper.checkFormatVersion("1.0", jsonElement.metadonnees)
    MarshallerHelper.checkIsJsonElement('artefact', jsonElement.artefact)
    MarshallerHelper.checkClassIn([
        ExportClass.SUJET,
        ExportClass.QUESTION_ATOMIQUE,
        ExportClass.QUESTION_COMPOSITE
    ],
        jsonElement.artefact
    )

    ArtefactDto artefactDto = null
    switch (jsonElement.artefact.class.toString()) {
      case ExportClass.SUJET.name():
        artefactDto = SujetMarshaller.parse(jsonElement.artefact)
        break;

      case ExportClass.QUESTION_ATOMIQUE.name():
      case ExportClass.QUESTION_COMPOSITE.name():
        artefactDto = QuestionMarshaller.parse(jsonElement.artefact)
        break
    }


    return new ExportDto(
        date: MarshallerHelper.parseDate(jsonElement.metadonnees.date),
        exporteur: PersonneMarshaller.parse(jsonElement.metadonnees.exporteur),
        formatVersion: jsonElement.metadonnees.formatVersion,
        artefact: artefactDto
    )
  }
}
