<%@ page import="org.lilie.services.eliot.tdbase.importexport.Format; org.lilie.services.eliot.tice.CopyrightsType" %>
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
  <r:require modules="eliot-tdbase-ui, jquery-ui, eliot-tdbase-combobox-autocomplete"/>
  <r:script>
    $(document).ready(function () {
      $('#menu-item-sujets').addClass('actif');
      initButtons();

      initComboboxAutoComplete({
        combobox: '#matiereId',

        recherche: function(recherche, callback) {
          if (recherche == null || recherche.length < 3) {
            callback([]);
          }
          else {
            $.ajax({
              url: '${g.createLink(absolute:true, uri:"/sujet/matiereBcns")}',

              data: {
                recherche: recherche
              },

              success: function(matiereBcns) {
                var options = [];

                for(var i = 0; i < matiereBcns.length; i++) {
                  options.push({
                    id: matiereBcns[i].id,
                    value: matiereBcns[i].libelleEdition
                  });
                }

                callback(options);
              }
            });
          }
        }

      });
    });
  </r:script>
  <title>
    <g:if test="${afficheFormulaire}">
      <g:message code="sujet.recherche.head.title"/>
    </g:if>
    <g:else>
      <g:message code="sujet.recherche.mesSujets.head.title"/>
    </g:else>
  </title>
</head>

<body>
<g:render template="/breadcrumps" plugin="eliot-tice-plugin"
          model="[liens: liens]"/>
<g:if test="${afficheFormulaire}">
  <form>
    <div class="portal-form_container recherche">
      <table>
        <tr>
          <td class="label">
            Titre :
          </td>
          <td>
            <g:textField name="patternTitre" title="titre"
                         value="${rechercheCommand.patternTitre}"/>
          </td>
          <td width="20"/>
          <td class="label">Type :
          </td>
          <td>
            <g:select name="typeId" value="${rechercheCommand.typeId}"
                      noSelection="${['null': 'Tous']}"
                      from="${typesSujet}"
                      optionKey="id"
                      optionValue="nom"/>
          </td>
        </tr>
        <tr>
          <td class="label">
            Description :
          </td>
          <td>
            <g:textField name="patternPresentation" title="description"
                         value="${rechercheCommand.patternPresentation}"/>
          </td>
          <td width="20"/>
          <td class="label">Matière :
          </td>
          <td>
            <g:select name="matiereId" value="${rechercheCommand.matiereId}"
                      from="${matiereBcns}"
                      optionKey="id"
                      optionValue="libelleEdition"/>
          </td>
        </tr>
        <tr>
        <g:if test="${artefactHelper.partageArtefactCCActive}">
          <td class="label">Auteur :
          </td>
          <td>
            <g:textField name="patternAuteur" title="auteur"
                         value="${rechercheCommand.patternAuteur}"/>
          </td>
         </g:if>
         <g:else>
             <td class="label">&nbsp;
             </td>
             <td>
                 &nbsp;
             </td>
         </g:else>
          <td width="20"/>
          <td class="label">Niveau :
          </td>
          <td>
            <g:select name="niveauId" value="${rechercheCommand.niveauId}"
                      noSelection="${['null': 'Tous']}"
                      from="${niveaux}"
                      optionKey="id"
                      optionValue="libelleLong"/>
          </td>
        </tr>

      </table>
    </div>

    <div class="form_actions recherche">
      <g:actionSubmit value="Rechercher" action="recherche" class="button"
                      title="Lancer la recherche"/>
    </div>
  </form>
</g:if>

<g:if test="${sujets}">

  <div class="portal_pagination">
    <p class="nb_result">${sujets.totalCount} résultat(s)</p>

    <g:if test="${affichePager}">
      <div class="pager">Page(s) : <g:paginate total="${sujets.totalCount}"
                                               params="${rechercheCommand?.toParams()}"></g:paginate></div>
    </g:if>
  </div>

  <div class="portal-default_results-list sujet">
    <%
      def messageDialogue = g.message(code: "sujet.partage.dialogue", args: [CopyrightsType.getDefaultForPartage().logo, CopyrightsType.getDefaultForPartage().code, CopyrightsType.getDefaultForPartage().lien])
    %>
    <g:each in="${sujets}" status="i" var="sujetInstance">
      <div class="${(i % 2) == 0 ? 'even' : 'odd'}" style="z-index: 0">

        <h1>${fieldValue(bean: sujetInstance, field: "titre")}</h1>

        <button id="${sujetInstance.id}">Actions</button>
        <ul id="menu_actions_${sujetInstance.id}" class="tdbase-menu-actions">
          <li><g:link action="teste" id="${sujetInstance.id}">
            Tester
          </g:link>
          </li>
          <li><g:link action="ajouteSeance" id="${sujetInstance.id}">
            Nouvelle&nbsp;séance
          </g:link>
          </li>
          <li><hr/></li>
          <g:if test="${artefactHelper.utilisateurPeutModifierArtefact(utilisateur, sujetInstance)}">
            <li><g:link action="edite"
                        id="${sujetInstance.id}">Modifier</g:link></li>
          </g:if>
          <g:else>
            <li>Modifier</li>
          </g:else>
          <g:if test="${artefactHelper.utilisateurPeutDupliquerArtefact(utilisateur, sujetInstance)}">
            <li><g:link action="duplique"
                        id="${sujetInstance.id}">Dupliquer</g:link></li>
          </g:if>
          <g:else>
            <li>Dupliquer</li>
          </g:else>
          <li><hr/></li>
        <g:if test="${artefactHelper.partageArtefactCCActive}">
          <g:if test="${artefactHelper.utilisateurPeutPartageArtefact(utilisateur, sujetInstance)}">
            <%
              def docLoc = g.createLink(action: 'partage', id: sujetInstance.id)
            %>
            <li><g:link action="partage"
                        id="${sujetInstance.id}"
                        onclick="afficheDialogue('${messageDialogue}', '${docLoc}');return false;">Partager</g:link></li>
          </g:if>
          <g:else>
            <li>Partager</li>
          </g:else>
        </g:if>
          <g:set var="peutExporterNatifJson"
                 value="${artefactHelper.utilisateurPeutExporterArtefact(utilisateur, sujetInstance, Format.NATIF_JSON)}"/>
          <g:set var="peutExporterMoodleXml"
                 value="${artefactHelper.utilisateurPeutExporterArtefact(utilisateur, sujetInstance, Format.MOODLE_XML)}"/>

          <g:if test="${peutExporterNatifJson || peutExporterMoodleXml}">
            <li>
              <g:set var="urlFormatNatifJson" value="${createLink(action: 'exporter', id: sujetInstance.id, params: [format: Format.NATIF_JSON.name()])}"/>
              <g:set var="urlFormatMoodleXml" value="${createLink(action: 'exporter', id: sujetInstance.id, params: [format: Format.MOODLE_XML.name()])}"/>
              <a href="#" onclick="actionExporter('${urlFormatNatifJson}', '${peutExporterMoodleXml ? urlFormatMoodleXml : null}')">Exporter</a>
            </li>
          </g:if>
          <g:else>
            Exporter
          </g:else>

          <li><hr/></li>
          <g:if test="${artefactHelper.utilisateurPeutSupprimerArtefact(utilisateur, sujetInstance)}">
            <li><g:link action="supprime" id="${sujetInstance.id}">Supprimer</g:link></li>
          </g:if>
          <g:else>
            <li>Supprimer</li>
          </g:else>
        </ul>

        <p class="date">Mise à jour le ${sujetInstance.lastUpdated?.format('dd/MM/yy HH:mm')}</p>

        <p>
          <g:if
              test="${sujetInstance.niveau?.libelleLong}"><strong>» Niveau :</strong> ${sujetInstance.niveau?.libelleLong}</g:if>
          <g:if
              test="${sujetInstance.matiereBcn?.libelleEdition}"><strong>» Matière :</strong> ${sujetInstance.matiereBcn?.libelleEdition}</g:if>
          <g:if
              test="${fieldValue(bean: sujetInstance, field: "dureeMinutes")}"><strong>» Durée :</strong> ${fieldValue(bean: sujetInstance, field: "dureeMinutes")}</g:if>
          <g:if test="${artefactHelper.partageArtefactCCActive && afficheFormulaire}">
            <strong>» Auteur :</strong> ${sujetInstance.proprietaire.prenom} ${sujetInstance.proprietaire.nom}
          </g:if>
          <g:if test="${artefactHelper.partageArtefactCCActive}">
          <strong>» Partagé :</strong> ${sujetInstance.estPartage() ? 'oui' : 'non'}
          </g:if>
        </p>

      </div>
    </g:each>
  </div>

</g:if>
<g:else>
  <div class="portal_pagination">
    <p class="nb_result">Aucun résultat</p>
  </div>
</g:else>

<g:render template="../importexport/export_dialog"/>
</body>
</html>