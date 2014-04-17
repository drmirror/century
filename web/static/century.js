var ge;
google.load("earth", "1", {"other_params": "sensor=false"});

function init() {
    google.earth.createInstance('map3d', initCB, failureCB);
}

function initCB(instance) {
    ge = instance;
    ge.getWindow().setVisibility(true);

    var link = ge.createLink('');
    var href = location.href + '/kml.kml';
    link.setHref(href);

    var networkLink = ge.createNetworkLink('');
    // Sets the link, refreshVisibility, and flyToView.
    networkLink.set(link, true, true);

    ge.getFeatures().appendChild(networkLink);

    /* DOM-dependent stuff. */
    $(function() {
        $('#dateform').submit(function(form) {
            var theDate = $('#dateinput').val();
            loadWeatherForDate(theDate);
            return false;
        });
    });
}

function failureCB(errorCode) {
    alert("Error!: " + errorCode);
}

function loadWeatherForDate(theDate) {
    // TODO
}

google.setOnLoadCallback(init);
