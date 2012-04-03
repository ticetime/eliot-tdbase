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
    $(document).ready(function () {
      $('#menu-item-sujets').addClass('actif');
    });
  </r:script>
  <title>TDBase - Edition des propriétés du sujet</title>
</head>

<body>
<g:render template="/breadcrumps" plugin="eliot-tice-plugin"
          model="[liens: liens]"/>

<g:hasErrors bean="${sujet}">
  <div class="portal-messages">
    <g:eachError>
      <li class="error"><g:message error="${it}"/></li>
    </g:eachError>
  </div>
</g:hasErrors>
<g:if test="${request.messageCode}">
  <div class="portal-messages">
    <li class="success"><g:message code="${request.messageCode}"
                                   class="portal-messages success"/></li>
  </div>
</g:if>

<form method="post" action="#" class="sujet">
  <div class="portal-form_container edite">
    <table>
      <tr>
        <td class="label title">Titre:</td>
        <td>
          <input size="80" type="text" value="${sujet.titre}" name="titre"/>
        </td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td>&nbsp;</td>
      </tr>
      <tr>
        <td class="label">Type&nbsp;:</td>
        <td>
          <g:select name="sujetType.id" value="${sujet.sujetType?.id}"
                    from="${typesSujet}"
                    optionKey="id"
                    optionValue="nom"/>
        </td>
      </tr>
      <tr>
        <td class="label">Mati&egrave;re&nbsp;:</td>
        <td>
          <g:select name="matiere.id" value="${sujet.matiere?.id}"
                    noSelection="${['null': g.message(code:"default.select.null")]}"
                    from="${matieres}"
                    optionKey="id"
                    optionValue="libelleLong"/>
        </td>
      </tr>
      <tr>
        <td class="label">Niveau&nbsp;:</td>
        <td>
          <g:select name="niveau.id" value="${sujet.niveau?.id}"
                    noSelection="${['null': g.message(code:"default.select.null")]}"
                    from="${niveaux}"
                    optionKey="id"
                    optionValue="libelleLong"/>
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
        <td class="label">Ordre&nbsp;questions&nbsp;:</td>
        <td>
          <g:checkBox name="ordreQuestionsAleatoire"
                      checked="${sujet.ordreQuestionsAleatoire}"/>
          Al&eacute;atoire</td>
      </tr>

      <tr>
        <td class="label">Description&nbsp;:</td>
        <td>
          <g:textArea cols="56" rows="10" name="presentation"
                      value="${sujet.presentation}"/>
        </td>
      </tr>
      <tr>
        <td class="label">Partage :</td>
        <td>
          <g:if test="${sujet.estPartage()}">
            <a href="${sujet.copyrightsType.lien}"
               target="_blank">${sujet.copyrightsType.presentation}</a>
          </g:if>
          <g:else>
            ce sujet n'est pas partagé
          </g:else>
        </td>
      </tr>
      <g:if test="${sujet.paternite}">
        <g:render template="/artefact/paternite"
                  model="[paternite: sujet.paternite]"/>
      </g:if>
    </table>
  </div>
  <g:hiddenField name="id" value="${sujet.id}"/>
  <div class="form_actions">
    <g:link action="${lienRetour.action}" controller="${lienRetour.controller}"
            params="${lienRetour.params}">Annuler</g:link> |
    <g:actionSubmit value="Enregistrer" action="enregistrePropriete"
                    class="button"
                    title="Enregistrer"/>
  </div>
</form>

</body>
</html>