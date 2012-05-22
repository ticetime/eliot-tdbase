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

function initDragNDrop() {

    initWidgets();
    registerEventHandlers();

    function initWidgets() {
        //hide html tags
        $(".hotspotLabel").hide();
        $(".hotspotAttribute").hide();

        // style html tags
        $('[name="hotspotSupressButton"]').each(function () {
            $(this).addClass('hotspotSupressButton');
        });
        $('.hotspot').addClass('hotspotStyle');
        $('.hotspot').addClass('unHighlightedHotspot');

        // make hotspots draggable
        $(".hotspot").draggable({containment:'#theImage', stack:'div'});

        // make hotspots resizable
        $(".hotspot").resizable({containment:'#theImage', stack:'div', handles:"se", stop:function (event, ui) {
            onResize($(this), ui)
        }});

        resizeHotspots();
        positionHotspots();
        addHotpotIds();
    }

    function registerEventHandlers() {

        $(".hotspot").bind("dragstop", function () {
            onDragStop($(this));
        })
    }

    function positionHotspots() {
        $(".hotspot").each(function () {
            var offLeft = $(this).children('#offLeft').val();
            var offTop = $(this).children('#offTop').val();
            $(this).position({
                                 of:$("#theImage"), my:"left top", at:"left top",
                                 offset:offLeft + " " + offTop, collision:"none"
                             });
        });
    }

    function onDragStop(hotspot) {
        var hotspotId = hotspot.attr("id");

        var imageLeft = $('#theImage').position().left;
        var imageTop = $('#theImage').position().top;

        var hotspotLeft = $('#' + hotspotId).position().left - imageLeft;
        var hotspotTop = $('#' + hotspotId).position().top - imageTop;

        $("#" + hotspotId + ">input#offLeft").val(hotspotLeft);
        $("#" + hotspotId + ">input#offTop").val(hotspotTop);
    }

    function addHotpotIds() {
        $(".hotspot").each(function () {

            var id = $(this).children(".idField").val();

            $(this).append("<span class='hotspotId'>" + id + "</span>");

        });
    }

    function resizeHotspots(){

        $(".hotspot").each(function () {

            var width = $(this).children('#width').val();
            var height = $(this).children('#height').val();
            $(this).css("width", width);
            $(this).css("height", height);
        });

    }

    function onResize(hotSpot, ui) {
        var hotspotId = $(hotSpot).attr("id");
        $("#" + hotspotId + ">input#width").val(ui.size.width);
        $("#" + hotspotId + ">input#height").val(ui.size.height);
    }
}