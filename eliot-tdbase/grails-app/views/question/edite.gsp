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
  <r:require module="eliot-tice-tiny_mce"/>

  <r:script>
    $(document).ready(function() {
      $('#menu-item-contributions').addClass('actif');
    });
  </r:script>
  <r:script disposition="head">
    tinyMCE.init({
               // General options
               language:'fr',
               mode : "none",
               theme : "advanced",
               plugins : "pagebreak,style,layer,table,save,advhr,advimage,advlink,emotions,iespell,inlinepopups,insertdatetime,preview,media,searchreplace,print,contextmenu,paste,directionality,fullscreen,noneditable,visualchars,nonbreaking,xhtmlxtras,template",

               // Theme options
               theme_advanced_buttons1 : "bold,italic,underline,strikethrough,|,forecolor,backcolor,|,justifyleft,justifycenter,justifyright,justifyfull,|,formatselect,fontselect,fontsizeselect,|,preview",
               theme_advanced_buttons2 : "cut,copy,paste,pastetext,pasteword,|,search,replace,|,bullist,numlist,|,outdent,indent,blockquote,|,link,unlink,image,cleanup,help,code",
               theme_advanced_buttons3 : "tablecontrols,|,hr,removeformat,|,sub,sup,|,charmap,iespell,media,advhr",

               theme_advanced_toolbar_location : "top",
               theme_advanced_toolbar_align : "left",
               theme_advanced_statusbar_location : "bottom",
               theme_advanced_resizing : true
       });
  </r:script>
  <title>TDBase - Edition d'une question</title>
</head>

<body>

<div class="column span-22 last middle">
  <g:render template="/breadcrumps" plugin="eliot-tice-plugin" model="[liens: liens]"/>

  <g:hasErrors bean="${question}">
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

  <g:if test="${sujet}">
    <g:render template="/sujet/listeElements" model="[sujet:sujet]"/>
  </g:if>

  <g:form method="post" controller="question${question.type.code}">
    <div class="portal-form_container" style="width: 70%;margin-left: 15px;">
      <table>

        <tr>
          <td class="label">Titre:</td>
          <td>
            <input size="75" type="text" value="${question.titre}"
                   name="titre"/>
          </td>
        </tr>
        <tr>
          <td class="label">Type :</td>
          <td>
            ${question.type.nom}
          </td>
        </tr>
        <g:if test="${!question.id && sujet}">
          <tr>
          <td class="label">Mati&egrave;re :</td>
          <td>
            <g:select name="matiere.id" value="${sujet.matiere?.id}"
                      noSelection="${['null':'Sélectionner une matière...']}"
                      from="${matieres}"
                      optionKey="id"
                      optionValue="libelleLong"/>
          </td>
        </tr>
        <tr>
          <td class="label">Niveau :</td>
          <td>
            <g:select name="niveau.id" value="${sujet.niveau?.id}"
                      noSelection="${['null':'Sélectionner un niveau...']}"
                      from="${niveaux}"
                      optionKey="id"
                      optionValue="libelleLong"/>
          </td>
        </tr>
        </g:if>
        <g:else>
        <tr>
          <td class="label">Mati&egrave;re :</td>
          <td>
            <g:select name="matiere.id" value="${question.matiere?.id}"
                      noSelection="${['null':'Sélectionner une matière...']}"
                      from="${matieres}"
                      optionKey="id"
                      optionValue="libelleLong"/>
          </td>
        </tr>
        <tr>
          <td class="label">Niveau :</td>
          <td>
            <g:select name="niveau.id" value="${question.niveau?.id}"
                      noSelection="${['null':'Sélectionner un niveau...']}"
                      from="${niveaux}"
                      optionKey="id"
                      optionValue="libelleLong"/>
          </td>
        </tr>
          </g:else>
        <tr>
          <td class="label">Autonome&nbsp;:</td>
          <td>
            <g:checkBox name="estAutonome" title="Autonome"
                        checked="${question.estAutonome}"/>
          </td>
        </tr>
        <g:render
                template="/question/${question.type.code}/${question.type.code}Edition"
                model="[question:question]"/>
      </table>
    </div>
    <g:hiddenField name="id" value="${question.id}"/>
    <g:hiddenField name="type.id" value="${question.type.id}"/>

    <div class="form_actions" style="width: 70%;margin-left: 15px;">
      <g:link action="${lienRetour.action}"
              controller="${lienRetour.controller}"
              params="${lienRetour.params}">Annuler</g:link> |
      <g:if test="${sujet}">
        <g:hiddenField name="sujetId" value="${sujet.id}"/>
        <g:actionSubmit value="Enregistrer et insérer dans le sujet"
                        action="enregistreInsert"
                        title="Enregistrer et insérer dans le sujet"/>
      </g:if>
      <g:else>
        <g:actionSubmit value="Enregistrer"
                        action="enregistre"
                        title="Enregistrer"/>
      </g:else>
    </div>
  </g:form>
</div>

</body>
</html>