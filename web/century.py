from flask import Flask, render_template, request, abort
import pymongo

from centurylib import data, parsing

db = pymongo.MongoClient(port=5000).ncdc  # Assumes ssh tunnel on port 5000.
app = Flask(__name__)


@app.route("/")
def index():
    return render_template('index.html')


@app.route("/samples.kml")
def samples():
    date_str = request.args['date']
    dt = parsing.parse_date_str(date_str)
    if not dt:
        abort(400, "Can't parse date %r" % date_str)

    # Placemarks.
    rv = data.stations(dt, db, prettyprint=app.debug)
    print rv
    return rv


@app.route("/us-state.kml")
def us_state():
    lat = float(request.args['lat'])
    lng = float(request.args['lng'])

    state_name = data.state_name(lat, lng, db)
    if state_name:
        return open('static/states-kml/%s.kml' % state_name).read()
    else:
        return ''


if __name__ == "__main__":
    print 'Visit http://localhost:8000'
    app.run(port=8000, debug=True)
