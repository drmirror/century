import contextlib
import optparse
import datetime
import os
import time
import shutil
import sys

import dateutil.parser
import numpy as np
import matplotlib.pyplot as plt
import monary
import yappi
from scipy.interpolate import griddata
from mpl_toolkits.basemap import Basemap


def parse_options(argv):
    """Parse args and return options, start, end.

    args can be from sys.argv. start and end are datetimes.
    """
    usage = "python %prog START [END]"
    parser = optparse.OptionParser(usage=usage)
    parser.add_option('-p', '--port', type=int, default=4321)
    parser.add_option('-m', '--movie', default=False, action='store_true')
    parser.add_option('-v', '--verbose', default=False, action='store_true')
    parser.add_option('--profile', default=False, action='store_true',
                      help='Run with Yappi')

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


def timer_factory(options):
    @contextlib.contextmanager
    def timer(name):
        start = time.time()
        yield
        if options.verbose:
            print '%s took %.2f sec' % (name, time.time() - start)

    return timer


def hour(dt):
    return dt.replace(minute=0, second=0, microsecond=0)


def next_hour(dt):
    return dt + datetime.timedelta(hours=1)


def stations(options, monary_connection, dt):
    timer = timer_factory(options)

    query = {'t': {'$gte': dt, '$lt': next_hour(dt)}}

    with timer('query'):
        # Query the "flattened" collection made by flatten-data.js.
        arrays = monary_connection.query(
            db='ncdc',
            coll='flattened',
            query=query,
            # Timestamp, lon, lat, air temperature.
            fields=['t', 'x', 'y', 'a'],
            types=['date', 'float32', 'float32', 'int8'])

    if not len(arrays[0]):
        print 'No results'
        sys.exit()

    return arrays


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

    if options.profile:
        yappi.start(builtins=True)

    dt = start

    if options.movie:
        if os.path.exists('tmp'):
            shutil.rmtree('tmp')

        os.mkdir('tmp')

    timer = timer_factory(options)

    # make orthographic basemap.
    m = Basemap(resolution='c', projection='cyl', lat_0=60., lon_0=-60.)

    monary_connection = monary.Monary(port=options.port)

    while True:
        with timer('loop'):
            print dt
            station_data = stations(options, monary_connection, dt)
            timestamps, longitudes, latitudes, temperatures = station_data

            x, y = m(longitudes, latitudes)
            x_expanded, y_expanded, temperatures_expanded = expand_earth(
                x, y, temperatures)

            # create figure, add axes
            fig1 = plt.figure(figsize=(8, 10))
            ax = fig1.add_axes([0.1, 0.1, 0.8, 0.8])

            # Expanded grid, larger than earth.
            xi = np.linspace(-360, 359, 720)
            yi = np.linspace(-180, 179, 360)

            with timer('griddata'):
                zi = griddata((x_expanded, y_expanded),
                              temperatures_expanded,
                              (xi[None, :], yi[:, None]),
                              method='linear')

            with timer('plot'):
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

    monary_connection.close()

    if options.profile:
        yappi.stop()
        stats = yappi.get_func_stats()
        stats.save('callgrind.out', type='callgrind')


if __name__ == '__main__':
    main(sys.argv)
