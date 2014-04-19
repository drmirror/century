"""
Creating and plotting unstructured triangular grids.
"""
import datetime
import time

import matplotlib.pyplot as plt
from matplotlib.tri import UniformTriRefiner
from matplotlib.tri.triangulation import Triangulation
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


xytemp = np.asarray(data)

x = xytemp[:,0]
y = xytemp[:,1]
z = xytemp[:,2]



triangulation = Triangulation(x, y)
#-----------------------------------------------------------------------------
# Refine data
#-----------------------------------------------------------------------------
refiner = UniformTriRefiner(triangulation)
tri_refi, z_test_refi = refiner.refine_field(z, subdiv=3)

plt.figure()
plt.gca().set_aspect('equal')
plt.triplot(triangulation, lw=0.5, color='white')

# Number of levels among temperatures.
zmin = int(z.min()) - 1
zmax = int(z.max()) + 1
levels = np.arange(zmin, zmax, (zmax - zmin) / 20.)
cmap = cm.get_cmap(name='terrain', lut=None)
plt.tricontourf(tri_refi, z_test_refi, levels=levels, cmap=cmap)
plt.tricontour(tri_refi, z_test_refi, levels=levels,
               colors=['0.25', '0.5', '0.5', '0.5', '0.5'],
               linewidths=[1.0, 0.5, 0.5, 0.5, 0.5])

plt.title("High-resolution tricontouring")

plt.show()
