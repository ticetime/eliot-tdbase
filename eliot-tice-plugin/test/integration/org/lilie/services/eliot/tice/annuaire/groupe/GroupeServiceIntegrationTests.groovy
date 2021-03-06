/*
 * Copyright © FYLAB and the Conseil Régional d'Île-de-France, 2009
 *  This file is part of L'Interface Libre et Interactive de l'Enseignement (Lilie).
 *
 *  Lilie is free software. You can redistribute it and/or modify since
 *  you respect the terms of either (at least one of the both license) :
 *  - under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  - the CeCILL-C as published by CeCILL-C; either version 1 of the
 *  License, or any later version
 *
 *  There are special exceptions to the terms and conditions of the
 *  licenses as they are applied to this software. View the full text of
 *  the exception in file LICENSE.txt in the directory of this software
 *  distribution.
 *
 *  Lilie is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  Licenses for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  and the CeCILL-C along with Lilie. If not, see :
 *   <http://www.gnu.org/licenses/> and
 *   <http://www.cecill.info/licences.fr.html>.
 */

package org.lilie.services.eliot.tice.annuaire.groupe

import org.lilie.services.eliot.tice.annuaire.Personne
import org.lilie.services.eliot.tice.scolarite.Etablissement
import org.lilie.services.eliot.tice.scolarite.Fonction
import org.lilie.services.eliot.tice.scolarite.FonctionEnum
import org.lilie.services.eliot.tice.scolarite.FonctionService
import org.lilie.services.eliot.tice.scolarite.ProprietesScolarite
import org.lilie.services.eliot.tice.scolarite.StructureEnseignement
import org.lilie.services.eliot.tice.utils.BootstrapService

/**
 * @author John Tranier
 */
class GroupeServiceIntegrationTests extends GroovyTestCase {

    GroupeService groupeService
    BootstrapService bootstrapService
    FonctionService fonctionService

    void setUp() {
        bootstrapService.bootstrapJeuDeTestDevDemo()
    }


    void testFindAllPersonneInGroupeScolarite() {
        given: "Une classe"
        StructureEnseignement classe = bootstrapService.classe1ere

        and: "Le groupe scolarité des élèves de la classe"
        ProprietesScolarite groupeScolarite =
                ProprietesScolarite.withCriteria(uniqueResult: true) {
                    eq('structureEnseignement', classe)
                    eq('fonction', fonctionService.fonctionEleve())
                    isNull('responsableStructureEnseignement')
                }

        expect:
        assertEquals(
                "Le nombre d'élèves dans la classe est incorrect",
                2,
                groupeService.findAllPersonneInGroupeScolarite(
                        groupeScolarite
                ).size()
        )

        given: "Le groupe scolarité des parents de la classe"
        groupeScolarite =
        ProprietesScolarite.withCriteria(uniqueResult: true) {
            eq('structureEnseignement', classe)
            eq('fonction', fonctionService.fonctionResponsableEleve())
            isNull('responsableStructureEnseignement')
        }

        expect:
        assertEquals(
                "Le nombre de parents dans la classe est incorrect",
                1,
                groupeService.findAllPersonneInGroupeScolarite(
                        groupeScolarite
                ).size()
        )
    }

    void testFindAllGroupeScolariteForPersonne() {
        given:
        Personne personne = bootstrapService.eleve1

        expect:
        List<ProprietesScolarite> groupeScolariteList = groupeService.findAllGroupeScolariteForPersonne(personne)
        assertEquals(
                "Le nombre de groupes est incorrect",
                4,
                groupeScolariteList.size()
        )

        given:
        personne = bootstrapService.enseignant1

        expect:
        groupeScolariteList = groupeService.findAllGroupeScolariteForPersonne(personne)
        assertEquals(
                "Le nombre de groupes est incorrect",
                8,
                groupeScolariteList.size()
        )

        given:
        personne = bootstrapService.parent1

        expect:
        groupeScolariteList = groupeService.findAllGroupeScolariteForPersonne(personne)
        assertEquals(
                "Le nombre de groupes est incorrect",
                5,
                groupeScolariteList.size()
        )

        given:
        personne = bootstrapService.persDirection1

        expect:
        groupeScolariteList = groupeService.findAllGroupeScolariteForPersonne(personne)
        assertEquals(
                "Le nombre de groupes est incorrect",
                2,
                groupeScolariteList.size()
        )
    }

    void testFindAllGroupeEntForPersonne() {
        expect:
        assertEquals(
                [],
                groupeService.findAllGroupeEntForPersonne(bootstrapService.superAdmin1)
        )
        assertEquals(
                [bootstrapService.groupeEntLycee]*.nom,
                groupeService.findAllGroupeEntForPersonne(bootstrapService.eleve1)*.nom
        )
        assertEquals(
                [bootstrapService.groupeEntLycee]*.nom,
                groupeService.findAllGroupeEntForPersonne(bootstrapService.eleve2)*.nom
        )
        assertEquals(
                [
                        bootstrapService.groupeEntLycee,
                        bootstrapService.groupeEntCollege
                ]*.nom.sort(),
                groupeService.findAllGroupeEntForPersonne(
                        bootstrapService.enseignant1
                )*.nom.sort()
        )
        assertEquals(
                [
                        bootstrapService.groupeEntCollege
                ]*.nom as Set,
                groupeService.findAllGroupeEntForPersonne(
                        bootstrapService.enseignant2
                )*.nom as Set
        )
        assertEquals(
                [
                        bootstrapService.groupeEntCollege,
                        bootstrapService.groupeEntWithParent
                ]*.nom as Set,
                groupeService.findAllGroupeEntForPersonne(
                        bootstrapService.persDirection1
                )*.nom as Set
        )

    }

    void testFindAllGroupeEntInEtablissementForPersonne() {
        expect:
        assertEquals(
                [],
                groupeService.findAllGroupeEntInEtablissementListForPersonne(
                        bootstrapService.superAdmin1,
                        [bootstrapService.etablissementCollege]
                )
        )
        assertEquals(
                [bootstrapService.groupeEntLycee]*.nom,
                groupeService.findAllGroupeEntInEtablissementListForPersonne(
                        bootstrapService.eleve1,
                        [bootstrapService.etablissementLycee]
                )*.nom
        )
        assertEquals(
                [],
                groupeService.findAllGroupeEntInEtablissementListForPersonne(
                        bootstrapService.eleve1,
                        [bootstrapService.etablissementCollege]
                )
        )
        assertEquals(
                [bootstrapService.groupeEntLycee]*.nom,
                groupeService.findAllGroupeEntInEtablissementListForPersonne(
                        bootstrapService.eleve2,
                        [bootstrapService.etablissementLycee]
                )*.nom
        )
        assertEquals(
                [
                        bootstrapService.groupeEntLycee,
                        bootstrapService.groupeEntCollege
                ]*.nom.sort(),
                groupeService.findAllGroupeEntInEtablissementListForPersonne(
                        bootstrapService.enseignant1,
                        [
                                bootstrapService.etablissementLycee,
                                bootstrapService.etablissementCollege
                        ]
                )*.nom.sort()
        )
        assertEquals(
                [
                        bootstrapService.groupeEntLycee
                ]*.nom.sort(),
                groupeService.findAllGroupeEntInEtablissementListForPersonne(
                        bootstrapService.enseignant1,
                        [
                                bootstrapService.etablissementLycee
                        ]
                )*.nom.sort()
        )
    }

    void testFindAllPersonneForGroupeEntAndFonctionIn() {
        given:
        GroupeEnt groupeEnt = bootstrapService.groupeEntLycee
        List<Fonction> fonctionList = [
                FonctionEnum.ELEVE.fonction
        ]

        expect:
        assertEquals(
                [
                        bootstrapService.eleve1,
                        bootstrapService.eleve2,
                        bootstrapService.eleve3
                ]*.nomAffichage as Set,
                groupeService.findAllPersonneForGroupeEntAndFonctionIn(
                        groupeEnt,
                        fonctionList
                )*.nomAffichage as Set
        )

        given:
        groupeEnt = bootstrapService.groupeEntLycee
        fonctionList = [
                FonctionEnum.ELEVE.fonction,
                FonctionEnum.ENS.fonction
        ]

        expect:
        assertEquals(
                [
                        bootstrapService.eleve1,
                        bootstrapService.eleve2,
                        bootstrapService.eleve3,
                        bootstrapService.enseignant1
                ]*.nomAffichage as Set,
                groupeService.findAllPersonneForGroupeEntAndFonctionIn(
                        groupeEnt,
                        fonctionList
                )*.nomAffichage as Set
        )

        given:
        groupeEnt = bootstrapService.groupeEntLycee
        fonctionList = [
                FonctionEnum.ENS.fonction
        ]

        expect:
        assertEquals(
                [
                        bootstrapService.enseignant1
                ]*.id as Set,
                groupeService.findAllPersonneForGroupeEntAndFonctionIn(
                        groupeEnt,
                        fonctionList
                )*.id as Set
        )

        given:
        groupeEnt = bootstrapService.groupeEntCollege
        fonctionList = [
                FonctionEnum.ELEVE.fonction
        ]

        expect:
        assertEquals(
                [],
                groupeService.findAllPersonneForGroupeEntAndFonctionIn(
                        groupeEnt,
                        fonctionList
                )
        )

        given:
        groupeEnt = bootstrapService.groupeEntCollege
        fonctionList = [
                FonctionEnum.ENS.fonction
        ]

        expect:
        assertEquals(
                [
                        bootstrapService.enseignant1,
                        bootstrapService.enseignant2
                ]*.id as Set,
                groupeService.findAllPersonneForGroupeEntAndFonctionIn(
                        groupeEnt,
                        fonctionList
                )*.id as Set
        )

        given:
        groupeEnt = bootstrapService.groupeEntWithParent
        fonctionList = [
                FonctionEnum.DIR.fonction,
                FonctionEnum.PERS_REL_ELEVE.fonction
        ]

        expect:
        assertEquals(
                [
                        bootstrapService.persDirection1,
                        bootstrapService.parent1
                ]*.id as Set,
                groupeService.findAllPersonneForGroupeEntAndFonctionIn(
                        groupeEnt,
                        fonctionList
                )*.id as Set
        )

        given:
        groupeEnt = bootstrapService.groupeEntWithParent
        fonctionList = [
                FonctionEnum.PERS_REL_ELEVE.fonction
        ]

        expect:
        assertEquals(
                [
                        bootstrapService.parent1
                ]*.id as Set,
                groupeService.findAllPersonneForGroupeEntAndFonctionIn(
                        groupeEnt,
                        fonctionList
                )*.id as Set
        )
    }

    void testRechercheGroupeScolarite() {
        given:
        Personne enseignant = bootstrapService.enseignant1
        Etablissement etablissement = bootstrapService.etablissementLycee
        Fonction fonction = fonctionService.fonctionEleve()

        and:
        Map mockRestResult = [
                groupes       : [
                        [
                                id             : 1,
                                'nom-affichage': 'groupe 1',
                                'type': 'SCOLARITE'
                        ],
                        [
                                id             : 2,
                                'nom-affichage': 'groupe 2',
                                'type': 'SCOLARITE'
                        ]
                ],
                'nombre-total': 2
        ]

        and:
        RechercheGroupeRestService rechercheGroupeRestService = [
                rechercheGroupeList: {
                    Personne personne,
                    RechercheGroupeCritere critere,
                    GroupeType groupeType,
                    String codePorteur ->

                        return mockRestResult
                }
        ] as RechercheGroupeRestService
        groupeService.rechercheGroupeRestService = rechercheGroupeRestService

        expect:
        RechercheGroupeResultat resultat =
                groupeService.rechercheGroupeScolarite(
                        enseignant,
                        new RechercheGroupeCritere(
                                etablissement: etablissement,
                                fonction: fonction
                        )
                )

        assertEquals(
                2,
                resultat.nombreTotal
        )
        assertEquals(
                2,
                resultat.groupes.size()
        )
    }

    void testRechercheGroupeEnt() {
        given:
        Personne enseignant = bootstrapService.enseignant1
        Etablissement etablissement = bootstrapService.etablissementLycee
        Fonction fonction = fonctionService.fonctionEleve()

        and:
        Map mockRestResult = [
                groupes       : [
                        [
                                id             : 1,
                                'nom-affichage': 'groupe 1',
                                'type'         : 'ENT'
                        ],
                        [
                                id             : 2,
                                'nom-affichage': 'groupe 2',
                                'type'         : 'ENT'
                        ]
                ],
                'nombre-total': 2
        ]

        and:
        RechercheGroupeRestService rechercheGroupeRestService = [
                rechercheGroupeList: {
                    Personne personne,
                    RechercheGroupeCritere critere,
                    GroupeType groupeType,
                    String codePorteur ->

                        return mockRestResult
                }
        ] as RechercheGroupeRestService
        groupeService.rechercheGroupeRestService = rechercheGroupeRestService

        expect:
        RechercheGroupeResultat resultat =
                groupeService.rechercheGroupeEnt(
                        enseignant,
                        new RechercheGroupeCritere(
                                etablissement: etablissement,
                                fonction: fonction
                        )
                )

        assertEquals(
                2,
                resultat.nombreTotal
        )
        assertEquals(
                2,
                resultat.groupes.size()
        )
    }
}
