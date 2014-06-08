google.load("earth", "1", {"other_params": "sensor=false"});

function init() {
    google.earth.createInstance('map3d', initCB, failureCB);
}

var datePat = new RegExp("(\\d{4})-(\\d{2})-(\\d{2}) (\\d{2})");
var weatherLoaderToken = 0;
var previousStations = null;
var previousPlacemark = null;
var previousUSState = null;
var displayStyle = "map";  // map or info
var lat = null, lng = null;

function isDate(s) { return datePat.test(s); }
function nextDate(s) {
    var match = datePat.exec(s),
        year = parseInt(match[1]),
        month = parseInt(match[2]) - 1, // Convert month to 0-indexed.
        day = parseInt(match[3]),
        hour = parseInt(match[4]),
        ts = Date.UTC(year, month, day, hour);

    // Add one hour.
    ts += 60 * 60 * 1000;

    function toStr(n, length) {
        var nStr = '' + n;
        return new Array(length - nStr.length + 1).join('0') + nStr;
    }

    var d = new Date(ts);

    return ('' + toStr(d.getUTCFullYear(), 4)
            + '-' + toStr(d.getUTCMonth() + 1, 2) // Convert month.
            + '-' + toStr(d.getUTCDate(), 2)
            + ' ' + toStr(d.getUTCHours(), 2));
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
                    lat = latlng.lat();
                    lng = latlng.lng();

                    $('#lat').html(lat);
                    $('#lng').html(lng);

                    var lookAt = ge.createLookAt('');
                    lookAt.setLatitude(lat);
                    lookAt.setLongitude(lng);
                    lookAt.setTilt(60);  // degrees
                    lookAt.setRange(100 * 1000.0);  // km
                    ge.getView().setAbstractView(lookAt);

                    setPlacemark(ge, lat, lng);
                    showUSState(ge, lat, lng);
                } else {
                    alert('Geocode failed: ' + status);
                }
            });

            return false;
        });

        $('#dateform').submit(function() {
            // Cancel previous load in progress.
            weatherLoaderToken++;

            var theDate = $('#dateinput').val();
            if (!isDate(theDate)) {
                alert("Try a date formatted like '1978-10-01 10'");
                return false;
            }

            loadWeatherForDate(weatherLoaderToken, theDate, ge, loadWeatherCB);
            return false;
        });

        $('#stopform').submit(function() {
            // Cancel previous load in progress.
            weatherLoaderToken++;
            return false;
        });

        $('#displayform').submit(function() {
            if (displayStyle == 'map') {
                $('#map3d').hide();
                $('#info').show();
                $('#displaybutton').text('map');
                displayStyle = 'info';
            } else {
                $('#map3d').show();
                $('#info').hide();
                $('#displaybutton').text('info');
                displayStyle = 'map';
            }

            return false;
        })
    });
}

function failureCB(errorCode) {
    alert("Error!: " + errorCode);
}

/*
 * Executed when the weather has been loaded for a date.
 */
function loadWeatherCB(token, theDate, ge) {
    if (weatherLoaderToken == token) {
        setTimeout(function() {
            loadWeatherForDate(token, nextDate(theDate), ge, loadWeatherCB);
        }, 1000);
    }
}

function loadWeatherForDate(token, theDate, ge, callback) {
    var kmlUrl = location.href + 'samples.kml?date=' + theDate;
    google.earth.fetchKml(ge, kmlUrl, function (kmlObject) {
        if (kmlObject) {
            $('#date').html(theDate + ':00 UTC');
            var features = ge.getFeatures();
            if (previousStations) features.removeChild(previousStations);
            ge.getFeatures().appendChild(kmlObject);
            previousStations = kmlObject;
            callback(token, theDate, ge);
        } else {
            // Defer alert to prevent deadlock in some browsers.
            setTimeout(function () {
                alert("Error fetching historical data");
            }, 0);
        }
    });

    /*
     * Get full info for nearest observation.
     */
    var apiUrl = (
        location.href
        + 'info?date=' + theDate
        + '&lat=' + lat
        + '&lng=' + lng);

    $.ajax({
        url: apiUrl,
        error: function () {
            alert("error retrieving weather observation");
        },
        success: function (observation) {
            $('#info').html('<pre>' + observation + '</pre>');
        }
    });
}

/*
 * Put a placemark at the current geolocation.
 */
function setPlacemark(ge, lat, lng) {
    var features = ge.getFeatures();
    if (previousPlacemark) {
        features.removeChild(previousPlacemark);
    }

    previousPlacemark = ge.createPlacemark('');
    var point = ge.createPoint('');
    point.setLatLng(lat, lng);
    previousPlacemark.setGeometry(point);
    features.appendChild(previousPlacemark);
}

/*
 * Look up which US state a lat/lng is in, and display its outline.
 */
function showUSState(ge, lat, lng) {
    var apiUrl = location.href + 'us-state?lat=' + lat + '&lng=' + lng;
    $.ajax({
        url: apiUrl,
        success: function (stateName, status) {
            $('#state-name').html(stateName);

            var kmlUrl = (
                location.href
                + 'static/states-kml/' + stateName.toLowerCase() + '.kml');

            google.earth.fetchKml(ge, kmlUrl, function (kmlObject) {
                if (kmlObject) {
                    var features = ge.getFeatures();
                    if (previousUSState) features.removeChild(previousUSState);
                    ge.getFeatures().appendChild(kmlObject);
                    previousUSState = kmlObject;
                }
            });
        }
    });
}

google.setOnLoadCallback(init);
