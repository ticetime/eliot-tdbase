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

package org.lilie.services.eliot.tdbase.impl.graphicmatch

import org.lilie.services.eliot.tdbase.Reponse
import org.lilie.services.eliot.tdbase.ReponseSpecification
import org.lilie.services.eliot.tdbase.ReponseSpecificationService

/**
 * Created by IntelliJ IDEA.
 * User: bert
 * Date: 20/12/11
 * Time: 14:02
 * To change this template use File | Settings | File Templates.
 */
class ReponseGraphicMatchSpecificationService extends ReponseSpecificationService<ReponseGraphicMatchSpecification> {

    @Override
    ReponseGraphicMatchSpecification createSpecification(Map map) {
        new ReponseGraphicMatchSpecification(map)
    }

    @Override
    Float evalueReponse(Reponse reponse) {
        ReponseGraphicMatchSpecification repSpecObj = reponse.specificationObject
        reponse.correctionNoteAutomatique = repSpecObj.evaluate(reponse.sujetQuestion.points)
        reponse.save()
        reponse.correctionNoteAutomatique
    }
}

/**
 * Specifications de reponses de type graphique à compléter.
 */
class ReponseGraphicMatchSpecification implements ReponseSpecification {

    /**
     * Liste d'elements fournis comme reponse à la question.
     */
    List<TextField> valeursDeReponse = []

    /**
     * Liste d'elements qui forment une reponse correcte.
     */
    List<TextField> reponsesPossibles = []

    /**
     * Constructeur par defaut
     */
    ReponseGraphicMatchSpecification() {
        super()
    }

    /**
     * Constructeur
     * @param params map des paramètres pour l'initialisation de l'objet
     */
    ReponseGraphicMatchSpecification(Map params) {
        valeursDeReponse = params.valeursDeReponse.collect {createTextField(it)}
        reponsesPossibles = params.reponsesPossibles.collect {createTextField(it)}
    }

    @Override
    Map toMap() {
        [
                valeursDeReponse: valeursDeReponse.collect {it.toMap()},
                reponsesPossibles: reponsesPossibles.collect {it.toMap()}
        ]
    }

    /**
     * Logique d'evaluation.
     * @param maximumPoints les points maximum que l'on peut atteindre si la reponse est bonne.
     * @return les points correspondants à l'evaluation.
     */
    def evaluate(float maximumPoints) {
        def differenceCount = (reponsesPossibles - valeursDeReponse).size()
        def totalFieldCount = reponsesPossibles.size()
        def validFieldCount = totalFieldCount - differenceCount
        maximumPoints * validFieldCount / totalFieldCount
    }

    def createTextField(TextField field) {
        field
    }

    def createTextField(Map params) {
        new TextField(params)
    }
}
