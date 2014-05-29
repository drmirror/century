import collections
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
import pytz
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
        start = hour(dateutil.parser.parse(args[1], ignoretz=True))
    except Exception:
        parser.error("Couldn't parse start date")

    start = start.replace(tzinfo=pytz.UTC)

    if len(args) == 3:
        try:
            end = hour(dateutil.parser.parse(args[2], ignoretz=True))
        except Exception:
            parser.error("Couldn't parse end date")

        end = end.replace(tzinfo=pytz.UTC)
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


def stations(options, monary_connection, dt, hours):
    """Get station data from datetime to datetime + (hours - 1).

    Returns numpy arrays: (timestamps, lons, lats, temperatures).
    """
    timer = timer_factory(options)
    datetimes = [
        dt + datetime.timedelta(hours=h)
        for h in range(hours)]

    query = {'t': {'$in': datetimes}}
    with timer('query'):
        # Query the "flattened" collection made by flatten-data.js.
        arrays = monary_connection.query(
            db='ncdc',
            coll='flattened',
            query=query,
            sort='t',
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
        "ffmpeg -r 6 -pattern_type glob -i '%s'"
        " -c:v libx264 -r 6 -pix_fmt yuv420p %s") % (glob_pattern, outname)

    print
    print cmd
    print
    if 0 != os.system(cmd):
        raise Exception()


def dt_from_monary(monary_timestamp):
    return datetime.datetime.fromtimestamp(float(monary_timestamp) / 1000.0)


def dt_to_monary(dt):
    return 1000.0 * time.mktime(dt.timetuple())


def get_slice(timestamps, start, end):
    start_i = np.argmax(timestamps >= dt_to_monary(start))
    length = np.argmax(timestamps[start_i:] >= dt_to_monary(end))

    # Final segment.
    if not length:
        length = len(timestamps[start_i:])

    return slice(start_i, start_i + length)


def loop(options, monary_connection, m, dt, n_hours):
    """Render a chunk of frames.

    m is the Basemap, dt the start time of the chunk.
    """
    timer = timer_factory(options)

    with timer('loop'):
        station_data = stations(options, monary_connection, dt, n_hours)

        timestamps, longitudes, latitudes, temperatures = station_data

        print 'got %d rows for %s and next %d hours' % (
            len(timestamps), dt, n_hours)

        # Fix timezone issue.
        tz_offset_ms = dt_to_monary(dt) - timestamps[0]
        timestamps += tz_offset_ms

        for k, v in sorted(collections.Counter(timestamps).items()):
            print '\t%s: %d %d' % (dt_from_monary(k), k, v)

        chunk_dts = [dt + datetime.timedelta(hours=h) for h in range(n_hours)]
        for chunk_dt in chunk_dts:
            chunk_dt_str = chunk_dt.strftime('%Y-%m-%d %H:%M')
            chunk_slice = get_slice(timestamps,
                                    chunk_dt,
                                    chunk_dt + datetime.timedelta(hours=1))

            print '%s: slice = %s, %d points' % (
                chunk_dt_str,
                chunk_slice, len(temperatures[chunk_slice]))

            x, y = m(longitudes[chunk_slice], latitudes[chunk_slice])
            x_expanded, y_expanded, temperatures_expanded = expand_earth(
                x, y, temperatures[chunk_slice])

            # create figure, add axes
            fig1 = plt.figure(figsize=(8, 4.2))
            ax = fig1.add_axes([0, 0, 1, .93])
            ax.set_title(chunk_dt_str)

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
                plt.savefig('tmp/%s.png' % chunk_dt.strftime('%Y-%m-%d %H'))
            else:
                plt.show()
                break

            plt.close()


def main(argv):
    options, start, end = parse_options(argv)
    td = end - start
    total_hours = td.days * 24 + td.seconds / 3600
    print '%d hours' % total_hours

    if options.profile:
        yappi.start(builtins=True)

    if options.movie:
        if os.path.exists('tmp'):
            shutil.rmtree('tmp')

        os.mkdir('tmp')

    # Make orthographic basemap.
    m = Basemap(resolution='c', projection='cyl', lat_0=60., lon_0=-60.)

    monary_connection = monary.Monary(port=options.port)
    dt = start
    n_hours = 10

    while dt <= end:
        loop(options, monary_connection, m, dt, n_hours)
        dt += datetime.timedelta(hours=n_hours)

        if not options.movie:
            break

    monary_connection.close()

    if options.movie:
        if os.path.exists('out.mp4'):
            os.remove('out.mp4')

        make_movie('tmp/*.png', 'out.mp4')
        os.system('open out.mp4')

    if options.profile:
        yappi.stop()
        stats = yappi.get_func_stats()
        stats.save('callgrind.out', type='callgrind')


if __name__ == '__main__':
    main(sys.argv)
