import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.lilie.services.eliot.tdbase.annuaire.DefaultTDBaseRoleUtilisateurService
import org.lilie.services.eliot.tdbase.emaeval.emawsconnector.ReferentielMarshaller
import org.lilie.services.eliot.tdbase.securite.SecuriteSessionService
import org.lilie.services.eliot.tdbase.xml.transformation.MoodleQuizTransformationHelper
import org.lilie.services.eliot.tdbase.xml.transformation.MoodleQuizTransformer
import org.lilie.services.eliot.tice.annuaire.impl.DefaultRoleUtilisateurService
import org.lilie.services.eliot.tice.annuaire.impl.DefaultUtilisateurService
import org.lilie.services.eliot.tice.annuaire.impl.LilieRoleUtilisateurService
import org.lilie.services.eliot.tice.annuaire.impl.LilieUtilisateurService
import org.lilie.services.eliot.tice.migrations.LiquibaseWrapper
import org.lilie.services.eliot.tice.securite.rbac.EliotTiceUserDetailsService
import org.lilie.services.eliot.tice.utils.EliotEditeurRegistrar

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

class EliotTdbasePluginGrailsPlugin {
    // the group id
    def groupId = "org.lilie.services.eliot"
    // the plugin version
    def version = "3.0.0-SNAPSHOT"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.0.1 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def title = "Eliot TD Base  Plugin" // Headline display name of the plugin
    def author = "Franck Silvestre - Ticetime"
    def authorEmail = ""
    def description = '''\
  Plugin contenant les services métiers relatifs à la gestion des TD"
  '''

    def doWithSpring = {
        userDetailsService(EliotTiceUserDetailsService) {
            utilisateurService = ref("utilisateurService")
            roleUtilisateurService = ref("roleUtilisateurService")
        }

        def conf = ConfigurationHolder.config

        // configure la gestion de l'annuaire
        //
        if (conf.eliot.portail.lilie) {
            utilisateurService(LilieUtilisateurService) {
                springSecurityService = ref("springSecurityService")
            }
        } else {
            utilisateurService(DefaultUtilisateurService) {
                springSecurityService = ref("springSecurityService")
            }
        }


        roleUtilisateurService(DefaultTDBaseRoleUtilisateurService) {
            securiteSessionServiceProxy = ref("securiteSessionServiceProxy")
        }


        // bean orientés sécurité

        //bean orientés gestion des formulaires
        customPropertyEditorRegistrar(EliotEditeurRegistrar)

        // beans pour la migration des données
        liquibase(LiquibaseWrapper) {
            dataSource = ref("dataSource")
            changeLog = "classpath:migrations/changelog-tice-dbmigration-all.xml"
        }

        // bean pour l'import moodle xml
        xmlTransformationHelper(MoodleQuizTransformationHelper) {
            dataStore = ref("dataStore")
        }

        moodleQuizTransformer(MoodleQuizTransformer) {
            xmlTransformationHelper = ref("xmlTransformationHelper")
        }

        // bean pour convertir les référentiels EmaEval en référentiels Eliot
        emaEvalReferentielMarshaller(ReferentielMarshaller)
    }
}
