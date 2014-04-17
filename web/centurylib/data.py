import random
from fastkml import kml, Document
from fastkml.geometry import Point


def random_point():
    lat = 180.0 - 360.0 * random.random()
    lng = 90.0 - 180.0 * random.random()
    return Point(lat, lng)


def stations(dt, prettyprint=False):
    """Take a datetime and return list of Placemarks, the stations active."""
    k = kml.KML(ns='')
    doc = Document(name='stations')

    # Fake weather stations.
    for i in range(10000):
        station_mark = kml.Placemark(
            ns='', id=str(i), name=str(i), description='description')

        station_mark.geometry = random_point()
        doc.append(station_mark)

    k.append(doc)
    return k.to_string(prettyprint=prettyprint)
