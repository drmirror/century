# Just like plot_temperature_ortho.py but uses scipy.interpolate.griddata.

import datetime
import time

import numpy as np
import numpy.ma as ma
import matplotlib.pyplot as plt
from scipy.interpolate import griddata
from mpl_toolkits.basemap import Basemap, shiftgrid
from matplotlib.tri.triangulation import Triangulation
import pymongo
import sys

usage = "Usage: python plot_temperature_ortho_griddata.py '1991-10-01T10:00:00'"

if len(sys.argv) != 2:
    print usage
    sys.exit()

datestr = sys.argv[1]
try:
    dt = datetime.datetime.strptime(datestr, "%Y-%m-%dT%H:%M:%S")
except ValueError:
    print usage
    sys.exit()


def stations(dt):
    db = pymongo.MongoClient().ncdc

    next_hour = dt + datetime.timedelta(hours=1)

    # Positions of stations active in this hour. Needs index on 'ts'.
    pipeline = [{
                    '$match': {
                        'ts': {'$gte': dt, '$lt': next_hour},
                        # Valid temperature samples.
                        'airTemperature.quality': '1',
                        # Positions of 0, 0 are probably invalid.
                        'position.coordinates': {'$ne': [0, 0]}
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
    ret = []
    for doc in cursor:
        n += 1
        try:
            lon, lat = doc['position']['coordinates']
            tmp = doc['airTemperature']['value']
        except (TypeError, KeyError):
            # Incomplete.
            pass
        else:
            ret.append((lon, lat, tmp))

    print 'aggregation %d docs, took %.2f sec' % (n, time.time() - start)
    if not ret:
        print 'No results'
        sys.exit()
    return ret


station_data = np.array(stations(dt))
longitudes = station_data[:, 0]
latitudes = station_data[:, 1]
temperatures = station_data[:, 2]

# make orthographic basemap.
m = Basemap(resolution='c', projection='cyl', lat_0=60., lon_0=-60.)

x, y = m(longitudes, latitudes)


def expand_earth(x, y):
    """Surround Earth with copies to simulate spherical wrapping.

    Add earths to the left and right of earth, and vertically-flipped copies
    above and below (A, B, E, and F):

         +---------+---------+
         |         |         |
         |    A    |    B    |
         |         |         |
    +----+----+----+----+----+-----+
    |         |         |          |
    |    C    |  earth  |    D     |
    |         |         |          |
    +----+----+----+----+----+-----+
         |         |         |
         |    E    |    F    |
         |         |         |
         +---------+---------+
    """
    start = time.time()

    # Create C, earth, and D.
    x_expanded = np.concatenate((x - 360, x, x + 360))
    y_expanded = np.tile(y, 3)

    # Add A. The earths below and above earth are flipped top-to-bottom.
    x_expanded = np.concatenate((x_expanded, x - 180))
    y_expanded = np.concatenate((y_expanded, -y + 180))

    # Add B.
    x_expanded = np.concatenate((x_expanded, x + 180))
    y_expanded = np.concatenate((y_expanded, -y + 180))

    # Add E.
    x_expanded = np.concatenate((x_expanded, x - 180))
    y_expanded = np.concatenate((y_expanded, -y - 180))

    # Add F.
    x_expanded = np.concatenate((x_expanded, x + 180))
    y_expanded = np.concatenate((y_expanded, -y - 180))

    temperatures_expanded = np.tile(temperatures, 7)
    print 'tiling took %.2f sec' % (time.time() - start)
    return x_expanded, y_expanded, temperatures_expanded

x_expanded, y_expanded, temperatures_expanded = expand_earth(x, y)

# set desired contour levels.
clevs = np.arange(temperatures.min() - 10, temperatures.max() + 10, 5)

# create figure, add axes
fig1 = plt.figure(figsize=(8, 10))
ax = fig1.add_axes([0.1, 0.1, 0.8, 0.8])

# Expanded grid, larger than earth.
xi = np.linspace(-360, 359, 720)
yi = np.linspace(-180, 179, 360)
zi = griddata((x_expanded, y_expanded),
              temperatures_expanded,
              (xi[None, :], yi[:, None]),
              method='linear')

# CS1 = plt.contour(xi, yi, zi, 15, linewidths=0.5, colors='k')
CS2 = plt.contourf(xi, yi, zi, 15, cmap=plt.cm.RdBu_r)

# Plot weather stations' locations as small blue dots.
# m.plot(x, y, 'b.')

# define parallels and meridians to draw.
parallels = np.arange(-80., 90, 20.)
meridians = np.arange(0., 360., 20.)

# draw coastlines, parallels, meridians.
m.drawcoastlines(linewidth=1.5)
m.drawparallels(parallels)
m.drawmeridians(meridians)
plt.show()
