import datetime
import time
from matplotlib.tri import UniformTriRefiner

import numpy as np
import matplotlib.pyplot as plt
from mpl_toolkits.basemap import Basemap, shiftgrid
from matplotlib.tri.triangulation import Triangulation
import pymongo

dt = datetime.datetime(1978, 10, 01, 10)


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
        except KeyError:
            # Incomplete.
            pass
        else:
            ret.append((lon, lat, tmp))

    print 'aggregation %d docs, took %.2f sec' % (n, time.time() - start)
    return ret


station_data = np.array(stations(dt))
longitudes = station_data[:, 0]
latitudes = station_data[:, 1]
temperatures = station_data[:, 2]

# TODO: add cyclic points (could use addcyclic function)

# set desired contour levels.
clevs = np.arange(temperatures.min() - 10, temperatures.max() + 10, 5)

# make orthographic basemap.
m = Basemap(resolution='c', projection='cyl', lat_0=60., lon_0=-60.)

# create figure, add axes
fig1 = plt.figure(figsize=(8, 10))
ax = fig1.add_axes([0.1, 0.1, 0.8, 0.8])

# compute native x,y coordinates of grid.
x, y = m(longitudes, latitudes)

start = time.time()
# Add earths to the left and right of earth, copy the triple-earth 3 times.
x_expanded = np.tile(np.concatenate((x - 360, x, x + 360)), 3)
# Whereas x values are first expanded, then tiled, y values do the reverse.
y_tiled = np.tile(y, 3)
y_expanded = np.concatenate((y_tiled - 180, y_tiled, y_tiled + 180))
temperatures_expanded = np.tile(temperatures, 9)
print 'tiling took %.2f sec' % (time.time() - start)

start = time.time()
triangulation = Triangulation(x_expanded, y_expanded)
print 'triangulation took %.2f sec' % (time.time() - start)

# start = time.time()
# refiner = UniformTriRefiner(triangulation)
# subdiv = 1
# tri_refi, z_test_refi = refiner.refine_field(temperatures_expanded,
#                                              subdiv=subdiv)
# print 'refining %d subdivisions took %.2f sec' % (subdiv, time.time() - start)

# TODO: clip these to viewport.
tri_refi = triangulation
z_test_refi = temperatures_expanded
# Plot weather stations' locations as small blue dots.
m.plot(x, y, 'b.')
# plt.gca().tricontour(tri_refi, z_test_refi, clevs, colors='g')
cmap = plt.cm.RdBu_r
contour_plot = plt.gca().tricontourf(tri_refi, z_test_refi, clevs, cmap=cmap)

# plot temperature contours.
# TODO: animated=True??
# CS1 = m.contour(x, y, temperatures, clevs, tri=True, linewidths=1, colors='g',
#                 animated=True)
#
# CS2 = m.contourf(x_expanded, y_expanded, temperatures_expanded, clevs, tri=True,
#                  cmap=plt.cm.RdBu_r,
#                  animated=True)

# define parallels and meridians to draw.
parallels = np.arange(-80., 90, 20.)
meridians = np.arange(0., 360., 20.)

# draw coastlines, parallels, meridians.
m.drawcoastlines(linewidth=1.5)
m.drawparallels(parallels)
m.drawmeridians(meridians)
plt.show()
