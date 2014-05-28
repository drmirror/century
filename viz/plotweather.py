# Just like plot_temperature_ortho.py but uses scipy.interpolate.griddata.

import optparse
import datetime
import os
import time
import sys

import dateutil.parser
import numpy as np
import matplotlib.pyplot as plt
from scipy.interpolate import griddata
from mpl_toolkits.basemap import Basemap
import shutil
from viz.connect import get_db


def parse_options(argv):
    """Parse args and return options, start, end.

    args can be from sys.argv. start and end are datetimes.
    """
    usage = "python %prog START [END]"
    parser = optparse.OptionParser(usage=usage)
    parser.add_option('-p', '--port', type=int, default=4321)
    parser.add_option('-m', '--movie', default=False, action='store_true')
    options, args = parser.parse_args(argv)

    # args[0] is the program name.
    if len(args) not in (2, 3):
        parser.error("incorrect number of arguments")

    start, end = None, None
    try:
        start = hour(dateutil.parser.parse(args[1]))
    except Exception:
        parser.error("Couldn't parse start date")

    if len(args) == 3:
        try:
            end = hour(dateutil.parser.parse(args[2]))
        except Exception:
            parser.error("Couldn't parse end date")

    else:
        end = next_hour(start)

    if end - start < datetime.timedelta(hours=1):
        parser.error("END must be at least an hour after START")

    return options, start, end


def hour(dt):
    return dt.replace(minute=0, second=0, microsecond=0)


def next_hour(dt):
    return dt + datetime.timedelta(hours=1)


def stations(options, dt):
    db = get_db(options.port)

    # Positions of stations active in this hour. Needs index on 'ts'.
    pipeline = [{
        '$match': {
            'ts': {'$gte': dt, '$lt': next_hour(dt)},
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

    agg_start = time.time()
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

    print 'aggregation for %s: %d docs, took %.2f sec' % (
        dt, n, time.time() - agg_start)

    if not ret:
        print 'No results'
        sys.exit()
    return ret


def expand_earth(x, y, temperatures):
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
    return x_expanded, y_expanded, temperatures_expanded


def make_movie(glob_pattern, outname):
    cmd = (
        "ffmpeg -pattern_type glob -i '%s'"
        " -c:v libx264 -r 30 -pix_fmt yuv420p %s") % (glob_pattern, outname)

    if 0 != os.system(cmd):
        raise Exception()


def main(argv):
    options, start, end = parse_options(argv)


    dt = start

    if options.movie:
        if os.path.exists('tmp'):
            shutil.rmtree('tmp')

        os.mkdir('tmp')

    while True:
        station_data = np.array(stations(options, dt))
        longitudes = station_data[:, 0]
        latitudes = station_data[:, 1]
        temperatures = station_data[:, 2]

        # make orthographic basemap.
        m = Basemap(resolution='c', projection='cyl', lat_0=60., lon_0=-60.)

        x, y = m(longitudes, latitudes)
        x_expanded, y_expanded, temperatures_expanded = expand_earth(
            x, y, temperatures)

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

        CS1 = plt.contour(xi, yi, zi, 15,
                          linewidths=0.5, colors='k', alpha=0.25)

        CS2 = plt.contourf(xi, yi, zi, 15, cmap=plt.cm.RdBu_r)

        # Plot weather stations' locations as small blue dots.
        m.plot(x, y, 'b.', alpha=0.25)

        # define parallels and meridians to draw.
        parallels = np.arange(-80., 90, 20.)
        meridians = np.arange(0., 360., 20.)

        # draw coastlines, parallels, meridians.
        m.drawcoastlines(linewidth=1.5)
        m.drawparallels(parallels)
        m.drawmeridians(meridians)

        if options.movie:
            plt.savefig('tmp/%s.png' % dt)
            if dt < end:
                # Loop
                dt = next_hour(dt)
            else:
                if os.path.exists('out.mp4'):
                    os.remove('out.mp4')

                make_movie('tmp/*.png', 'out.mp4')
                os.system('open out.mp4')
                break
        else:
            plt.show()
            break

        plt.close()



if __name__ == '__main__':
    main(sys.argv)
