import datetime
import time

import numpy
import scipy.spatial
from fastkml import Placemark
from fastkml.geometry import LinearRing


def triangles(dt, db):
    """Triangular mesh of stations active at one time.

    Takes a datetime and pymongo database. Returns list of KML Polygons.
    """
    next_hour = dt + datetime.timedelta(hours=1)

    # Positions of stations active in this hour. Needs index on 'ts'.
    pipeline = [{
        '$match': {'ts': {'$gte': dt, '$lt': next_hour}}
    }, {
        '$group': {
            '_id': '$st',
            'position': {'$first': '$position'}
        }
    }]

    start = time.time()
    result = db.data.aggregate(pipeline=pipeline)['result']
    buf = numpy.array(list(
        doc['position']['coordinates'] for doc in result
        if 'position' in doc))

    ar = numpy.ndarray(
        shape=(len(buf), 2),
        dtype=float,
        buffer=buf)

    delaunay = scipy.spatial.Delaunay(ar, incremental=True)
    kml_triangles = []
    for triangle in ar[delaunay.simplices]:
        ring = LinearRing([
            triangle[0].tolist(),
            triangle[1].tolist(),
            triangle[2].tolist(),
            triangle[0].tolist(),
        ])

        mark = Placemark(ns='')
        mark.geometry = ring
        mark.altitude_mode = 'relativeToGround'
        kml_triangles.append(mark)

    print 'triangles(%s):' % dt, time.time() - start, 'seconds', \
        len(buf), 'docs'

    return kml_triangles
