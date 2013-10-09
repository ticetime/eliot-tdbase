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



import groovy.util.slurpersupport.GPathResult
import org.codehaus.groovy.grails.commons.ApplicationAttributes
import org.lilie.services.eliot.competence.Referentiel
import org.lilie.services.eliot.competence.ReferentielDto
import org.lilie.services.eliot.competence.ReferentielService
import org.lilie.services.eliot.emaeval.ReferentielMarshaller
import org.lilie.services.eliot.tdbase.patch.PatchExecution
import org.lilie.services.eliot.tdbase.patch.PatchTDB40
import org.lilie.services.eliot.tice.migrations.DbMigrationService
import org.lilie.services.eliot.tice.utils.BootstrapService
import org.lilie.services.eliot.tice.utils.PortailTagLibService
import org.springframework.core.io.ClassPathResource


class BootStrap {

  BootstrapService bootstrapService
  DbMigrationService dbMigrationService
  PortailTagLibService portailTagLibService

  ReferentielService referentielService

  def init = { servletContext ->

    def config = servletContext.grailsApplication.config

    if (config.eliot.bootstrap.migration) {
      dbMigrationService.updateDb()
    }

    if (config.eliot.bootstrap.jeudetest) {
      bootstrapService.bootstrapJeuDeTestDevDemo()
      initialiseReferentielCompetence()
    }

    try {
      portailTagLibService.addManuelDocumentUrls(config.eliot.manuels.documents.urlMap)
    } catch (Exception e) {
      log.error("manuels.urls.update.echec")
      log.error(e.message)
    }

    try {
      portailTagLibService.addHelpDocumentUrls(config.eliot.help.documents.urlMap)
    } catch (Exception e) {
      log.error("help.urls.update.echec")
      log.error(e.message)
    }

    portailTagLibService.applicationInFrame = config.eliot.pages.container.forceDimensions
    portailTagLibService.divHeight = config.eliot.pages.container.height
    portailTagLibService.divWidth = config.eliot.pages.container.width

    executeAllPatch(servletContext)
  }

  private void initialiseReferentielCompetence() {
    final String REFERENTIEL_NOM = "Palier 1"
    if(!Referentiel.findByNom(REFERENTIEL_NOM)) {
      GPathResult xml = new XmlSlurper().parse(
          new ClassPathResource('emaeval/palier1.xml').inputStream
      )

      ReferentielMarshaller referentielMarshaller = new ReferentielMarshaller()
      ReferentielDto referentielDto = referentielMarshaller.parse(xml)

      referentielService.importeReferentiel(referentielDto)
    }
  }

  private static void executeAllPatch(def servletContext) {
    final LOCK_ID = 1

    PatchExecution.withTransaction {
      PatchExecution.lock(LOCK_ID) // Verrouille la ligne correspondant au LOCK pour qu'il n'y ait pas d'applications de patches concurrentes


      def applicationContext = servletContext.getAttribute(ApplicationAttributes.APPLICATION_CONTEXT)

      if (!PatchExecution.findByCode(PatchTDB40.CODE)) {

        PatchTDB40 patchTDB40 = new PatchTDB40(applicationContext: applicationContext)
        patchTDB40.execute()

        new PatchExecution(code: PatchTDB40.CODE).save()
      }
    }


  }

  def destroy = {}


}
