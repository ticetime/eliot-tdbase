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
      $('#menu-item-seances').addClass('actif');
      $('.supprime').click(function() {
        return confirm('Êtes vous sur de vouloir supprimer la séance et toutes les copies associées ?');
      })
    });
  </r:script>
  <title>TDBase - Liste des séances</title>
</head>

<body>
<div class="column span-22 last middle">
  <g:render template="/breadcrumps" model="[liens: liens]"/>

  <g:if test="${seances}">
    <div class="portal_pagination">
      ${seances.totalCount} résultat(s) <g:paginate total="${seances.totalCount}"></g:paginate>
    </div>

    <div class="portal-default_table">
      <table>
        <thead>
        <tr>
          <th>Groupe</th>
          <th>Résultats</th>
          <th>Matière</th>
          <th>Sujet</th>
          <th>Début</th>
          <th>Fin</th>
          <th>Modifier</th>
          <th>Supprimer</th>
        </tr>
        </thead>

        <tbody>
        <g:each in="${seances}" status="i" var="seance">
          <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
            <td>
              ${seance.groupeLibelle}
            </td>
            <td>
              <g:link action="listeResultats" controller="seance"
                      id="${seance.id}">
              <img border="0"
                     src="/eliot-tdbase/images/eliot/magglass-btn.gif"
                     width="16" height="16"/>
              </g:link>
            </td>
            <td>
              ${seance.matiere?.libelleLong}
            </td>
            <td>
              ${seance.sujet.titre}
            </td>
            <td>
              ${seance.dateDebut.format('dd/MM/yy HH:mm')}
            </td>
            <td>
              ${seance.dateFin.format('dd/MM/yy HH:mm')}
            </td>
            <td>
              <g:link action="edite" controller="seance"
                      id="${seance.id}">
                <img border="0"
                     src="/eliot-tdbase/images/eliot/write-btn.gif"
                     width="18" height="16"/>
              </g:link>
            </td>
            <td>
              <g:link action="supprime" controller="seance"
                      id="${seance.id}" class="supprime">
                <img border="0"
                     src="/eliot-tdbase/images/eliot/trashcan-btn.gif"
                     width="20" height="19"/>
                </g:link>
            </td>
          </tr>
        </g:each>
        </tbody>
      </table>
    </div>
  </g:if>
  <g:else>
     <div class="portal_pagination">
      Aucune séance
    </div>
  </g:else>
</div>

</body>
</html>