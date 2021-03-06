%<%@ page import="org.lilie.services.eliot.tdbase.importexport.Format; org.lilie.services.eliot.tice.CopyrightsType; org.lilie.services.eliot.tdbase.SujetType" %>
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
    <r:require modules="eliot-tdbase-ui"/>
    <r:script>
        $(document).ready(function () {
            $('#menu-item-sujets').addClass('actif');
            initButtons();
        });
    </r:script>
    <title><g:message code="sujet.editeProprietes.head.title"/></title>
</head>

<body>
<g:render template="/breadcrumps" plugin="eliot-tice-plugin"
          model="[liens: liens]"/>

<div class="portal-tabs">
    <span class="portal-tabs-famille-liens">
        <g:render template="menuSujet"
                  model="${[
                      artefactHelper: artefactHelper,
                      sujet: sujet,
                      utilisateur: utilisateur,
                      modeEdition: false
                  ]}"/>
    </span>

  <span class="portal-tabs-famille-liens">
    <button id="toolbar_${sujet.id}">Actions</button>
    <ul id="menu_actions_toolbar_${sujet.id}" class="tdbase-menu-actions">
      <g:render template="menuActions"
                model="${[
                    artefactHelper: artefactHelper,
                    sujet         : sujet,
                    utilisateur   : utilisateur
                ]}"/>
    </ul>
  </span>
</div>
<g:if test="${flash.erreurMessage}">
  <div class="portal-messages">
    <li class="error"><g:message
        code="${flash.erreurMessage}"
        class="portal-messages error"/></li>
  </div>
</g:if>
<g:if test="${flash.messageCode}">
    <div class="portal-messages">
        <li class="success"><g:message code="${flash.messageCode}"
                                       class="portal-messages success"/></li>
    </div>
</g:if>

<div class="portal-form_container edite">
    <table>
        <tr>
            <td class="label title">Titre&nbsp;:</td>
            <td>
                ${sujet.titre}
            </td>
        </tr>
        <tr>
            <td>&nbsp;</td>
            <td>&nbsp;</td>
        </tr>
        <tr>
            <td class="label">Type&nbsp;:</td>
            <td>
                ${sujet.sujetType.nom}
            </td>
        </tr>
        <tr>
            <td class="label">Mati&egrave;re&nbsp;:</td>
            <td>
                ${sujet.matiereBcn?.libelleEdition}
            </td>
        </tr>
        <tr>
            <td class="label">Niveau&nbsp;:</td>
            <td>
                ${sujet.niveau?.libelleLong}
            </td>
        </tr>
        <tr>
            <td class="label">Travail collaboratif&nbsp;:</td>
            <td>
                <g:if test="${sujet.contributeurs}">
                    <ul>
                        <g:each in="${sujet.contributeurs}" var="contributeur">
                            <li>${contributeur.nomAffichage}</li>
                        </g:each>
                    </ul>
                </g:if>
                <g:else>
                    Aucun formateur ajouté<br/>
                </g:else>
            </td>
        </tr>
        <tr>
            <td class="label">Dur&eacute;e&nbsp;:</td>
            <td>
                ${sujet.dureeMinutes}
                (en minutes)
            </td>
        </tr>

        <tr>
            <td class="label">Ordre&nbsp;questions&nbsp;:</td>
            <td>
                <g:checkBox name="ordreQuestionsAleatoire"
                            checked="${sujet.ordreQuestionsAleatoire}" disabled="true"/>
                Al&eacute;atoire</td>
        </tr>

        <tr>
            <td class="label">Description&nbsp;:</td>
            <td>
                ${sujet.presentation}
            </td>
        </tr>
        <g:if test="${artefactHelper.partageArtefactCCActive}">
            <tr>
                <td class="label">Partage :</td>
                <td>
                    <g:if test="${sujet.estPartage()}">
                        <a href="${sujet.copyrightsType.lien}"
                           target="_blank"><img src="${sujet.copyrightsType.logo}"
                                                title="${sujet.copyrightsType.presentation}"/>
                        </a>
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
        </g:if>
    </table>
</div>

<g:render template="../importexport/export_dialog"/>

</body>
</html>