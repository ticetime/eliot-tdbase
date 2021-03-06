package org.lilie.services.eliot.tdbase.preferences

import org.lilie.services.eliot.tdbase.securite.RoleApplicatif
import org.lilie.services.eliot.tdbase.webservices.rest.client.ScolariteRestService
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

  ProfilScolariteService profilScolariteService
  ScolariteRestService scolariteRestService

  /**
   * Récupère l'objet correspondant aux préférences d'un établissement
   * @param personne la personne effectuant la demande
   * @param role le role applicatif de la personne effectuant la demande
   * @param etablissement
   * @return l'objet correspondant aux préférences de l'établissement
   */
  PreferenceEtablissement getPreferenceForEtablissement(Personne personne,
                                                        Etablissement etablissement) {

    PreferenceEtablissement pref =
        PreferenceEtablissement.findByEtablissement(etablissement)

    if (!pref) {
      pref = new PreferenceEtablissement(
          etablissement: etablissement,
          lastUpdateAuteur: personne,
          mappingFonctionRole: MappingFonctionRole.defaultMappingFonctionRole.toJsonString())
      pref.save(failOnError: true)
    }
    pref
  }

  /**
   * Récupère le mapping fonction rôle d'un établissement
   * @param personne la personne effectuant la demande
   * @param etablissement l'établissement
   * @return le mapping fonction role
   */
  MappingFonctionRole getMappingFonctionRoleForEtablissement(Personne personne,
                                                             Etablissement etablissement) {
    getPreferenceForEtablissement(personne, etablissement).mappingFonctionRoleAsMap()
  }

  /**
   * Met à jour en base une préférence établissement
   * @param personne la personne effectuant la mise à jour
   * @param preferenceEtablissement la préférence établissement
   * @param roleApplicatif le rôle de la personne mettant à jour
   * @return la préférence établissement mise à jour
   */
  PreferenceEtablissement updatePreferenceEtablissement(Personne personne,
                                                        PreferenceEtablissement preferenceEtablissement) {
    Contract.requires(profilScolariteService.personneEstPersonnelDirectionForEtablissement(personne, preferenceEtablissement.etablissement)
        || profilScolariteService.personneEstAdministrateurLocalForEtablissement(personne, preferenceEtablissement.etablissement),
        "requires_personne_est_administrateur_local_ou_personne_de_direction_pour_etablissement")
    preferenceEtablissement.lastUpdateAuteur = personne
    preferenceEtablissement.lastUpdated = new Date()
    preferenceEtablissement.save(failOnError: true)
    preferenceEtablissement
  }

  /**
   * Reset toutes les préférences établissement
   * @param personne la personne effectuant le reset
   */
  int resetAllPreferencesEtablissement(Personne personne) {
    Contract.requires(profilScolariteService.personneEstAdministrateurCentral(personne), "requires_personne_est_administrateur_central")
    def query = PreferenceEtablissement.where {
      etablissement != null
    }
    query.deleteAll()
  }

  /**
   * Récupère la liste des fonctions administrables pour un établissement donné
   * @param etablissement l'établissement
   * @return la liste des fonctions
   */
  List<Fonction> getFonctionsForEtablissement(Etablissement etablissement) {
    def res = null
    try {
      res = scolariteRestService.findFonctionsForEtablissement(etablissement.id).collect {
        FonctionEnum.valueOf(it.code).fonction
      }
    } catch (Exception e) {
      log.error(e.message)
    }
    if (!res) {
      res = [
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
    res
  }

  /*
   * @return la liste des fonctions associées au rôle apprenant sur l'établissement courant
   */

  List<Fonction> getFonctionListForRoleApprenant(Personne personne,
                                                 Etablissement etablissement) {
    return getFonctionListForRole(
        personne,
        etablissement,
        RoleApplicatif.ELEVE
    )
  }

  List<Fonction> getFonctionListForRoleFormateur(Personne personne,
                                                 Etablissement etablissement) {
    return getFonctionListForRole(
        personne,
        etablissement,
        RoleApplicatif.ENSEIGNANT
    )
  }


  List<Fonction> getFonctionListForRole(Personne personne,
                                        Etablissement etablissement,
                                        RoleApplicatif roleApplicatif) {
    return getPreferenceForEtablissement(
        personne,
        etablissement
    ).mappingFonctionRoleAsMap().getFonctionEnumListForRole(
        roleApplicatif
    ).collect {
      it.fonction
    }.sort { Fonction f -> f.libelle }
  }
}

