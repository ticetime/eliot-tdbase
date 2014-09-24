package org.lilie.services.eliot.tdbase.preferences

import org.lilie.services.eliot.tdbase.RoleApplicatif
import org.lilie.services.eliot.tice.annuaire.Personne
import org.lilie.services.eliot.tice.scolarite.Etablissement
import org.lilie.services.eliot.tice.scolarite.Fonction
import org.lilie.services.eliot.tice.scolarite.FonctionEnum
import org.lilie.services.eliot.tice.scolarite.ProfilScolariteService
import org.lilie.services.eliot.tice.utils.contract.Contract


/**
 * Service de gestion des préférences établissement
 * @author Franck Silvestre
 */
class PreferenceEtablissementService {

    static scope = "session"
    static proxy = true

    /**
     * Récupère l'objet correspondant aux préférences d'un établissement
     * @param personne la personne effectuant la demande
     * @param role le role applicatif de la personne effectuant la demande
     * @param etablissement
     * @return l'objet correspondant aux préférences de l'établissement
     */
    PreferenceEtablissement getPreferenceForEtablissement(Personne personne,
                                                          Etablissement etablissement,
                                                          RoleApplicatif role = null) {

        PreferenceEtablissement pref = PreferenceEtablissement.findByEtablissement(etablissement)
        if (!pref && role == RoleApplicatif.ADMINISTRATEUR) {
            pref = new PreferenceEtablissement(etablissement: etablissement,
                    lastUpdateAuteur: personne,
                    mappingFonctionRole: MappingFonctionRole.defaultMappingFonctionRole.toJsonString())
            pref.save(failOnError: true)
        }
        pref
    }

    /**
     * Récupère l'objet correspondant aux préférences d'un établissement
     * @param etablissement
     * @return l'objet correspondant aux préférences de l'établissement
     */
    PreferenceEtablissement getPreferenceForEtablissement(Etablissement etablissement) {

        PreferenceEtablissement pref = PreferenceEtablissement.findByEtablissement(etablissement)
        if (!pref) {
            throw new PreferenceEtablissementNotSetException()
        }
        pref
    }

    /**
     * Met à jour en base une préférence établissement
     * @param personne la personne effectuant la mise à jour
     * @param preferenceEtablissement la préférence établissement
     * @param roleApplicatif le rôle de la personne mettant à jour
     * @return la préférence établissement mise à jour
     */
    PreferenceEtablissement updatePreferenceEtablissement(Personne personne,
                                      PreferenceEtablissement preferenceEtablissement,
                                      RoleApplicatif roleApplicatif) {
        Contract.requires(roleApplicatif == RoleApplicatif.ADMINISTRATEUR)
        preferenceEtablissement.lastUpdateAuteur = personne
        preferenceEtablissement.lastUpdated = new Date()
        preferenceEtablissement.save(failOnError: true)
        preferenceEtablissement
    }

    //TODO : à coder reellement après WS fourni par OMT
    /**
     * Récupère la liste des fonctions administrables pour un établissement donné
     * @param etablissement l'établissement
     * @return la liste des fonctions
     */
    List<Fonction> getFonctionsForEtablissement(Etablissement etablissement) {
        [
                FonctionEnum.DIR.fonction,
                FonctionEnum.AL.fonction,
                FonctionEnum.ENS.fonction,
                FonctionEnum.DOC.fonction,
                FonctionEnum.ELEVE.fonction,
                FonctionEnum.PERS_REL_ELEVE.fonction,
                FonctionEnum.EDU.fonction,
                FonctionEnum.CTR.fonction
        ]
    }
}
