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
      $('#menu-item-contributions').addClass('actif');
    });
  </r:script>
  <title>TDBase - Recherche de contributions</title>
</head>

<body>
<div class="column span-22 last middle">
  <g:render template="/breadcrumps" model="[liens: liens]"/>

  <g:if test="${sujet}">
    <g:render template="/sujet/listeElements" model="[sujet:sujet]"/>
  </g:if>

    <form>
      <div class="portal-form_container">
        <table>
          <tr>
            <td class="label">
              Titre :
            </td>
            <td>
              <g:textField name="patternTitre" title="titre" value="${rechercheCommand.patternTitre}"/>
            </td>
            <td width="20"/>
            <td class="label">Type :
            </td>
            <td>
              <g:select name="typeId" value="${rechercheCommand.typeId}"
                      noSelection="${['null':'Tous']}"
                      from="${typesQuestion}"
                      optionKey="id"
                      optionValue="nom" />
            </td>
          </tr>
          <tr>
            <td class="label">
              Contenu :
            </td>
            <td>
              <g:textField name="patternSpecification" title="titre" value="${rechercheCommand.patternSpecification}"/>
            </td>
            <td width="20"/>
            <td class="label">Matière :
            </td>
            <td>
               <g:select name="matiereId" value="${rechercheCommand.matiereId}"
                      noSelection="${['null':'Toutes']}"
                      from="${matieres}"
                      optionKey="id"
                      optionValue="libelleLong" />
            </td>
          </tr>
          <tr>
            <td class="label">Autonome :
            </td>
            <td>
              <g:checkBox name="estAutonome" title="Autonome" checked="${rechercheCommand.estAutonome}"/>
            </td>
            <td width="20"/>
            <td class="label">Niveau :
            </td>
            <td>
              <g:select name="niveauId" value="${rechercheCommand.niveauId}"
                      noSelection="${['null':'Tous']}"
                      from="${niveaux}"
                      optionKey="id"
                      optionValue="libelleLong" />
            </td>
          </tr>

        </table>
      </div>

      <div class="form_actions">
        <g:hiddenField name="sujetId" value="${sujet?.id}"/>
        <g:actionSubmit value="Rechercher" action="recherche"
                        title="Lancer la recherche"/>
      </div>
    </form>


  <g:if test="${questions}">
    <div class="portal_pagination">
      ${questions.totalCount} résultat(s) <g:paginate total="${questions.totalCount}" params="${rechercheCommand?.toParams()}"></g:paginate>
    </div>

    <div class="portal-default_table">
      <table>
        <thead>
        <tr>
          <th>Titre</th>
          <th>Niveau</th>
          <th>Matière</th>
          <th>Autonome</th>
          <th>Détail</th>
          <th>Modifier</th>
          <th>Mise à jour le</th>
        </tr>
        </thead>

        <tbody>
        <g:each in="${questions}" status="i" var="questionInstance">
          <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
            <td>
              ${fieldValue(bean: questionInstance, field: "titre")}
            </td>
            <td>
              ${questionInstance.niveau?.libelleLong}
            </td>
            <td>
              ${questionInstance.matiere?.libelleLong}
            </td>
            <td>
              ${questionInstance.estAutonome ? 'oui' : 'non'}
            </td>
            <td>
              <g:link action="detail" controller="question${questionInstance.type.code}"
                      id="${questionInstance.id}" params="[sujetId:sujet?.id]">
                <img border="0"
                     src="/eliot-tdbase/images/eliot/magglass-btn.gif"
                     width="16" height="16"/>
              </g:link>
            </td>
            <td>
              <g:link action="edite" controller="question${questionInstance.type.code}"
                      id="${questionInstance.id}" params="[sujetId:sujet?.id]">
                <img border="0"
                     src="/eliot-tdbase/images/eliot/write-btn.gif"
                     width="18" height="16"/>
              </g:link>
            </td>
            <td>
              ${questionInstance.lastUpdated?.format('dd/MM/yy HH:mm')}
            </td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
  </g:if>
  <g:else>
     <div class="portal_pagination">
      Aucun résultat
    </div>
  </g:else>
</div>

</body>
</html>