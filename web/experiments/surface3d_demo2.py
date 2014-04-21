from math import acos, atan2, cos, sin
import math

from matplotlib.collections import LineCollection
from mpl_toolkits.basemap import Basemap, cm
from mpl_toolkits.mplot3d import Axes3D
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d.art3d import Line3DCollection, Poly3DCollection
import numpy as np

fig = plt.figure()
ax = fig.add_subplot(111, projection='3d')

u = np.linspace(0, 2 * np.pi, 100)
v = np.linspace(0, np.pi, 100)

r = .5  # Sphere radius.

x = r * np.outer(np.cos(u), np.sin(v))
y = r * np.outer(np.sin(u), np.sin(v))
z = r * np.outer(np.ones(np.size(u)), np.cos(v))
ax.plot_wireframe(x, y, z, linestyle=':', rstride=4, cstride=4, color='#7777FF')

m = Basemap(projection='merc',
            rsphere=r,
            resolution='l',
            area_thresh=10000)

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
    coastsegs3d = []
    for segment in coastsegs:
        segment3d = []
        for point in segment:
            lon, lat = point
            lat -= math.pi
            segment3d.append((
                r * math.cos(-2 * lat) * math.cos(2 * lon),
                r * math.cos(-2 * lat) * math.sin(2 * lon),
                r * math.sin(-2 * lat)))

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
ax.autoscale_view()

plt.show()

