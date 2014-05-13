"""
Creating and plotting unstructured triangular grids.
"""
import datetime
import time

import matplotlib.pyplot as plt
from matplotlib.tri import UniformTriRefiner, TriAnalyzer
from matplotlib.tri.triangulation import Triangulation
from mpl_toolkits.mplot3d import Axes3D  # Registers '3d' projection type.
import matplotlib.cm as cm
import numpy as np
import pymongo

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
    except (TypeError, KeyError):
        # Incomplete.
        pass
    else:
        data.append([lng, lat, temp])

print 'aggregation took %.2f sec' % (time.time() - start)
start = time.time()
xytemp = np.asarray(data)
print 'arrayification took %.2f sec' % (time.time() - start)

x = xytemp[:,0]
y = xytemp[:,1]
z = xytemp[:,2]

start = time.time()
# Add earths to the left and right of earth.
x_expanded = np.concatenate((x - 360, x, x + 360))
# Copy the triple-earth 3 times.
x_tiled = np.tile(x_expanded, 3)
# Whereas x values are first expanded, then tiled, y values do the reverse.
y_tiled = np.tile(y, 3)
y_expanded = np.concatenate((y_tiled - 180, y_tiled, y_tiled + 180))
z_tiled = np.tile(z, 9)
print 'tiling took %.2f sec' % (time.time() - start)

start = time.time()
triangulation = Triangulation(x_tiled, y_expanded)
print 'triangulation took %.2f sec' % (time.time() - start)

#-----------------------------------------------------------------------------
# Refine data
#-----------------------------------------------------------------------------
start = time.time()
refiner = UniformTriRefiner(triangulation)
tri_refi, z_test_refi = refiner.refine_field(z_tiled, subdiv=3)
# tri_refi, z_test_refi = triangulation, z_tiled
print 'refining took %.2f sec' % (time.time() - start)

# masking badly shaped triangles at the border of the triangular mesh.
# start = time.time()
# mask = TriAnalyzer(tri_refi).get_flat_tri_mask(min_circle_ratio=.1)
# tri_refi.set_mask(mask)
# print 'masking took %.2f sec' % (time.time() - start)

plt.figure()
plt.gca().set_aspect('equal')

# Hide tiled copies of earth.
plt.gca().set_xlim(-180, 180)
plt.gca().set_ylim(-90, 90)

# Number of levels among temperatures.
start = time.time()
zmin = int(z.min()) - 10
zmax = int(z.max()) + 10

print 'calculating maxima took %.2f sec' % (time.time() - start)

levels = np.arange(zmin, zmax, (zmax - zmin) / 50.)
print len(levels), 'levels'
cmap = cm.get_cmap(name='terrain', lut=len(levels))
# cmap.set_over('blue')
# cmap.set_under('blue')

# plt.triplot(triangulation, lw=0.5, color='black')
plt.tricontourf(tri_refi, z_test_refi, levels=levels, cmap=cmap)
plt.show()
