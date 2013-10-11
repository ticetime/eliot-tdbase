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

package org.lilie.services.eliot.emaeval

import com.pentila.evalcomp.domain.definition.Referentiel as EmaEvalReferentiel
import grails.converters.JSON
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.lilie.services.eliot.competence.Referentiel as EliotReferetiel
import org.lilie.services.eliot.competence.SourceReferentiel

class EmaEvalController {

  static defaultAction = "admin"

  EmaEvalService emaEvalService
  GrailsApplication grailsApplication

  def admin() {
    return [
        config: grailsApplication.config,
        eliotReferentiel: emaEvalService.eliotReferentielPalier3
    ]
  }

  /**
   * Importe un référentiel depuis EmaEval dans Eliot
   * @param emaEvalReferentielId l'identifiant EmaEval du référentiel à importer
   * @return
   */
  def importeReferentiel(Long emaEvalReferentielId) {

    try {
      emaEvalService.importeReferentielDansEliot(
          emaEvalService.findEmaEvalReferentielById(emaEvalReferentielId)
      )

      redirect(action: 'admin')
    }
    catch (Exception e) {
      render(view: 'erreurImport', model: [exception: e])
    }
  }

  /**
   * Vérifie que l'on peut bien récupérer le référentiel "Palier 3" par les WS
   * EmaEval
   */
  def verifiePreconditionImportReferentielEmaEvalVersEliot() {
    EmaEvalReferentiel emaEvalReferentiel = null

    try {
      emaEvalReferentiel = emaEvalService.findEmaEvalReferentielByName(
          EmaEvalService.REFERENTIEL_PALIER_3_NOM
      )
    }
    catch (Exception e) {
      log.warn("La connexion à EmaEval a échouée", e)

      render([
          connexionEtablie: false,
          exception: g.renderStackTrace(exception: e)
      ] as JSON)
      return
    }

    if (!emaEvalReferentiel) {
      render([
          connexionEtablie: true,
          emaEvalReferentielId: null
      ] as JSON)
      return
    }

    render([
        connexionEtablie: true,
        emaEvalReferentielId: emaEvalReferentiel.id
    ] as JSON)
  }

  /**
   * Vérifie que la correspondance entre un référentiel Eliot et son homologue dans EmaEval
   * @param eliotReferentielId
   * @return
   */
  def verifieLiaisonEliotEmaEvalReferentiel(Long eliotReferentielId) {
    try {
      EliotReferetiel eliotReferentiel = EliotReferetiel.get(eliotReferentielId)

      EmaEvalReferentiel emaEvalReferentiel
      try {
        emaEvalReferentiel = emaEvalService.findEmaEvalReferentielById(
            Long.parseLong(eliotReferentiel.getIdExterne(SourceReferentiel.EMA_EVAL))
        )
      }
      catch (Exception e) {
        log.warn("La connexion à EmaEval a échouée", e)

        render([
            connexionEtablie: false,
            exception: g.renderStackTrace(exception: e)
        ] as JSON)
        return
      }

      if (!emaEvalReferentiel) {
        render([
            connexionEtablie: true,
            emaEvalReferentiel: false
        ] as JSON)
        return
      }

      try {
        emaEvalService.verifieCorrespondanceReferentiel(
            eliotReferentiel,
            emaEvalReferentiel
        )
      }
      catch (Exception e) {
        render([
            connexionEtablie: true,
            emaEvalReferentiel: true,
            exception: g.renderStackTrace(exception: e)
        ] as JSON)
        return
      }

      render([
          connexionEtablie: true,
          emaEvalReferentiel: true,
          coherenceReferentiel: true
      ] as JSON)
      return
    }
    catch (Exception e) {
      render([
          exception: e
      ])
    }
  }

}
