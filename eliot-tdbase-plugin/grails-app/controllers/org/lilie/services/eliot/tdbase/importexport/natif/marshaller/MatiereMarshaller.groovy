package org.lilie.services.eliot.tdbase.importexport.natif.marshaller

import org.codehaus.groovy.grails.web.json.JSONElement
import org.codehaus.groovy.grails.web.json.JSONObject
import org.lilie.services.eliot.tdbase.importexport.dto.MatiereDto
import org.lilie.services.eliot.tice.scolarite.Matiere

/**
 * Marshaller qui permet de convertir une matière en une représentation à base de Map
 * qui pourra ensuite être convertie en XML ou en JSON
 *
 * @author John Tranier
 */
class MatiereMarshaller {

  @SuppressWarnings('ReturnsNullInsteadOfEmptyCollection')
  Map marshall(Matiere matiere) {
    if(!matiere) {
      return null
    }

    return [
        class: ExportClass.MATIERE.name(),
        identifiant: matiere.id,
        codeGestion: matiere.codeGestion,
        libelleLong: matiere.libelleLong,
        libelleCourt: matiere.libelleCourt,
        libelleEdition: matiere.libelleEdition
    ]
  }

  static MatiereDto parse(JSONElement jsonElement) {
    MarshallerHelper.checkClass(ExportClass.MATIERE, jsonElement)
    return new MatiereDto(
        identifiant: MarshallerHelper.jsonObjectToObject(jsonElement.identifiant),
        codeGestion: MarshallerHelper.jsonObjectToString(jsonElement.codeGestion),
        libelleLong: MarshallerHelper.jsonObjectToString(jsonElement.libelleLong),
        libelleCourt: MarshallerHelper.jsonObjectToString(jsonElement.libelleCourt),
        libelleEdition: MarshallerHelper.jsonObjectToString(jsonElement.libelleEdition)
    )
  }

  static parse(JSONObject.Null jsonElement) {
    return null
  }
}

