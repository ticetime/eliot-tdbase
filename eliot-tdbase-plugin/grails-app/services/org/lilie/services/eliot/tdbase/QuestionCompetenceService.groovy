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
package org.lilie.services.eliot.tdbase

import org.lilie.services.eliot.competence.Competence
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Service de gestion des compétences associées à une question
 *
 * @author John Tranier
 */
class QuestionCompetenceService {

  static transactional = false

  @Transactional(propagation = Propagation.REQUIRED)
  QuestionCompetence createQuestionCompetence(Question question, Competence competence) {
    QuestionCompetence questionCompetence = new QuestionCompetence(
        question: question,
        competence: competence
    )

    question.addToAllQuestionCompetence(questionCompetence)
    question.save(failOnError: true)
    questionCompetence.save(failOnError: true)

    return questionCompetence
  }

  @Transactional(propagation = Propagation.REQUIRED)
  void deleteQuestionCompetence(QuestionCompetence questionCompetence) {
    Question question = questionCompetence.question
    question.removeFromAllQuestionCompetence(questionCompetence)
    question.save(flush: true)
  }

  @Transactional(propagation = Propagation.REQUIRED)
  updateQuestionCompetenceList(Question question, List<Competence> competenceList) {
    // Ajoute toutes les nouvelles compétences
    List competenceExistanteList = question.allQuestionCompetence*.competence ?: []
    competenceList.each { Competence competence ->
      if(!competenceExistanteList*.id.contains(competence.id)) {
        createQuestionCompetence(question, competence)
      }
    }

    // Supprimes toutes les compétences qui ne sont plus associées à la question
    new ArrayList<QuestionCompetence>(question.allQuestionCompetence ?: []).each { QuestionCompetence questionCompetence ->
      if(!competenceList*.id.contains(questionCompetence.competence.id)) {
        deleteQuestionCompetence(questionCompetence)
      }
    }
  }

  boolean isQuestionAssociableACompetence(Question question) {
    return question.type.code in [
        QuestionTypeEnum.MultipleChoice.name(),
        QuestionTypeEnum.ExclusiveChoice.name(),
        QuestionTypeEnum.Integer.name(),
        QuestionTypeEnum.Decimal.name(),
        QuestionTypeEnum.Slider.name(),
        QuestionTypeEnum.FillGap.name(),
        QuestionTypeEnum.FillGraphics.name(),
        QuestionTypeEnum.GraphicMatch.name(),
        QuestionTypeEnum.Associate.name(),
        QuestionTypeEnum.Order.name(),
        QuestionTypeEnum.BooleanMatch.name()
    ]
  }
}
