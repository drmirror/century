from math import acos, atan2, cos, sin
import math
import datetime
from matplotlib import cm
from matplotlib.colors import colorConverter

from matplotlib.collections import LineCollection
from mpl_toolkits.basemap import Basemap
from mpl_toolkits.mplot3d import Axes3D
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d.art3d import Line3DCollection, Poly3DCollection
import numpy as np
import pymongo
import time


def make_temp_triangulation():
    db = pymongo.MongoClient().ncdc

    # Positions of stations active in this hour. Needs index on 'ts'.
    dt = datetime.datetime(1978, 10, 01, 10)
    next_hour = dt + datetime.timedelta(hours=1)

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
    data = []
    for doc in cursor:
        n += 1
        try:
            lng, lat = doc['position']['coordinates']
            temp = doc['airTemperature']['value']
        except KeyError:
            # Incomplete.
            pass
        else:
            data.append([lng, lat, temp])

    print 'aggregation took %.2f sec' % (time.time() - start)
    start = time.time()
    lat_lng_tmp = np.asarray(data)
    print 'arrayification took %.2f sec' % (time.time() - start)

    lngs = lat_lng_tmp[:, 0]  # Degrees longitude.
    lats = lat_lng_tmp[:, 1]  # Degrees latitude.
    tmps = lat_lng_tmp[:, 2]  # Degrees centigrade.

    return lngs, lats, tmps


fig = plt.figure()
ax = fig.add_subplot(111, projection='3d')

u = np.linspace(0, 2 * np.pi, 100)
v = np.linspace(0, np.pi, 100)

r = 100  # Sphere radius.

x = r * np.outer(np.cos(u), np.sin(v))
y = r * np.outer(np.sin(u), np.sin(v))
z = r * np.outer(np.ones(np.size(u)), np.cos(v))
ax.plot_wireframe(x, y, z, linestyle=':', rstride=4, cstride=4, color='#7777FF')

# Cylindrical projection, lat / lon coordinates.
m = Basemap(projection='cyl',
            rsphere=r,
            resolution='c',
            area_thresh=1000)


# Adapted from Basemap.drawcoastlines().
def drawcoastlines(m, linewidth=1., linestyle='solid', color='k', antialiased=1,
                   ax=None):
    if m.resolution is None:
        raise AttributeError(
            'there are no boundary datasets associated with this Basemap instance')
    # get current axes instance (if none specified).
    ax = ax or m._check_ax()
    coastsegs = m.coastsegs

    print 'left %.2f, right %.2f, bottom %.2f, top %.2f' % (
        min(p[0] for seg in coastsegs for p in seg),
        max(p[0] for seg in coastsegs for p in seg),
        min(p[1] for seg in coastsegs for p in seg),
        max(p[1] for seg in coastsegs for p in seg),
    )

    # Convert from Mercator to Cartesian 3-space.
    # TODO: use numpy for this, not Python.
    coastsegs3d = []
    for segment in coastsegs:
        segment3d = []
        for point in segment:
            lon, lat = point
            lonrads = np.pi * lon / 180.0
            latrads = np.pi * lat / 180.0
            segment3d.append((
                r * math.cos(latrads) * math.cos(lonrads),
                r * math.cos(latrads) * math.sin(lonrads),
                r * math.sin(latrads)))

        coastsegs3d.append(segment3d)

    # coastlines = Line3DCollection(coastsegs3d)
    coastlines = Poly3DCollection(coastsegs3d)
    coastlines.set_color(color)
    coastlines.set_facecolor('w')
    coastlines.set_linestyle(linestyle)
    coastlines.set_linewidth(linewidth)
    coastlines.set_label('_nolabel_')
    ax.add_collection(coastlines)
    # set axes limits to fit map region.
    # m.set_axes_limits(ax=ax)
    return coastlines

drawcoastlines(m)
lng, lat, tmp = make_temp_triangulation()
ax.plot_surface(
    lng, lat, tmp,#np.zeros(len(lng)),
    facecolors=[
        colorConverter.to_rgba('r')
    ] * len(lng))

ax.set_xlim(-100, 100)
ax.set_ylim(-100, 100)
ax.set_zlim(-100, 100)
plt.show()

