google.load("earth", "1", {"other_params": "sensor=false"});

function init() {
    google.earth.createInstance('map3d', initCB, failureCB);
}

function initCB(instance) {
    /*
     * Geocoding.
     */
    var geocoder = new google.maps.Geocoder();

    /*
     * Google Earth.
     */
    var ge = instance;
    ge.getWindow().setVisibility(true);

    /*
     * User interaction, with jQuery.
     */
    $(function() {
        $('#addressform').submit(function() {
            var address = $('#address').val();
            geocoder.geocode({address: address}, function(results, status) {
                if (status == google.maps.GeocoderStatus.OK) {
                    var latlng = results[0].geometry.location;
                    $('#lat').html(latlng.lat());
                    $('#lng').html(latlng.lng());
                    var lookAt = ge.createLookAt('');
                    lookAt.setLatitude(latlng.lat());
                    lookAt.setLongitude(latlng.lng());
                    lookAt.setTilt(60);  // degrees
                    lookAt.setRange(100 * 1000.0);  // km
                    ge.getView().setAbstractView(lookAt);
                } else {
                    alert('Geocode failed: ' + status);
                }
            });

            return false;
        });

        $('#dateform').submit(function() {
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
