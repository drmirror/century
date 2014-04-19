"""
Creating and plotting unstructured triangular grids.
"""
import matplotlib.pyplot as plt
import numpy as np

xy = np.asarray([
    [-0.101,0.872],[-0.080,0.883],[-0.069,0.888],[-0.054,0.890],[-0.045,0.897],
    [-0.057,0.895],[-0.073,0.900],[-0.087,0.898],[-0.090,0.904],[-0.069,0.907],
])
x = xy[:,0]*180/3.14159
y = xy[:,1]*180/3.14159

# Rather than create a Triangulation object, can simply pass x, y and triangles
# arrays to triplot directly.  It would be better to use a Triangulation object
# if the same triangulation was to be used more than once to save duplicated
# calculations.
plt.figure()
plt.gca().set_aspect('equal')
plt.triplot(x, y, 'ko-')
plt.title('auto triplot')
plt.xlabel('Longitude (degrees)')
plt.ylabel('Latitude (degrees)')

plt.show()
