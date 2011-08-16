<%@ page import="org.lilie.services.eliot.tdbase.SujetType" %>
%{--
  - Copyright © FYLAB and the Conseil Régional d'Île-de-France, 2009
  - This file is part of L'Interface Libre et Interactive de l'Enseignement (Lilie).
  -
  - Lilie is free software. You can redistribute it and/or modify since
  - you respect the terms of either (at least one of the both license) :
  -  under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -  the CeCILL-C as published by CeCILL-C; either version 1 of the
  - License, or any later version
  -
  - There are special exceptions to the terms and conditions of the
  - licenses as they are applied to this software. View the full text of
  - the exception in file LICENSE.txt in the directory of this software
  - distribution.
  -
  - Lilie is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - Licenses for more details.
  -
  - You should have received a copy of the GNU General Public License
  - and the CeCILL-C along with Lilie. If not, see :
  -  <http://www.gnu.org/licenses/> and
  -  <http://www.cecill.info/licences.fr.html>.
  --}%

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <meta name="layout" content="eliot-tdbase"/>
  <r:require modules="jquery"/>
  <r:script>
    $(document).ready(function() {
      $('#menu-item-sujets').addClass('actif');
    });
  </r:script>
  <title>TDBase - Edition des propriétés du sujet</title>
</head>

<body>

<div class="column span-22 last middle">
  <g:render template="/breadcrumps" model="[liens: liens]"/>

  <g:hasErrors bean="${sujet}">
    <div class="portal-messages error">
      <g:eachError>
        <li><g:message error="${it}"/></li>
      </g:eachError>
    </div>
  </g:hasErrors>
  <g:if test="${request.messageCode}">
    <div class="portal-messages success">
      <li><g:message code="${request.messageCode}"
                     class="portal-messages success"/></li>
    </div>
  </g:if>

  <form method="post"
        action="#">
    <div class="portal-form_container">
      <table>
        <tr>
          <td class="label">Titre:</td>
          <td>
            <input size="80" type="text" value="${sujet.titre}" name="titre"/>
          </td>
        </tr>
        <tr>
          <td class="label">Type&nbsp;:</td>
          <td>
             <g:select name="sujetType.id" value="${sujet.sujetType?.id}"
                      noSelection="${['null':'Sélectionner un type...']}"
                      from="${typesSujet}"
                      optionKey="id"
                      optionValue="nom" />
          </td>
        </tr>
        <tr>
          <td class="label">Mati&egrave;re&nbsp;:</td>
          <td>
            <g:select name="matiere.id" value="${sujet.matiere?.id}"
                      noSelection="${['null':'Sélectionner une matière...']}"
                      from="${matieres}"
                      optionKey="id"
                      optionValue="libelleLong" />
          </td>
        </tr>
        <tr>
          <td class="label">Niveau&nbsp;:</td>
          <td>
            <g:select name="niveau.id" value="${sujet.niveau?.id}"
                      noSelection="${['null':'Sélectionner un niveau...']}"
                      from="${niveaux}"
                      optionKey="id"
                      optionValue="libelleLong" />
          </td>
        </tr>
        <tr>
          <td class="label">Dur&eacute;e&nbsp;:</td>
          <td>
            <input type="text" name="dureeMinutes" value="${sujet.dureeMinutes}"/>
            <i>(en minutes)</i>
          </td>
        </tr>
        <tr>
          <td class="label">Accessible&nbsp;:</td>
          <td>
            <g:checkBox name="accesPublic" checked="${sujet.accesPublic}"/>
            via lien public</td>
        </tr>
        <tr>
          <td class="label">Présentation&nbsp;:</td>
          <td>
            <g:checkBox name="accesSequentiel" checked="${sujet.accesSequentiel}"/>
            1 seule question par écran</td>
        </tr>
        <tr>
          <td class="label">Ordre&nbsp;questions&nbsp;:</td>
          <td>
            <g:checkBox name="ordreQuestionsAleatoire" checked="${sujet.ordreQuestionsAleatoire}"/>
            Al&eacute;atoire</td>
        </tr>
        <tr>
          <td class="label">S&eacute;lection&nbsp;:</td>
          <td>
            <input type="text" name="nbQuestions" value="${sujet.nbQuestions}"/>
            <i>(le nombre de questions &agrave; selectionner)</i>
          </td>
        </tr>
        <tr>
          <td class="label">Description&nbsp;:</td>
          <td>
            <g:textArea cols="56" rows="10" name="presentation" value="${sujet.presentation}"/>
          </td>
        </tr>
      </table>
    </div>
    <g:hiddenField name="id" value="${sujet.id}"/>
    <div class="form_actions">
      <g:link action="${lienRetour.action}" controller="${lienRetour.controller}"
              params="${lienRetour.params}">Annuler</g:link> |
      <g:actionSubmit value="Enregistrer" action="enregistrePropriete" title="Enregistrer"/>
    </div>
  </form>
</div>

</body>
</html>