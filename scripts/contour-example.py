import os
import random
import sys
import math

import numpy as np
import matplotlib.tri as tri
from matplotlib import pyplot as plt
from scipy.interpolate import griddata

step = int(sys.argv[1])

size = 7
npoints = 15

plt.gca().set_xticklabels([])
plt.gca().set_yticklabels([])
ax = plt.gca()
plt.xlim(0, size)
plt.ylim(0, size)
ax.set_xticks(np.arange(0, size, 1))
ax.set_yticks(np.arange(0, size, 1))


random.seed(5)

points = [
    (.5 + random.random() * (size - 1), .5 + random.random() * (size - 1))
    for _ in range(npoints)]

temps = [int(40 + random.random() * 15) for _ in range(npoints)]
xy = np.asarray(points)
x = xy[:, 0]
y = xy[:, 1]

plt.plot(x, y, 'bo', alpha=0.5)

xi = yi = np.linspace(0, size, size + 1)
zi = griddata((x, y),
              temps,
              (xi[None, :], yi[:, None]),
              method='linear')

# Label sample points.
if step in (0, 1, 2):
    for i, temp in enumerate(temps):
        plt.annotate(str(temp), (x[i] + .1, y[i] + .1))

# Show triangulation.
if step in (1, 2):
    triangulation = tri.Triangulation(x, y)
    plt.triplot(triangulation, 'bo-')


# Show grid.
if step in (2, 3, 4, 5):
    plt.grid(True, linestyle='-')

# Show interpolated temperatures.
if step in (3, 4, 5):
    for colnum, col in enumerate(zi):
        for rownum, temp in enumerate(col):
            if not math.isnan(temp):
                plt.plot(xi[rownum], yi[colnum], 'ko')
                plt.annotate('%.1f' % temp, (xi[rownum] + .1, yi[colnum] + .1))

# Contour lines.
if step in (4, 5):
    plt.contour(xi, yi, zi, 15,
                linewidths=0.5, colors='k', alpha=0.25)


# Contour colors.
if step == 5:
    plt.contourf(xi, yi, zi, 15, cmap=plt.cm.RdBu_r)


if not os.path.exists('tmp'):
    os.mkdir('tmp')

plt.savefig('tmp/%s.png' % step)
