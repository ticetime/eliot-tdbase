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

package org.lilie.services.eliot.tdbase.impl.fillgap

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.lilie.services.eliot.tdbase.ReponseSpecificationService
import org.springframework.transaction.annotation.Transactional
import static org.lilie.services.eliot.tice.utils.StringUtils.normalise

class ReponseFillGapSpecificationService implements ReponseSpecificationService {

    String getSpecificationFromObject(Object object) {
        assert (object instanceof ReponseFillGapSpecification)
        new JsonBuilder(object.toMap()).toString()

    }

    def getObjectFromSpecification(String specification) {
        if (!specification) {
            return new ReponseFillGapSpecification()
        }
        Map map = new JsonSlurper().parseText(specification)
        return new ReponseFillGapSpecification(map)
    }

    def getObjectInitialiseFromSpecification(org.lilie.services.eliot.tdbase.Question question) {
        assert (question.specificationObject instanceof FillGapSpecification)
        new ReponseFillGapSpecification(reponsesPossibles: question.specificationObject.reponsesPossibles)
    }

    def updateReponseSpecificationForObject(org.lilie.services.eliot.tdbase.Reponse reponse, Object object) {
        reponse.specification = getSpecificationFromObject(object)
        reponse.save()
    }

    def initialiseReponseSpecificationForQuestion(org.lilie.services.eliot.tdbase.Reponse reponse, org.lilie.services.eliot.tdbase.Question question) {
        updateReponseSpecificationForObject(reponse, getObjectInitialiseFromSpecification(question))
    }

    @Transactional
    Float evalueReponse(org.lilie.services.eliot.tdbase.Reponse reponse) {
        int reponsesCorrects = 0
        ReponseFillGapSpecification repSpecObj = reponse.specificationObject
        int numberRes = repSpecObj.valeursDeReponse.size()


        for (i in 0..numberRes - 1) {

            def valeurDeReponse = normalise(repSpecObj.valeursDeReponse[i].trim())

            if (repSpecObj.reponsesPossibles[i].contains(valeurDeReponse)) {
                reponsesCorrects++
            }
        }

        float points = (reponsesCorrects / numberRes) * reponse.sujetQuestion.points

        reponse.correctionNoteAutomatique = points
        reponse.save()
        points
    }
}

class ReponseFillGapSpecification {

    List<String> valeursDeReponse = []
    List<List<String>> reponsesPossibles = [[]]

    def toMap() {
        [
                valeursDeReponse: valeursDeReponse,
                reponsesPossibles: reponsesPossibles
        ]
    }

}