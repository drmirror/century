import datetime
import time

from lxml import etree


def cent_to_fahr(c):
    return 32 + 9 * c / 5


placemark_style = etree.Element('Style')
placemark_style.set('id', 'stationMark')
icon_style = etree.SubElement(placemark_style, 'IconStyle')
icon = etree.SubElement(icon_style, 'Icon')
href = etree.SubElement(icon, 'href')
href.text = '/static/icon.png'


def stations(dt, db, prettyprint=False):
    """The stations active at one time.

    Takes a datetime and pymongo database. Returns list of KML Placemarks.
    """
    assert dt.minute == 0
    assert dt.second == 0
    assert dt.microsecond == 0
    assert dt.tzinfo is None

    next_hour = dt + datetime.timedelta(hours=1)

    root = etree.Element('kml')
    root.set('xmlns', 'http://www.opengis.net/kml/2.2')
    kdoc = etree.SubElement(root, 'Document')
    etree.SubElement(kdoc, 'name').text = 'stations'
    etree.SubElement(kdoc, 'visibility').text = '1'
    kdoc.append(placemark_style)

    # Positions of stations active in this hour. Needs index on 'ts'.
    pipeline = [{
        '$match': {
            'ts': {'$gte': dt, '$lt': next_hour},
            # Valid samples.
            'airTemperature.quality': '1'
        }
    }, {
        '$group': {
            '_id': '$st',
            'position': {'$first': '$position'},
            # Could average the temperatures, probably not worthwhile.
            'airTemperature': {'$first': '$airTemperature'},
        }
    }]

    start = time.time()
    cursor = db.data.aggregate(pipeline=pipeline, cursor={})
    n = 0
    for doc in cursor:
        if not doc.get('position') or not doc['position'].get('coordinates'):
            # Incomplete.
            continue

        try:
            lon, lat = doc['position']['coordinates']
        except (IndexError, KeyError):
            # Incomplete.
            continue

        mark = etree.SubElement(kdoc, 'Placemark')
        mark.set('id', doc['_id'])
        etree.SubElement(mark, 'visibility').text = '1'
        etree.SubElement(mark, 'styleUrl').text = '#stationMark'

        point = etree.SubElement(mark, 'Point')
        coordinates = etree.SubElement(point, 'coordinates')
        coordinates.text = '%f,%f' % (lon, lat)

        name = str(int(cent_to_fahr(doc['airTemperature']['value'])))
        etree.SubElement(mark, 'name').text = name

        n += 1

    print 'stations(%s):' % dt, time.time() - start, 'seconds', n, 'docs'
    return etree.tostring(root, pretty_print=prettyprint)


def state_name(lat, lng, db):
    """The name of the US state containing lat, lng. Or None."""

    # Relies on data loaded and indexed by load-us-states.py.
    doc = db.states.find_one({
        'geometry': {
            '$geoIntersects': {
                '$geometry': {
                    'type': 'Point',
                    'coordinates': [lng, lat]}}}})

    if doc:
        return doc['properties']['Name']
    else:
        return None


def info(lat, lng, dt, db):
    """Full info about nearest weather observation, or None.

    Nearest observation to a latitude, longitude, and datetime.
    """
    return db.data.find_one({
        'ts': dt,
        'position': {
            '$near': {
                '$geometry': {
                    'type': 'Point',
                    'coordinates': [lng, lat]
                }
            }
        }
    })
