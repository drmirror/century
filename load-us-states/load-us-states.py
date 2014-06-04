import json
import os
import tempfile
from subprocess import Popen

import pymongo


def convert_kml_to_geojson(filename):
    """Parse KML file and return python list and metadata.

    Inspired by http://okfnlabs.org/dataconverters/
    """
    # Install from
    # http://www.kyngchaos.com/files/software/frameworks/GDAL_Complete-1.11.dmg
    out_file = tempfile.NamedTemporaryFile()
    out_file.close()
    ogr2ogr = os.path.join(
        '/Library/Frameworks/GDAL.framework/Programs',
        'ogr2ogr')

    cmd = [ogr2ogr, '-f', 'GeoJSON', out_file.name, filename]
    inst = Popen(cmd)
    inst.communicate()

    content = open(out_file.name).read()
    os.remove(out_file.name)
    return json.loads(content)


def main():
    this_dir = os.path.dirname(__file__)
    filename = os.path.join(this_dir, 'gz_2010_us_040_00_500k.kml')
    print('loading %s' % filename)
    j = convert_kml_to_geojson(filename)
    print('loaded %s features' % len(j['features']))
    print('connecting to mongod')
    db = pymongo.MongoClient(port=5000).ncdc  # Assumes ssh tunnel on port 5000.
    print('dropping collection')
    db.states.drop()
    print('inserting data into "ncdc" database, "states" collection')
    db.states.insert(j['features'])
    print('done')


if __name__ == '__main__':
    main()
