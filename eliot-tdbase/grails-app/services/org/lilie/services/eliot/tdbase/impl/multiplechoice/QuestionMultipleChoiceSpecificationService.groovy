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



package org.lilie.services.eliot.tdbase.impl.multiplechoice

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.lilie.services.eliot.tdbase.QuestionSpecificationService
import org.lilie.services.eliot.tice.utils.StringUtils
import org.lilie.services.eliot.tdbase.Question

/**
 *
 * @author franck Silvestre
 */
class QuestionMultipleChoiceSpecificationService implements QuestionSpecificationService {

  static transactional = false


  /**
   *
   * @see QuestionSpecificationService
   */
  def getObjectFromSpecification(String specification) {
    if (!specification) {
      return new MultipleChoiceSpecification()
    }
    def slurper = new JsonSlurper()
    Map map = slurper.parseText(specification)
    return new MultipleChoiceSpecification(map)
  }

  /**
   *
   * @see QuestionSpecificationService
   */
  String getSpecificationFromObject(Object object) {

    assert(object instanceof MultipleChoiceSpecification)

    JsonBuilder builder = new JsonBuilder(object.toMap())
    return builder.toString()
  }

  /**
   *
   * @see QuestionSpecificationService
   */
  String getSpecificationNormaliseFromObject(Object object) {

    assert(object instanceof MultipleChoiceSpecification)

    String toNormalise = object.libelle
    if (toNormalise) {
      return StringUtils.normalise(toNormalise)
    }
    return null
  }

  /**
   *
   * @see QuestionSpecificationService
   */
  def updateQuestionSpecificationForObject(Question question, Object object) {
    question.specification = getSpecificationFromObject(object)
    question.specificationNormalise = getSpecificationNormaliseFromObject(object)
    question.save()
  }

}

/**
 * Représente un objet spécification pour une question de type MultipleChoice
 */
class MultipleChoiceSpecification {
  String libelle
  String correction
  List<MultipleChoiceSpecificationReponsePossible> reponses = []

  MultipleChoiceSpecification() {
    super()
  }

  /**
   * Créer et initialise un nouvel objet de type MultipleChoiceSpecification
   * @param map la map permettant d'initialiser l'objet en cours
   * de création
   */
  MultipleChoiceSpecification(Map map) {
    libelle = map.libelle
    correction = map.correction
    reponses = map.reponses.collect {
      if (it instanceof MultipleChoiceSpecificationReponsePossible) {
        it
      } else {
        new MultipleChoiceSpecificationReponsePossible(it)
      }
    }
  }

  def toMap() {
    [
            libelle: libelle,
            correction: correction,
            reponses: reponses*.toMap()
    ]
  }

}

/**
 * Représente un objet réponse possible de la spécification  pour une question de
 * type MultipleChoice
 */
class MultipleChoiceSpecificationReponsePossible {
  String libelleReponse
  boolean estUneBonneReponse
  Float rang

  MultipleChoiceSpecificationReponsePossible() {
    super()
  }

  /**
   * Créer et initialise un nouvel objet de type MultipleChoiceSpecificationReponsePossible
   * @param map la map permettant d'initialiser l'objet en cours
   * de création
   */
  MultipleChoiceSpecificationReponsePossible(Map map) {
    libelleReponse = map.libelleReponse
    estUneBonneReponse = map.estUneBonneReponse
    rang = map.rang
  }

  def toMap() {
    [
            libelleReponse: libelleReponse,
            estUneBonneReponse: estUneBonneReponse,
            rang: rang
    ]
  }
}