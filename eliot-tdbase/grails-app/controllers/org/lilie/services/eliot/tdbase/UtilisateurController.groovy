package org.lilie.services.eliot.tdbase

import org.lilie.services.eliot.tdbase.preferences.PreferencePersonne
import org.lilie.services.eliot.tdbase.preferences.PreferencePersonneService
import org.lilie.services.eliot.tice.annuaire.Personne
import org.lilie.services.eliot.tice.utils.BreadcrumpsService

class UtilisateurController {

    PreferencePersonneService preferencePersonneService
    BreadcrumpsService breadcrumpsServiceProxy

    /**
     * Rend la page des préférence de l'utilisateur connecté
     */
    def preference() {
        Personne personne = authenticatedPersonne
        breadcrumpsServiceProxy.manageBreadcrumps(params,
                message(code: "utilisateur.preference.index.title"))
        PreferencePersonne preferencePersonne = preferencePersonneService.getPreferenceForPersonne(personne)
        [liens             : breadcrumpsServiceProxy.liens,
         preferencePersonne: preferencePersonne]
    }

    /**
     * Enregistre les préférences de l'utilisateur
     * @param preferencePersonne la préférence personne à enregistrer
     */
    def enregistrePreference() {
        PreferencePersonne preferencePersonne = PreferencePersonne.get(params.id)
        preferencePersonne.notificationOnCreationSeance = params.notificationOnCreationSeance ? true : false
        preferencePersonne.notificationOnPublicationResultats = params.notificationOnPublicationResultats ? true : false
        preferencePersonneService.updatePreferencePersonne(preferencePersonne, authenticatedPersonne)
        flash.messageTextesCode = "utilisateur.preference.save.success"
        redirect(controller: "utilisateur", action: "preference", params: [bcInit: true])
    }

}