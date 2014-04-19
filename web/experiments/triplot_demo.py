"""
Creating and plotting unstructured triangular grids.
"""
import matplotlib.pyplot as plt
from matplotlib.tri import UniformTriRefiner
from matplotlib.tri.triangulation import Triangulation
import matplotlib.cm as cm
import numpy as np

xytemp = np.asarray([
    [-0.101,0.872,17],[-0.080,0.883,11],[-0.069,0.888,22],[-0.054,0.890,27],
    [-0.057,0.895,9],[-0.073,0.900,14],[-0.087,0.898,17],[-0.090,0.904,16],
])


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

levels = np.arange(0., 30., 1)
cmap = cm.get_cmap(name='terrain', lut=None)
plt.tricontourf(tri_refi, z_test_refi, levels=levels, cmap=cmap)
plt.tricontour(tri_refi, z_test_refi, levels=levels,
               colors=['0.25', '0.5', '0.5', '0.5', '0.5'],
               linewidths=[1.0, 0.5, 0.5, 0.5, 0.5])

plt.title("High-resolution tricontouring")

plt.show()
