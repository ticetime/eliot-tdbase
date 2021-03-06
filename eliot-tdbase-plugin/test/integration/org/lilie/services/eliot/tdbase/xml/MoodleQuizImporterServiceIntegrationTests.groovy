/*
 * Copyright © FYLAB and the Conseil Régional d'Île-de-France, 2009
 * This file is part of L'Interface Libre et Interactive de l'Enseignement (Lilie).
 *
 *  Lilie is free software. You can redistribute it and/or modify since
 *  you respect the terms of either (at least one of the both license) :
 *  - under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  - the CeCILL-C as published by CeCILL-C; either version 1 of the
 *  License, or any later version
 *
 *  There are special exceptions to the terms and conditions of the
 *  licenses as they are applied to this software. View the full text of
 *  the exception in file LICENSE.txt in the directory of this software
 *  distribution.
 *
 *  Lilie is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  Licenses for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  and the CeCILL-C along with Lilie. If not, see :
 *  <http://www.gnu.org/licenses/> and
 *  <http://www.cecill.info/licences.fr.html>.
 */

package org.lilie.services.eliot.tdbase.xml

import org.lilie.services.eliot.tice.annuaire.Personne
import org.lilie.services.eliot.tice.scolarite.StructureEnseignement
import org.lilie.services.eliot.tdbase.*
import org.lilie.services.eliot.tice.utils.BootstrapService
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationContext

/**
 *
 * @author franck Silvestre
 */
class MoodleQuizImporterServiceIntegrationTests extends GroovyTestCase implements ApplicationContextAware {

  private static final String SUJET_1_TITRE = "Sujet test 1"
  static final INPUT = 'org/lilie/services/eliot/tdbase/xml/exemples/quiz-exemple-20120229-0812.xml'


  ApplicationContext applicationContext
  Personne personne1
  Personne personne2
  StructureEnseignement struct1ere

  BootstrapService bootstrapService
  SujetService sujetService
  MoodleQuizImporterService moodleQuizImporterService


  protected void setUp() {
    super.setUp()
    bootstrapService.bootstrapForIntegrationTest()
    personne1 = bootstrapService.enseignant1
    personne2 = bootstrapService.enseignant2
    struct1ere = bootstrapService.classe1ere
  }

  protected void tearDown() {
    super.tearDown()
  }



  void testImportMoodleQuiz() {
    Sujet sujet2 = sujetService.createSujet(personne1, SUJET_1_TITRE)
    assertFalse(sujet2.hasErrors())
    def sujet1 = Sujet.get(sujet2.id)
    def input = applicationContext.getResource("classpath:$INPUT").getInputStream().bytes
    MoodleQuizImportReport report = moodleQuizImporterService.importMoodleQuiz(
        input,
        sujet1,
        new ReferentielEliot(
            matiereBcn: sujet1.matiereBcn,
            niveau: sujet1.niveau
        ),
        personne1
    )
    assert report.nombreItemsTraites == 11
    assert report.itemsImportes.size() == 9
    assert report.itemsNonImportes.size() == 2
    println report

  }

}
